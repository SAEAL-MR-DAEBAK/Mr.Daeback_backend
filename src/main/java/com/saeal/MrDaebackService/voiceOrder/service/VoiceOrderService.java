package com.saeal.MrDaebackService.voiceOrder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saeal.MrDaebackService.voiceOrder.dto.LlmResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.ChatMessageDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.ChatResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UiAction;
import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import com.saeal.MrDaebackService.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 음성/텍스트 주문 처리 서비스 (원본 기반 - 심플 버전)
 *
 * 주요 흐름:
 * 1. IDLE → 2. SELECTING_ADDRESS (주소 선택) → 3. SELECTING_MENU (메뉴 선택)
 * → 4. SELECTING_STYLE (스타일 선택) → 5. SELECTING_QUANTITY (수량 선택)
 * → 6. ASKING_MORE (추가 주문?) → 7. CONFIRMING (확인)
 *
 * ★ CONFIRMING 상태가 되면 프론트엔드에서 Cart API를 직접 호출하여 주문 완료
 * ★ 백엔드는 Product 생성/결제 처리를 하지 않음 (GUI에서만 처리)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceOrderService {

    private final GroqService groqService;
    private final MenuMatcher menuMatcher;
    private final CartManager cartManager;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 채팅 메시지 처리
     */
    public ChatResponseDto processChat(ChatRequestDto request, UUID userId) {
        // 1. 사용자 메시지 추출
        String userMessage = extractUserMessage(request);

        // 2. 대화 히스토리 변환
        List<Map<String, String>> history = convertHistory(request.getConversationHistory());

        // 3. 현재 장바구니
        List<OrderItemRequestDto> currentOrder = request.getCurrentOrder() != null
                ? request.getCurrentOrder() : new ArrayList<>();

        // 4. 사용자 주소 목록
        List<String> userAddresses = getUserAddresses(userId);

        // 5. 선택된 주소 (프론트에서 전달)
        String selectedAddress = request.getSelectedAddress();

        // 6. LLM 호출 (최근 히스토리 1턴만)
        String systemPrompt = buildSystemPrompt(currentOrder, selectedAddress, userAddresses);
        List<Map<String, String>> recentHistory = history.size() > 2
                ? history.subList(history.size() - 2, history.size())
                : history;
        String llmRawResponse = groqService.chat(systemPrompt, recentHistory, userMessage);

        // 7. JSON 파싱
        LlmResponseDto llmResponse = parseLlmResponse(llmRawResponse);

        // 8. Intent 처리
        return processIntent(userMessage, llmResponse, currentOrder, selectedAddress, userAddresses);
    }

    private String extractUserMessage(ChatRequestDto request) {
        if (request.getAudioBase64() != null && !request.getAudioBase64().isEmpty()) {
            byte[] audioData = Base64.getDecoder().decode(request.getAudioBase64());
            return groqService.transcribe(audioData, request.getAudioFormat());
        }
        return request.getMessage();
    }

    private List<Map<String, String>> convertHistory(List<ChatMessageDto> history) {
        List<Map<String, String>> result = new ArrayList<>();
        if (history != null) {
            for (ChatMessageDto msg : history) {
                result.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }
        return result;
    }

    private List<String> getUserAddresses(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getAddresses() != null && !user.getAddresses().isEmpty()) {
                return new ArrayList<>(user.getAddresses());
            }
        } catch (Exception e) {
            log.warn("사용자 주소 조회 실패: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private String buildSystemPrompt(List<OrderItemRequestDto> currentOrder, String selectedAddress, List<String> userAddresses) {
        StringBuilder orderSummary = new StringBuilder();
        if (currentOrder != null && !currentOrder.isEmpty()) {
            orderSummary.append("\n\n## Current Order\n");
            for (OrderItemRequestDto item : currentOrder) {
                orderSummary.append(String.format("- %s (%s) x%d = %,d원\n",
                        item.getDinnerName(),
                        item.getServingStyleName() != null ? item.getServingStyleName() : "스타일 미선택",
                        item.getQuantity(), item.getTotalPrice()));
            }
        }

        StringBuilder addressInfo = new StringBuilder();
        if (selectedAddress != null && !selectedAddress.isEmpty()) {
            addressInfo.append("\n\n## Selected Address: ").append(selectedAddress);
        }
        if (userAddresses != null && !userAddresses.isEmpty()) {
            addressInfo.append("\n\n## User's Addresses:\n");
            for (int i = 0; i < userAddresses.size(); i++) {
                addressInfo.append(String.format("%d. %s\n", i + 1, userAddresses.get(i)));
            }
        }

        return String.format("""
                You are an AI order assistant for "Mr.Daeback" (미스터대백) restaurant.

                ## Available Menus
                %s

                ## Available Serving Styles
                %s
                %s%s

                ## Order Flow (IMPORTANT - Address First!)
                1. FIRST ask for delivery address if not selected
                2. Then menu selection
                3. Then style selection
                4. Then quantity
                5. Ask if they want more
                6. Confirm order

                ## Your Task
                1. Understand user's intent
                2. Extract entities (menu name, style name, quantity, address index)
                3. Generate a friendly Korean response message

                ## Output Format (MUST ALWAYS be valid JSON)
                {"intent":"ORDER_MENU","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Valentine Dinner 선택하셨어요! 스타일은 어떻게 할까요?"}

                ## Intent Types
                - ORDER_MENU: User wants to ORDER a menu item (MUST have menuName + ordering expression like "주세요", "주문", "할게요", "줘")
                - SELECT_STYLE: User selects serving style for current item (NO menuName, only styleName like "그랜드로", "심플 스타일로 할게")
                - SET_QUANTITY: User specifies quantity only (NO menuName, only quantity like "2인분", "3개", "3인분으로 할게")
                - EDIT_ORDER: User wants to modify an existing order item (MUST include menuName + "바꿔", "수정", "변경")
                - REMOVE_ITEM: User wants to delete a specific item (menuName + "빼줘", "삭제", "취소". Use "LAST" for last item)
                - ADD_TO_CART: User wants to finish ordering ("장바구니", "주문 끝", "아니요", "됐어요")
                - SELECT_ADDRESS: User selects address ("1번", "첫번째")
                - CANCEL_ORDER: User cancels ALL orders (전체 취소)
                - ASK_MENU_INFO: User asks about menu OR says menu name only without ordering expression
                - GREETING: Greetings or casual talk
                - CONFIRM_YES: Positive response ("네", "좋아요")
                - CONFIRM_NO: Negative response ("아니요")

                ## Rules
                - ALWAYS respond in JSON format, even for GREETING or ASK_MENU_INFO
                - menuName/styleName must match exactly from the lists above
                - DO NOT default quantity to 1 - only set quantity if user explicitly mentions it
                - Restaurant name is "Mr.Daeback" (미스터대백) - never change this name
                - If address not selected, ask for address FIRST before menu selection
                """,
                menuMatcher.getMenuListForPrompt(),
                menuMatcher.getStyleListForPrompt(),
                orderSummary.toString(),
                addressInfo.toString()
        );
    }

    private LlmResponseDto parseLlmResponse(String rawResponse) {
        try {
            String jsonContent = rawResponse.trim();
            if (jsonContent.startsWith("```json")) jsonContent = jsonContent.substring(7);
            if (jsonContent.startsWith("```")) jsonContent = jsonContent.substring(3);
            if (jsonContent.endsWith("```")) jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            jsonContent = jsonContent.trim();

            return objectMapper.readValue(jsonContent, LlmResponseDto.class);
        } catch (JsonProcessingException e) {
            LlmResponseDto fallback = new LlmResponseDto();
            fallback.setIntent("ASK_MENU_INFO");
            fallback.setMessage(rawResponse.trim());
            return fallback;
        }
    }

    private ChatResponseDto processIntent(String userMessage, LlmResponseDto llmResponse,
                                          List<OrderItemRequestDto> currentOrder,
                                          String selectedAddress,
                                          List<String> userAddresses) {
        UserIntent intent = parseIntent(llmResponse.getIntent());
        LlmResponseDto.ExtractedEntities entities = llmResponse.getEntities();

        List<OrderItemDto> updatedOrder = cartManager.convertToOrderItemDtoList(currentOrder);
        OrderFlowState nextState = OrderFlowState.IDLE;
        UiAction uiAction = UiAction.NONE;
        String message = llmResponse.getMessage();
        String finalSelectedAddress = selectedAddress;

        // 진행 중인 아이템 찾기 (quantity == 0)
        OrderItemDto pendingItem = findPendingItem(updatedOrder);
        int pendingIdx = pendingItem != null ? updatedOrder.indexOf(pendingItem) : -1;

        switch (intent) {
            case GREETING -> {
                // ★ 주소가 없으면 먼저 주소 선택 요청
                if (finalSelectedAddress == null || finalSelectedAddress.isEmpty()) {
                    if (!userAddresses.isEmpty()) {
                        message = "안녕하세요! Mr.Daeback입니다. 🍽️\n먼저 배달받으실 주소를 선택해주세요!\n" + formatAddressList(userAddresses);
                        nextState = OrderFlowState.SELECTING_ADDRESS;
                    } else {
                        message = "안녕하세요! Mr.Daeback입니다. 저장된 배달 주소가 없어요. 마이페이지에서 주소를 먼저 추가해주세요!";
                        nextState = OrderFlowState.IDLE;
                    }
                } else {
                    message = "안녕하세요! Mr.Daeback입니다. 배달 주소는 '" + finalSelectedAddress + "'로 설정되어 있어요. 어떤 메뉴를 주문하시겠어요?";
                    nextState = OrderFlowState.SELECTING_MENU;
                }
            }

            case SELECT_ADDRESS -> {
                if (entities != null && entities.getAddressIndex() != null) {
                    int idx = entities.getAddressIndex() - 1;
                    if (idx >= 0 && idx < userAddresses.size()) {
                        finalSelectedAddress = userAddresses.get(idx);
                        nextState = OrderFlowState.SELECTING_MENU;
                        message = finalSelectedAddress + "로 배달해드릴게요! 어떤 메뉴를 주문하시겠어요?";
                    } else {
                        message = "올바른 주소 번호를 선택해주세요. (1~" + userAddresses.size() + ")";
                        nextState = OrderFlowState.SELECTING_ADDRESS;
                    }
                }
            }

            case ORDER_MENU -> {
                // ★ 주소가 없으면 먼저 주소 선택 요청
                if (finalSelectedAddress == null || finalSelectedAddress.isEmpty()) {
                    if (!userAddresses.isEmpty()) {
                        message = "먼저 배달 주소를 선택해주세요!\n" + formatAddressList(userAddresses);
                        nextState = OrderFlowState.SELECTING_ADDRESS;
                        break;
                    } else {
                        message = "저장된 배달 주소가 없어요. 마이페이지에서 주소를 먼저 추가해주세요!";
                        nextState = OrderFlowState.IDLE;
                        break;
                    }
                }

                // 진행 중인 아이템이 있고, 수량만 말한 경우
                if (pendingItem != null && entities != null && entities.getQuantity() != null && entities.getMenuName() == null) {
                    if (pendingItem.getServingStyleId() != null) {
                        OrderItemDto updated = cartManager.setQuantity(pendingItem, entities.getQuantity());
                        updatedOrder.set(pendingIdx, updated);
                        nextState = OrderFlowState.ASKING_MORE;
                        uiAction = UiAction.UPDATE_ORDER_LIST;
                        message = updated.getDinnerName() + " " + entities.getQuantity() + "개 주문 완료! 더 주문하실 게 있으세요?";
                    } else {
                        message = "먼저 스타일을 선택해주세요!";
                        nextState = OrderFlowState.SELECTING_STYLE;
                    }
                    break;
                }

                // 진행 중인 아이템이 있고, 스타일만 말한 경우
                if (pendingItem != null && entities != null && entities.getStyleName() != null && entities.getMenuName() == null) {
                    var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
                    if (styleOpt.isPresent()) {
                        OrderItemDto updated = cartManager.applyStyleToItem(pendingItem, styleOpt.get());
                        updatedOrder.set(pendingIdx, updated);
                        uiAction = UiAction.UPDATE_ORDER_LIST;

                        if (updated.getQuantity() == 0) {
                            nextState = OrderFlowState.SELECTING_QUANTITY;
                            message = styleOpt.get().getStyleName() + "로 선택하셨어요! " + buildQuantityQuestion(updated.getDinnerName());
                        } else {
                            nextState = OrderFlowState.ASKING_MORE;
                            message = updated.getDinnerName() + " " + styleOpt.get().getStyleName() + " 적용 완료! 더 주문하실 게 있으세요?";
                        }
                    } else {
                        message = "죄송해요, '" + entities.getStyleName() + "' 스타일을 찾을 수 없어요.";
                        nextState = OrderFlowState.SELECTING_STYLE;
                    }
                    break;
                }

                if (entities != null && entities.getMenuName() != null) {
                    // 진행 중인 아이템이 있으면 먼저 완성하도록 안내
                    if (pendingItem != null) {
                        if (pendingItem.getServingStyleId() == null) {
                            message = pendingItem.getDinnerName() + "의 스타일을 먼저 선택해주세요!";
                            nextState = OrderFlowState.SELECTING_STYLE;
                        } else {
                            message = pendingItem.getDinnerName() + "의 수량을 먼저 선택해주세요!";
                            nextState = OrderFlowState.SELECTING_QUANTITY;
                        }
                        break;
                    }

                    var dinnerOpt = menuMatcher.findDinnerByName(entities.getMenuName());
                    if (dinnerOpt.isPresent()) {
                        // 임시 아이템 생성 (수량 0으로 시작)
                        OrderItemDto newItem = cartManager.addMenuWithoutQuantity(dinnerOpt.get());

                        // 스타일도 함께 지정된 경우
                        if (entities.getStyleName() != null) {
                            var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
                            if (styleOpt.isPresent()) {
                                newItem = cartManager.applyStyleToItem(newItem, styleOpt.get());
                            }
                        }

                        // 수량도 함께 지정된 경우
                        if (entities.getQuantity() != null && entities.getQuantity() > 0) {
                            newItem = cartManager.setQuantity(newItem, entities.getQuantity());
                        }

                        updatedOrder.add(newItem);
                        uiAction = UiAction.UPDATE_ORDER_LIST;

                        // 다음 상태 결정
                        if (newItem.getServingStyleId() == null) {
                            nextState = OrderFlowState.SELECTING_STYLE;
                            message = dinnerOpt.get().getDinnerName() + " 선택하셨어요! 어떤 스타일로 하실래요? Simple Style, Grand Style, Deluxe Style이 있어요.";
                        } else if (newItem.getQuantity() == 0) {
                            nextState = OrderFlowState.SELECTING_QUANTITY;
                            message = newItem.getDinnerName() + " " + newItem.getServingStyleName() + "이요! " + buildQuantityQuestion(newItem.getDinnerName());
                        } else {
                            nextState = OrderFlowState.ASKING_MORE;
                        }
                    } else {
                        message = "죄송해요, '" + entities.getMenuName() + "' 메뉴를 찾을 수 없어요.";
                        nextState = OrderFlowState.SELECTING_MENU;
                    }
                }
            }

            case SELECT_STYLE -> {
                if (entities != null && entities.getStyleName() != null) {
                    if (pendingItem != null && pendingIdx >= 0) {
                        var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
                        if (styleOpt.isPresent()) {
                            OrderItemDto updated = cartManager.applyStyleToItem(pendingItem, styleOpt.get());
                            updatedOrder.set(pendingIdx, updated);
                            uiAction = UiAction.UPDATE_ORDER_LIST;

                            if (updated.getQuantity() == 0) {
                                nextState = OrderFlowState.SELECTING_QUANTITY;
                                message = styleOpt.get().getStyleName() + "로 선택하셨어요! " + buildQuantityQuestion(updated.getDinnerName());
                            } else {
                                nextState = OrderFlowState.ASKING_MORE;
                            }
                        } else {
                            message = "죄송해요, '" + entities.getStyleName() + "' 스타일을 찾을 수 없어요.";
                            nextState = OrderFlowState.SELECTING_STYLE;
                        }
                    } else {
                        message = "먼저 메뉴를 선택해주세요!";
                        nextState = OrderFlowState.SELECTING_MENU;
                    }
                } else {
                    message = "먼저 메뉴를 선택해주세요!";
                    nextState = OrderFlowState.SELECTING_MENU;
                }
            }

            case SET_QUANTITY -> {
                if (entities != null && entities.getQuantity() != null) {
                    if (pendingItem != null && pendingIdx >= 0) {
                        if (pendingItem.getServingStyleId() == null) {
                            message = "먼저 스타일을 선택해주세요!";
                            nextState = OrderFlowState.SELECTING_STYLE;
                        } else {
                            OrderItemDto updated = cartManager.setQuantity(pendingItem, entities.getQuantity());
                            updatedOrder.set(pendingIdx, updated);
                            nextState = OrderFlowState.ASKING_MORE;
                            uiAction = UiAction.UPDATE_ORDER_LIST;
                            message = updated.getDinnerName() + " " + entities.getQuantity() + "개 주문 완료! 더 주문하실 게 있으세요?";
                        }
                    } else {
                        message = "먼저 메뉴를 선택해주세요!";
                        nextState = OrderFlowState.SELECTING_MENU;
                    }
                } else {
                    message = "먼저 메뉴를 선택해주세요!";
                    nextState = OrderFlowState.SELECTING_MENU;
                }
            }

            case EDIT_ORDER -> {
                if (entities != null && entities.getMenuName() != null) {
                    int targetIdx = -1;
                    for (int i = 0; i < updatedOrder.size(); i++) {
                        if (updatedOrder.get(i).getDinnerName().equalsIgnoreCase(entities.getMenuName())
                            || menuMatcher.isMatchingMenu(updatedOrder.get(i).getDinnerName(), entities.getMenuName())) {
                            targetIdx = i;
                            break;
                        }
                    }

                    if (targetIdx >= 0) {
                        OrderItemDto targetItem = updatedOrder.get(targetIdx);

                        if (entities.getQuantity() != null && entities.getQuantity() > 0) {
                            targetItem = cartManager.setQuantity(targetItem, entities.getQuantity());
                            updatedOrder.set(targetIdx, targetItem);
                            message = targetItem.getDinnerName() + " " + entities.getQuantity() + "개로 변경했어요!";
                            uiAction = UiAction.UPDATE_ORDER_LIST;
                        }

                        if (entities.getStyleName() != null) {
                            var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
                            if (styleOpt.isPresent()) {
                                targetItem = cartManager.changeStyle(targetItem, styleOpt.get());
                                updatedOrder.set(targetIdx, targetItem);
                                message = targetItem.getDinnerName() + " 스타일을 " + styleOpt.get().getStyleName() + "로 변경했어요!";
                                uiAction = UiAction.UPDATE_ORDER_LIST;
                            }
                        }

                        nextState = OrderFlowState.ASKING_MORE;
                    } else {
                        message = "'" + entities.getMenuName() + "' 메뉴가 장바구니에 없어요.";
                        nextState = OrderFlowState.ASKING_MORE;
                    }
                } else {
                    message = "어떤 메뉴를 수정할까요?";
                    nextState = OrderFlowState.ASKING_MORE;
                }
            }

            case REMOVE_ITEM -> {
                if (updatedOrder.isEmpty()) {
                    message = "장바구니가 비어있어요!";
                    nextState = OrderFlowState.IDLE;
                } else if (entities != null && entities.getMenuName() != null) {
                    String menuName = entities.getMenuName();

                    if ("LAST".equalsIgnoreCase(menuName)) {
                        OrderItemDto removed = updatedOrder.remove(updatedOrder.size() - 1);
                        message = removed.getDinnerName() + "을(를) 삭제했어요!";
                        uiAction = UiAction.UPDATE_ORDER_LIST;
                        nextState = updatedOrder.isEmpty() ? OrderFlowState.SELECTING_MENU : OrderFlowState.ASKING_MORE;
                    } else {
                        int targetIdx = -1;
                        for (int i = 0; i < updatedOrder.size(); i++) {
                            if (updatedOrder.get(i).getDinnerName().equalsIgnoreCase(menuName)
                                || menuMatcher.isMatchingMenu(updatedOrder.get(i).getDinnerName(), menuName)) {
                                targetIdx = i;
                                break;
                            }
                        }

                        if (targetIdx >= 0) {
                            OrderItemDto removed = updatedOrder.remove(targetIdx);
                            message = removed.getDinnerName() + "을(를) 삭제했어요!";
                            uiAction = UiAction.UPDATE_ORDER_LIST;
                            nextState = updatedOrder.isEmpty() ? OrderFlowState.SELECTING_MENU : OrderFlowState.ASKING_MORE;
                        } else {
                            message = "'" + menuName + "' 메뉴가 장바구니에 없어요.";
                            nextState = OrderFlowState.ASKING_MORE;
                        }
                    }
                } else {
                    message = "어떤 메뉴를 삭제할까요?";
                    nextState = OrderFlowState.ASKING_MORE;
                }
            }

            case ADD_TO_CART, CONFIRM_NO -> {
                // 완성되지 않은 아이템(수량 0) 제거
                updatedOrder.removeIf(item -> item.getQuantity() == 0);

                if (!updatedOrder.isEmpty()) {
                    // ★ 주소가 없으면 주소 선택 먼저
                    if (finalSelectedAddress == null || finalSelectedAddress.isEmpty()) {
                        if (!userAddresses.isEmpty()) {
                            nextState = OrderFlowState.SELECTING_ADDRESS;
                            message = "배달 주소를 선택해주세요!\n" + formatAddressList(userAddresses);
                        } else {
                            message = "저장된 주소가 없어요. 마이페이지에서 주소를 추가해주세요.";
                            nextState = OrderFlowState.IDLE;
                        }
                    } else {
                        // ★ CONFIRMING 상태 → 프론트에서 Cart API 호출
                        nextState = OrderFlowState.CONFIRMING;
                        uiAction = UiAction.SHOW_CONFIRM_MODAL;
                        message = "주문을 확정하시겠어요?\n" + buildOrderSummary(updatedOrder, finalSelectedAddress);
                    }
                } else {
                    message = "장바구니가 비어있어요. 먼저 메뉴를 선택해주세요!";
                    nextState = OrderFlowState.SELECTING_MENU;
                }
            }

            case CANCEL_ORDER -> {
                updatedOrder.clear();
                nextState = OrderFlowState.IDLE;
                message = "주문이 취소되었어요. 새로운 주문을 시작해주세요!";
                uiAction = UiAction.SHOW_CANCEL_CONFIRM;
            }

            case CONFIRM_YES -> {
                // ★ 확정 → CONFIRMING 상태 → 프론트에서 Cart API 호출
                updatedOrder.removeIf(item -> item.getQuantity() == 0);

                if (updatedOrder.isEmpty()) {
                    message = "장바구니가 비어있어요!";
                    nextState = OrderFlowState.SELECTING_MENU;
                } else if (finalSelectedAddress == null || finalSelectedAddress.isEmpty()) {
                    if (!userAddresses.isEmpty()) {
                        nextState = OrderFlowState.SELECTING_ADDRESS;
                        message = "배달 주소를 선택해주세요!\n" + formatAddressList(userAddresses);
                    } else {
                        message = "저장된 주소가 없어요.";
                        nextState = OrderFlowState.IDLE;
                    }
                } else {
                    nextState = OrderFlowState.CONFIRMING;
                    uiAction = UiAction.SHOW_CONFIRM_MODAL;
                    message = "주문을 확정합니다!\n" + buildOrderSummary(updatedOrder, finalSelectedAddress);
                }
            }

            case ASK_MENU_INFO -> {
                // 주소가 없으면 주소 선택 안내 추가
                if (finalSelectedAddress == null || finalSelectedAddress.isEmpty()) {
                    if (!userAddresses.isEmpty()) {
                        message = message + "\n\n배달 주소를 먼저 선택해주세요!\n" + formatAddressList(userAddresses);
                        nextState = OrderFlowState.SELECTING_ADDRESS;
                    } else {
                        nextState = OrderFlowState.IDLE;
                    }
                } else {
                    nextState = OrderFlowState.SELECTING_MENU;
                }
            }

            default -> nextState = OrderFlowState.IDLE;
        }

        int totalPrice = cartManager.calculateTotalPrice(updatedOrder);

        return ChatResponseDto.builder()
                .userMessage(userMessage)
                .assistantMessage(message)
                .uiAction(uiAction)
                .currentOrder(updatedOrder)
                .totalPrice(totalPrice)
                .selectedAddress(finalSelectedAddress)
                .build();
    }

    private String formatAddressList(List<String> addresses) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addresses.size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, addresses.get(i)));
        }
        return sb.toString().trim();
    }

    private String buildOrderSummary(List<OrderItemDto> orderItems, String address) {
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (OrderItemDto item : orderItems) {
            sb.append(String.format("• %s (%s) x%d = %,d원\n",
                    item.getDinnerName(),
                    item.getServingStyleName() != null ? item.getServingStyleName() : "스타일 미선택",
                    item.getQuantity(),
                    item.getTotalPrice()));
            total += item.getTotalPrice();
        }
        sb.append(String.format("\n총 금액: %,d원", total));
        if (address != null && !address.isEmpty()) {
            sb.append(String.format("\n배달 주소: %s", address));
        }
        return sb.toString();
    }

    private String buildQuantityQuestion(String dinnerName) {
        if (dinnerName != null && dinnerName.toLowerCase().contains("champagne")) {
            return "1개가 2인분이에요. 몇 개로 드릴까요?";
        }
        return "몇 개로 드릴까요?";
    }

    private UserIntent parseIntent(String intentStr) {
        if (intentStr == null) return UserIntent.UNKNOWN;
        try {
            return UserIntent.valueOf(intentStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserIntent.UNKNOWN;
        }
    }

    private OrderItemDto findPendingItem(List<OrderItemDto> orderItems) {
        for (OrderItemDto item : orderItems) {
            if (item.getQuantity() == 0) {
                return item;
            }
        }
        return null;
    }
}
