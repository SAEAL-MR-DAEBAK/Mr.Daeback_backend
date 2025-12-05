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
        log.info("[User {}] 메시지: {}", userId, userMessage);

        // 2. 대화 히스토리 변환
        List<Map<String, String>> history = convertHistory(request.getConversationHistory());

        // 3. 현재 장바구니
        List<OrderItemRequestDto> currentOrder = request.getCurrentOrder() != null
                ? request.getCurrentOrder() : new ArrayList<>();

        // 4. 사용자 주소 목록
        List<String> userAddresses = getUserAddresses(userId);

        // 5. LLM 호출 (최근 히스토리 1개만 - 맥락 유지하면서 혼동 최소화)
        String systemPrompt = buildSystemPrompt();
        List<Map<String, String>> recentHistory = history.size() > 2
                ? history.subList(history.size() - 2, history.size())  // 최근 1턴 (user + assistant)
                : history;
        String llmRawResponse = groqService.chat(systemPrompt, recentHistory, userMessage);

        // 6. JSON 파싱
        LlmResponseDto llmResponse = parseLlmResponse(llmRawResponse);

        // 7. Intent 처리
        return processIntent(userMessage, llmResponse, currentOrder, userAddresses);
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

    private String buildSystemPrompt() {
        return String.format("""
                You are an AI order assistant for "Mr.Daeback" (미스터대백) restaurant.

                ## Available Menus
                %s

                ## Available Serving Styles
                %s

                ## Your Task
                1. Understand user's intent
                2. Extract entities (menu name, style name, quantity, address index)
                3. Generate a friendly Korean response message

                ## Output Format (MUST ALWAYS be valid JSON - no exceptions)
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
                - ASK_MENU_INFO: User asks about menu OR says menu name only without ordering expression (e.g., "발렌타인 디너", "발렌타인 디너가 뭐야?")
                - GREETING: Greetings or casual talk

                ## Order Flow (IMPORTANT)
                The order requires 3 pieces of information: Menu, Style, Quantity
                - If user says menu only → ask for style
                - If user says menu + style → ask for quantity
                - If user says menu + quantity → ask for style
                - If user says all 3 at once (e.g., "발렌타인 디너 그랜드 2인분") → extract all in ORDER_MENU intent
                - After style is selected → ask for quantity
                - After quantity is set → ask if they want more

                ## Rules
                - ALWAYS respond in JSON format, even for GREETING or ASK_MENU_INFO
                - menuName/styleName must match exactly from the lists above
                - DO NOT default quantity to 1 - only set quantity if user explicitly mentions it
                - Restaurant name is "Mr.Daeback" (미스터대백) - never change this name

                ## Few-Shot Examples

                User: "안녕하세요"
                {"intent":"GREETING","entities":null,"message":"안녕하세요! Mr.Daeback입니다. 무엇을 주문하시겠어요?"}

                User: "아 졸려"
                {"intent":"GREETING","entities":null,"message":"피곤하시군요! Mr.Daeback의 맛있는 음식으로 기분 전환 어떠세요?"}

                User: "와이프 생일인데 뭐가 좋을까?"
                {"intent":"ASK_MENU_INFO","entities":null,"message":"특별한 날엔 Valentine Dinner를 추천드려요! 로맨틱한 분위기의 코스요리입니다."}

                User: "메뉴 뭐 있어?"
                {"intent":"ASK_MENU_INFO","entities":null,"message":"저희 Mr.Daeback에는 Valentine Dinner, Champagne Feast 등 다양한 메뉴가 있어요!"}

                User: "발렌타인 디너"
                {"intent":"ASK_MENU_INFO","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Valentine Dinner는 로맨틱한 코스요리예요! 기본 가격은 50,000원이고, 주문하시려면 '발렌타인 디너 주세요'라고 말씀해주세요."}

                User: "발렌타인 디너가 뭐야?"
                {"intent":"ASK_MENU_INFO","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Valentine Dinner는 특별한 날을 위한 로맨틱한 코스요리입니다. 스테이크와 와인이 포함되어 있어요!"}

                User: "샴페인 피스트"
                {"intent":"ASK_MENU_INFO","entities":{"menuName":"Champagne Feast","styleName":null,"quantity":null,"addressIndex":null},"message":"Champagne Feast는 고급 샴페인과 함께하는 파티 메뉴예요! 주문하시려면 '샴페인 피스트 주세요'라고 해주세요."}

                User: "발렌타인 디너 주세요"
                {"intent":"ORDER_MENU","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Valentine Dinner 선택하셨어요! 어떤 스타일로 하실래요? Simple Style, Grand Style, Deluxe Style이 있어요."}

                User: "발렌타인 디너 그랜드로"
                {"intent":"ORDER_MENU","entities":{"menuName":"Valentine Dinner","styleName":"Grand Style","quantity":null,"addressIndex":null},"message":"Valentine Dinner Grand Style이요! 몇 개로 드릴까요?"}

                User: "발렌타인 디너 그랜드 2개"
                {"intent":"ORDER_MENU","entities":{"menuName":"Valentine Dinner","styleName":"Grand Style","quantity":2,"addressIndex":null},"message":"Valentine Dinner Grand Style 2개 주문 완료! 더 주문하실 게 있으세요?"}

                User: "심플로 해주세요"
                {"intent":"SELECT_STYLE","entities":{"menuName":null,"styleName":"Simple Style","quantity":null,"addressIndex":null},"message":"Simple Style로 선택하셨어요! 몇 개로 드릴까요?"}

                User: "그랜드 스타일"
                {"intent":"SELECT_STYLE","entities":{"menuName":null,"styleName":"Grand Style","quantity":null,"addressIndex":null},"message":"Grand Style로 선택 완료! 몇 개로 드릴까요?"}

                User: "그랜드 스타일로 할게"
                {"intent":"SELECT_STYLE","entities":{"menuName":null,"styleName":"Grand Style","quantity":null,"addressIndex":null},"message":"Grand Style로 선택했어요! 몇 개로 드릴까요?"}

                User: "그랜드로 ㄱㄱ"
                {"intent":"SELECT_STYLE","entities":{"menuName":null,"styleName":"Grand Style","quantity":null,"addressIndex":null},"message":"Grand Style로 할게요! 몇 개로 드릴까요?"}

                User: "심플로"
                {"intent":"SELECT_STYLE","entities":{"menuName":null,"styleName":"Simple Style","quantity":null,"addressIndex":null},"message":"Simple Style 선택! 몇 개로 드릴까요?"}

                User: "그랜드로 하자"
                {"intent":"SELECT_STYLE","entities":{"menuName":null,"styleName":"Grand Style","quantity":null,"addressIndex":null},"message":"Grand Style로 결정! 몇 개로 드릴까요?"}

                User: "2개요"
                {"intent":"SET_QUANTITY","entities":{"menuName":null,"styleName":null,"quantity":2,"addressIndex":null},"message":"2개 주문할게요! 더 주문하실 게 있으세요?"}

                User: "3개요"
                {"intent":"SET_QUANTITY","entities":{"menuName":null,"styleName":null,"quantity":3,"addressIndex":null},"message":"3개 주문 완료! 추가 주문 있으신가요?"}

                User: "3개로 할게"
                {"intent":"SET_QUANTITY","entities":{"menuName":null,"styleName":null,"quantity":3,"addressIndex":null},"message":"3개 주문할게요! 더 주문하실 게 있으세요?"}

                User: "1개만"
                {"intent":"SET_QUANTITY","entities":{"menuName":null,"styleName":null,"quantity":1,"addressIndex":null},"message":"1개 주문할게요! 더 주문하실 게 있으세요?"}

                User: "아니요 됐어요"
                {"intent":"ADD_TO_CART","entities":null,"message":"알겠습니다! 주문을 마무리할게요."}

                User: "장바구니에 담아줘"
                {"intent":"ADD_TO_CART","entities":null,"message":"네, 장바구니에 담을게요!"}

                User: "1번 주소로"
                {"intent":"SELECT_ADDRESS","entities":{"menuName":null,"styleName":null,"quantity":null,"addressIndex":1},"message":"1번 주소로 배달 준비할게요!"}

                User: "주문 취소할래"
                {"intent":"CANCEL_ORDER","entities":null,"message":"주문을 취소할게요. 새로운 주문을 시작해주세요!"}


                User: "발렌타인 디너 3인분으로 바꿔줘"
                {"intent":"EDIT_ORDER","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":3,"addressIndex":null},"message":"Valentine Dinner를 3인분으로 변경할게요!"}

                User: "수량 바꾸고 싶어"
                {"intent":"EDIT_ORDER","entities":null,"message":"어떤 메뉴의 수량을 변경할까요?"}

                User: "샴페인 피스트 그랜드로 바꿔"
                {"intent":"EDIT_ORDER","entities":{"menuName":"Champagne Feast","styleName":"Grand Style","quantity":null,"addressIndex":null},"message":"Champagne Feast 스타일을 Grand Style로 변경할게요!"}

                User: "발렌타인 디너 빼줘"
                {"intent":"REMOVE_ITEM","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Valentine Dinner를 삭제할게요!"}

                User: "마지막거 취소"
                {"intent":"REMOVE_ITEM","entities":{"menuName":"LAST","styleName":null,"quantity":null,"addressIndex":null},"message":"마지막 주문을 삭제할게요!"}

                User: "방금거 빼"
                {"intent":"REMOVE_ITEM","entities":{"menuName":"LAST","styleName":null,"quantity":null,"addressIndex":null},"message":"방금 추가한 메뉴를 삭제할게요!"}

                User: "하나 삭제하고 싶어"
                {"intent":"REMOVE_ITEM","entities":null,"message":"어떤 메뉴를 삭제할까요?"}
                """,
                menuMatcher.getMenuListForPrompt(),
                menuMatcher.getStyleListForPrompt()
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
            log.info("[LLM] 자연어 응답: {}", rawResponse);
            LlmResponseDto fallback = new LlmResponseDto();
            fallback.setIntent("ASK_MENU_INFO");
            fallback.setMessage(rawResponse.trim());
            return fallback;
        }
    }

    private ChatResponseDto processIntent(String userMessage, LlmResponseDto llmResponse,
                                          List<OrderItemRequestDto> currentOrder, List<String> userAddresses) {
        UserIntent intent = parseIntent(llmResponse.getIntent());
        LlmResponseDto.ExtractedEntities entities = llmResponse.getEntities();

        List<OrderItemDto> updatedOrder = cartManager.convertToOrderItemDtoList(currentOrder);
        OrderFlowState nextState = OrderFlowState.IDLE;
        UiAction uiAction = UiAction.NONE;
        String message = llmResponse.getMessage();
        String selectedAddress = null;

        // 진행 중인 아이템 찾기 (quantity == 0)
        OrderItemDto pendingItem = findPendingItem(updatedOrder);
        int pendingIdx = pendingItem != null ? updatedOrder.indexOf(pendingItem) : -1;

        switch (intent) {
            case ORDER_MENU -> {
                // 진행 중인 아이템이 있고, 수량만 말한 경우 → SET_QUANTITY처럼 처리
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

                // 진행 중인 아이템이 있고, 스타일만 말한 경우 → SELECT_STYLE처럼 처리
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
                        // 임시 아이템 생성 (수량 0으로 시작 - 아직 확정 안 됨)
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

                        // 다음 상태 결정: 스타일 → 수량 → 추가 주문 순
                        if (newItem.getServingStyleId() == null) {
                            nextState = OrderFlowState.SELECTING_STYLE;
                            message = dinnerOpt.get().getDinnerName() + " 선택하셨어요! 어떤 스타일로 하실래요? Simple Style, Grand Style, Deluxe Style이 있어요.";
                        } else if (newItem.getQuantity() == 0) {
                            nextState = OrderFlowState.SELECTING_QUANTITY;
                            message = newItem.getDinnerName() + " " + newItem.getServingStyleName() + "이요! " + buildQuantityQuestion(newItem.getDinnerName());
                        } else {
                            nextState = OrderFlowState.ASKING_MORE;
                            // LLM 메시지 사용
                        }
                    } else {
                        message = "죄송해요, '" + entities.getMenuName() + "' 메뉴를 찾을 수 없어요.";
                        nextState = OrderFlowState.SELECTING_MENU;
                    }
                }
            }
            case SELECT_STYLE -> {
                if (entities != null && entities.getStyleName() != null) {
                    // 진행 중인 아이템이 있으면 그 아이템에 적용
                    if (pendingItem != null && pendingIdx >= 0) {
                        var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
                        if (styleOpt.isPresent()) {
                            OrderItemDto updated = cartManager.applyStyleToItem(pendingItem, styleOpt.get());
                            updatedOrder.set(pendingIdx, updated);
                            uiAction = UiAction.UPDATE_ORDER_LIST;

                            // 수량이 아직 없으면 수량 선택으로
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
                    // 진행 중인 아이템이 있으면 그 아이템에 적용
                    if (pendingItem != null && pendingIdx >= 0) {
                        // 스타일이 없으면 먼저 스타일 선택
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
                // 완성된 아이템 수정 - 메뉴명 필수
                if (entities != null && entities.getMenuName() != null) {
                    // 해당 메뉴 찾기
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

                        // 수량 변경
                        if (entities.getQuantity() != null && entities.getQuantity() > 0) {
                            targetItem = cartManager.setQuantity(targetItem, entities.getQuantity());
                            updatedOrder.set(targetIdx, targetItem);
                            message = targetItem.getDinnerName() + " " + entities.getQuantity() + "개로 변경했어요!";
                            uiAction = UiAction.UPDATE_ORDER_LIST;
                        }

                        // 스타일 변경
                        if (entities.getStyleName() != null) {
                            var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
                            if (styleOpt.isPresent()) {
                                // 기존 스타일 가격 빼고 새 스타일 적용
                                targetItem = cartManager.changeStyle(targetItem, styleOpt.get());
                                updatedOrder.set(targetIdx, targetItem);
                                message = targetItem.getDinnerName() + " 스타일을 " + styleOpt.get().getStyleName() + "로 변경했어요!";
                                uiAction = UiAction.UPDATE_ORDER_LIST;
                            }
                        }

                        nextState = OrderFlowState.ASKING_MORE;
                    } else {
                        message = "'" + entities.getMenuName() + "' 메뉴가 장바구니에 없어요. 어떤 메뉴를 수정할까요?";
                        nextState = OrderFlowState.ASKING_MORE;
                    }
                } else {
                    // 메뉴명 없이 수정 요청
                    message = "어떤 메뉴를 수정할까요? 메뉴 이름을 말씀해주세요.";
                    nextState = OrderFlowState.ASKING_MORE;
                }
            }
            case REMOVE_ITEM -> {
                if (updatedOrder.isEmpty()) {
                    message = "장바구니가 비어있어요!";
                    nextState = OrderFlowState.IDLE;
                } else if (entities != null && entities.getMenuName() != null) {
                    String menuName = entities.getMenuName();

                    // "LAST" = 마지막 아이템 삭제
                    if ("LAST".equalsIgnoreCase(menuName)) {
                        OrderItemDto removed = updatedOrder.remove(updatedOrder.size() - 1);
                        message = removed.getDinnerName() + "을(를) 삭제했어요!";
                        uiAction = UiAction.UPDATE_ORDER_LIST;
                        nextState = updatedOrder.isEmpty() ? OrderFlowState.IDLE : OrderFlowState.ASKING_MORE;
                    } else {
                        // 특정 메뉴 삭제
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
                            nextState = updatedOrder.isEmpty() ? OrderFlowState.IDLE : OrderFlowState.ASKING_MORE;
                        } else {
                            message = "'" + menuName + "' 메뉴가 장바구니에 없어요.";
                            nextState = OrderFlowState.ASKING_MORE;
                        }
                    }
                } else {
                    // 메뉴명 없이 삭제 요청
                    message = "어떤 메뉴를 삭제할까요? 메뉴 이름을 말씀해주세요.";
                    nextState = OrderFlowState.ASKING_MORE;
                }
            }
            case ADD_TO_CART, CONFIRM_NO -> {
                // 완성되지 않은 아이템(수량 0) 제거
                updatedOrder.removeIf(item -> item.getQuantity() == 0);

                if (!updatedOrder.isEmpty()) {
                    if (!userAddresses.isEmpty()) {
                        nextState = OrderFlowState.SELECTING_ADDRESS;
                        message = "어느 주소로 등록해드릴까요?\n" + formatAddressList(userAddresses);
                    } else {
                        nextState = OrderFlowState.CONFIRMING;
                        uiAction = UiAction.SHOW_CONFIRM_MODAL;
                    }
                } else {
                    message = "장바구니가 비어있어요. 먼저 메뉴를 선택해주세요!";
                    nextState = OrderFlowState.SELECTING_MENU;
                }
            }
            case SELECT_ADDRESS -> {
                if (entities != null && entities.getAddressIndex() != null) {
                    int idx = entities.getAddressIndex() - 1;
                    if (idx >= 0 && idx < userAddresses.size()) {
                        selectedAddress = userAddresses.get(idx);
                        nextState = OrderFlowState.CONFIRMING;
                        uiAction = UiAction.SHOW_CONFIRM_MODAL;
                        message = selectedAddress + "로 등록해드릴게요!";
                    } else {
                        message = "올바른 주소 번호를 선택해주세요.";
                        nextState = OrderFlowState.SELECTING_ADDRESS;
                    }
                }
            }
            case CANCEL_ORDER -> {
                updatedOrder.clear();
                nextState = OrderFlowState.IDLE;
                message = "주문이 취소되었어요. 새로운 주문을 시작해주세요!";
                uiAction = UiAction.SHOW_CANCEL_CONFIRM;
            }
            case CONFIRM_YES -> nextState = OrderFlowState.ASKING_MORE;
            case ASK_MENU_INFO, GREETING -> nextState = OrderFlowState.IDLE;
            default -> nextState = OrderFlowState.IDLE;
        }

        int totalPrice = cartManager.calculateTotalPrice(updatedOrder);
        log.info("[Backend] Intent: {}, State: {}, Items: {}", intent, nextState, updatedOrder.size());

        // OrderItemDto를 OrderItemRequestDto로 변환
        List<ChatRequestDto.OrderItemRequestDto> currentOrderRequest = cartManager.convertToOrderItemRequestDtoList(updatedOrder);

        return ChatResponseDto.builder()
                .userMessage(userMessage)
                .assistantMessage(message)
                .uiAction(uiAction)
                .currentOrder(currentOrderRequest)
                .totalPrice(totalPrice)
                .selectedAddress(selectedAddress)
                .build();
    }

    private String formatAddressList(List<String> addresses) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addresses.size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, addresses.get(i)));
        }
        return sb.toString().trim();
    }

    /**
     * 메뉴별 수량 질문 생성 (Champagne Feast는 1개=2인분 설명 추가)
     */
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

    /**
     * 진행 중인 아이템 찾기 (quantity == 0)
     */
    private OrderItemDto findPendingItem(List<OrderItemDto> orderItems) {
        for (OrderItemDto item : orderItems) {
            if (item.getQuantity() == 0) {
                return item;
            }
        }
        return null;
    }
}
