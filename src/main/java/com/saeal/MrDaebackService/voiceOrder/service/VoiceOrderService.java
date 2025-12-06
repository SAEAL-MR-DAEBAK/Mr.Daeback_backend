package com.saeal.MrDaebackService.voiceOrder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saeal.MrDaebackService.cart.dto.request.CreateCartRequest;
import com.saeal.MrDaebackService.cart.dto.response.CartResponseDto;
import com.saeal.MrDaebackService.cart.enums.DeliveryMethod;
import com.saeal.MrDaebackService.cart.service.CartService;
import com.saeal.MrDaebackService.order.dto.response.OrderResponseDto;
import com.saeal.MrDaebackService.product.dto.request.CreateProductRequest;
import com.saeal.MrDaebackService.product.dto.response.ProductResponseDto;
import com.saeal.MrDaebackService.product.service.ProductService;
import com.saeal.MrDaebackService.voiceOrder.dto.LlmResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.ChatMessageDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.VoiceCheckoutRequest;
import com.saeal.MrDaebackService.voiceOrder.dto.response.ChatResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.VoiceCheckoutResponse;
import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UiAction;
import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import com.saeal.MrDaebackService.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 음성/텍스트 주문 처리 서비스
 *
 * 주요 흐름:
 * 1. IDLE → 대기
 * 2. SELECTING_ADDRESS → 배달 주소 선택
 * 3. SELECTING_MENU → 디너 메뉴 선택
 * 4. SELECTING_STYLE → 서빙 스타일 선택
 * 5. SELECTING_QUANTITY → 수량 선택
 * 6. ASKING_MORE_DINNER → 추가 디너 주문 여부 확인
 * 7. SELECTING_ADDITIONAL_MENU → 추가 메뉴 아이템 선택 (스테이크 추가, 와인 추가 등)
 * 8. ENTERING_MEMO → 메모/요청사항 입력 (일회용품, 배달 요청 등)
 * 9. CONFIRMING → 최종 확인
 * 10. CHECKOUT_READY → 결제 준비 완료 → 프론트에서 Cart API 호출 후 리디렉션
 *
 * ★ CHECKOUT_READY 상태가 되면 프론트엔드에서 Cart API를 직접 호출하여 주문 완료
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
    private final ProductService productService;
    private final CartService cartService;

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
                ? request.getCurrentOrder()
                : new ArrayList<>();

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

    private String buildSystemPrompt(List<OrderItemRequestDto> currentOrder, String selectedAddress,
            List<String> userAddresses) {
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

        return String.format(
                """
                        You are an AI order assistant for "Mr.Daeback" (미스터대백) restaurant.

                        ## Available Menus (한글 이름 = 영문 이름)
                        - 발렌타인 디너 = Valentine Dinner
                        - 프렌치 디너 = French Dinner
                        - 잉글리시 디너 = English Dinner
                        - 샴페인 축제 디너 = Champagne Feast Dinner

                        IMPORTANT: When user says Korean name, ALWAYS use the English name in entities.menuName
                        예: "샴페인 축제 디너 주세요" → menuName: "Champagne Feast Dinner"
                        예: "발렌타인 디너" → menuName: "Valentine Dinner"

                        ## Menu Details (from database)
                        %s

                        ## Available Serving Styles (한글 = 영문)
                        - 심플 = Simple Style (+0원)
                        - 그랜드 = Grand Style (+추가금)
                        - 디럭스 = Deluxe Style (+추가금)

                        ⚠️ STYLE RESTRICTION (중요!):
                        샴페인 축제 디너(Champagne Feast Dinner)는 Simple Style 불가!
                        → Grand Style 또는 Deluxe Style만 선택 가능
                        → 사용자가 심플 선택 시: "샴페인 축제 디너는 Simple Style을 제공하지 않아요. Grand Style 또는 Deluxe Style 중에서 선택해주세요!"

                        %s%s

                        ## Order Flow (IMPORTANT - Step by Step!)
                        1. FIRST ask for delivery address if not selected
                        2. Then DINNER menu selection (디너 메뉴 선택)
                        3. Then style selection (check restrictions!)
                        4. Then quantity
                        5. Ask if they want MORE DINNERS ("다른 디너 메뉴도 추가하시겠어요?")
                        6. If YES → go back to step 2
                        7. If NO → ask about ADDITIONAL MENU ITEMS ("추가 메뉴(스테이크, 와인 등)를 더 주문하시겠어요?")
                        8. Then ask for MEMO/REQUESTS ("메모나 요청사항이 있으신가요? 예: 일회용 수저, 문 앞에 놔주세요")
                        9. Confirm entire order
                        10. Proceed to checkout

                        ★ IMPORTANT: If user mentions 2+ dinners at once (e.g., "발렌타인 디너랑 샴페인 축제 디너 주세요")
                        → Process ONLY THE FIRST ONE and say: "두 가지 메뉴를 말씀하셨네요! 먼저 [첫번째 디너]부터 진행할게요. 스타일을 선택해주세요!"
                        → The second dinner will be added after the first one is complete

                        ## Your Task
                        1. Understand user's intent
                        2. Extract entities (menu name IN ENGLISH, style name IN ENGLISH, quantity, address index)
                        3. Generate a friendly Korean response message
                        4. Check style restrictions before accepting

                        ## Output Format (MUST ALWAYS be valid JSON)
                        {"intent":"ORDER_MENU","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Valentine Dinner 선택하셨어요! 스타일은 어떻게 할까요?"}

                        ## Intent Types
                        - ORDER_MENU: User wants to ORDER a dinner menu (MUST have menuName + ordering expression like "주세요", "주문", "할게요", "줘")
                        - SELECT_STYLE: User selects serving style for current item (NO menuName, only styleName like "그랜드로", "심플 스타일로 할게")
                        - SET_QUANTITY: User specifies quantity only (NO menuName, only quantity like "2인분", "3개", "3인분으로 할게")
                        - EDIT_ORDER: User wants to modify an existing order item (MUST include menuName + "바꿔", "수정", "변경")
                        - REMOVE_ITEM: User wants to delete a specific item (menuName + "빼줘", "삭제", "취소". Use "LAST" for last item)
                        - ADD_MORE_DINNER: User wants to add more DINNER (different dinner) - "다른 메뉴도 추가", "더 주문할게"
                        - NO_MORE_DINNER: User does NOT want more dinners - "디너는 됐어요", "디너는 끝"
                        - ADD_ADDITIONAL_MENU: User wants additional menu items like extra steak, wine, etc. ("스테이크 추가", "와인 추가해줘")
                        - NO_ADDITIONAL_MENU: User does NOT want additional menu items - "추가 메뉴 없어요", "없어요"
                        - SET_MEMO: User sets memo/request ("일회용 수저 넣어주세요", "문 앞에 놔주세요", "메모: ...")
                        - NO_MEMO: User has no memo - "메모 없어요", "요청사항 없어요"
                        - PROCEED_CHECKOUT: User wants to proceed to checkout ("결제할게요", "주문 완료", "결제 진행")
                        - SELECT_ADDRESS: User selects address ("1번", "첫번째")
                        - CANCEL_ORDER: User cancels ALL orders (전체 취소)
                        - ASK_MENU_INFO: User asks about menu OR says menu name only without ordering expression
                        - GREETING: Greetings or casual talk
                        - CONFIRM_YES: Positive response ("네", "좋아요", "응", "확인")
                        - CONFIRM_NO: Negative response ("아니요", "없어요", "괜찮아요")

                        ## Rules
                        - ALWAYS respond in JSON format
                        - menuName MUST be English: Valentine Dinner, French Dinner, English Dinner, Champagne Feast Dinner
                        - styleName MUST be English: Simple Style, Grand Style, Deluxe Style
                        - DO NOT default quantity to 1 - only set if user explicitly says
                        - Restaurant name is "Mr.Daeback" (미스터대백)
                        - If address not selected, ask for address FIRST
                        - Champagne Feast Dinner + Simple Style → REJECT and ask for Grand or Deluxe

                        ## Few-Shot Examples

                        User: "안녕하세요"
                        {"intent":"GREETING","entities":null,"message":"안녕하세요! Mr.Daeback입니다. 무엇을 주문하시겠어요?"}

                        User: "아 졸려"
                        {"intent":"GREETING","entities":null,"message":"피곤하시군요! Mr.Daeback의 맛있는 음식으로 기분 전환 어떠세요?"}

                        User: "와이프 생일인데 뭐가 좋을까?"
                        {"intent":"ASK_MENU_INFO","entities":null,"message":"특별한 날엔 Valentine Dinner를 추천드려요! 로맨틱한 분위기의 코스요리입니다."}

                        User: "메뉴 뭐 있어?"
                        {"intent":"ASK_MENU_INFO","entities":null,"message":"저희 Mr.Daeback에는 발렌타인 디너, 샴페인 축제 등 다양한 메뉴가 있어요!"}

                        User: "발렌타인 디너"
                        {"intent":"ASK_MENU_INFO","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Valentine Dinner는 로맨틱한 코스요리예요! 기본 가격은 50,000원이고, 주문하시려면 '발렌타인 디너 주세요'라고 말씀해주세요."}

                        User: "발렌타인 디너가 뭐야?"
                        {"intent":"ASK_MENU_INFO","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Valentine Dinner는 특별한 날을 위한 로맨틱한 코스요리입니다. 스테이크와 와인이 포함되어 있어요!"}

                        User: "샴페인 피스트"
                        {"intent":"ASK_MENU_INFO","entities":{"menuName":"Champagne Feast Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Champagne Feast Dinner는 고급 샴페인과 함께하는 파티 메뉴예요! 주문하시려면 '샴페인 축제 디너 주세요'라고 해주세요."}

                        User: "샴페인 축제 디너 주세요"
                        {"intent":"ORDER_MENU","entities":{"menuName":"Champagne Feast Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Champagne Feast Dinner 선택하셨어요! 이 메뉴는 Grand Style 또는 Deluxe Style만 가능해요. 어떤 스타일로 하실래요?"}

                        User: "샴페인 축제 디너 심플로"
                        {"intent":"ORDER_MENU","entities":{"menuName":"Champagne Feast Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"죄송해요, Champagne Feast Dinner는 Simple Style을 제공하지 않아요. Grand Style 또는 Deluxe Style 중에서 선택해주세요!"}

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

                        User: "1번 주소로"
                        {"intent":"SELECT_ADDRESS","entities":{"menuName":null,"styleName":null,"quantity":null,"addressIndex":1},"message":"1번 주소로 배달 준비할게요!"}

                        User: "주문 취소할래"
                        {"intent":"CANCEL_ORDER","entities":null,"message":"주문을 취소할게요. 새로운 주문을 시작해주세요!"}

                        User: "발렌타인 디너 3인분으로 바꿔줘"
                        {"intent":"EDIT_ORDER","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":3,"addressIndex":null},"message":"Valentine Dinner를 3인분으로 변경할게요!"}

                        User: "수량 바꾸고 싶어"
                        {"intent":"EDIT_ORDER","entities":null,"message":"어떤 메뉴의 수량을 변경할까요?"}

                        User: "샴페인 축제 그랜드로 바꿔"
                        {"intent":"EDIT_ORDER","entities":{"menuName":"Champagne Feast","styleName":"Grand Style","quantity":null,"addressIndex":null},"message":"Champagne Feast 스타일을 Grand Style로 변경할게요!"}

                        User: "발렌타인 디너 빼줘"
                        {"intent":"REMOVE_ITEM","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"Valentine Dinner를 삭제할게요!"}

                        User: "마지막거 취소"
                        {"intent":"REMOVE_ITEM","entities":{"menuName":"LAST","styleName":null,"quantity":null,"addressIndex":null},"message":"마지막 주문을 삭제할게요!"}

                        User: "방금거 빼"
                        {"intent":"REMOVE_ITEM","entities":{"menuName":"LAST","styleName":null,"quantity":null,"addressIndex":null},"message":"방금 추가한 메뉴를 삭제할게요!"}

                        User: "하나 삭제하고 싶어"
                        {"intent":"REMOVE_ITEM","entities":null,"message":"어떤 메뉴를 삭제할까요?"}

                        ## More Dinner Examples (ASKING_MORE_DINNER state)
                        User: "다른 디너도 주문할게요"
                        {"intent":"ADD_MORE_DINNER","entities":null,"message":"네! 어떤 디너를 추가하시겠어요?"}

                        User: "프렌치 디너도 추가해줘"
                        {"intent":"ORDER_MENU","entities":{"menuName":"French Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"French Dinner 추가할게요! 스타일을 선택해주세요."}

                        User: "디너는 이걸로 됐어요"
                        {"intent":"NO_MORE_DINNER","entities":null,"message":"알겠어요! 추가 메뉴(스테이크, 와인 등)를 더 주문하시겠어요?"}

                        User: "아니요 디너 끝이요"
                        {"intent":"NO_MORE_DINNER","entities":null,"message":"디너 주문 완료! 추가 메뉴(스테이크, 와인 등)를 더 주문하시겠어요?"}

                        ## Additional Menu Examples (SELECTING_ADDITIONAL_MENU state)
                        User: "스테이크 하나 추가해주세요"
                        {"intent":"ADD_ADDITIONAL_MENU","entities":{"menuItemName":"steak","quantity":1},"message":"스테이크 1개 추가할게요! 다른 추가 메뉴도 있으신가요?"}

                        User: "와인 2병 추가요"
                        {"intent":"ADD_ADDITIONAL_MENU","entities":{"menuItemName":"wine","quantity":2},"message":"와인 2병 추가했어요! 또 추가하실 메뉴 있으세요?"}

                        User: "추가 메뉴 없어요"
                        {"intent":"NO_ADDITIONAL_MENU","entities":null,"message":"알겠어요! 메모나 요청사항이 있으신가요? (예: 일회용 수저, 문 앞에 놔주세요)"}

                        User: "추가 안 할래요"
                        {"intent":"NO_ADDITIONAL_MENU","entities":null,"message":"추가 메뉴 없이 진행할게요! 메모나 요청사항이 있으신가요?"}

                        ## Memo Examples (ENTERING_MEMO state)
                        ★ IMPORTANT: For memo/request, copy the user's words EXACTLY as they said. Do not modify or interpret.

                        User: "일회용 수저 넣어주세요"
                        {"intent":"SET_MEMO","entities":{"memo":"일회용 수저 넣어주세요"},"message":"'일회용 수저 넣어주세요' 메모 완료! 주문을 확정하시겠어요?"}

                        User: "문 앞에 놔주세요"
                        {"intent":"SET_MEMO","entities":{"memo":"문 앞에 놔주세요"},"message":"'문 앞에 놔주세요' 메모 완료! 주문을 확정하시겠어요?"}

                        User: "일회용품 갖다줘"
                        {"intent":"SET_MEMO","entities":{"memo":"일회용품 갖다줘"},"message":"'일회용품 갖다줘' 메모 완료! 주문을 확정하시겠어요?"}

                        User: "젓가락 빼주세요"
                        {"intent":"SET_MEMO","entities":{"memo":"젓가락 빼주세요"},"message":"'젓가락 빼주세요' 메모 완료! 주문을 확정하시겠어요?"}

                        User: "경비실에 맡겨주세요"
                        {"intent":"SET_MEMO","entities":{"memo":"경비실에 맡겨주세요"},"message":"'경비실에 맡겨주세요' 메모 완료! 주문을 확정하시겠어요?"}

                        User: "벨 누르지 마세요"
                        {"intent":"SET_MEMO","entities":{"memo":"벨 누르지 마세요"},"message":"'벨 누르지 마세요' 메모 완료! 주문을 확정하시겠어요?"}

                        User: "메모 없어요"
                        {"intent":"NO_MEMO","entities":null,"message":"메모 없이 진행할게요! 주문을 확정하시겠어요?"}

                        User: "요청사항 없어요"
                        {"intent":"NO_MEMO","entities":null,"message":"알겠어요! 주문을 확정하시겠어요?"}

                        ## Checkout Examples (CONFIRMING state)
                        User: "네 확정이요"
                        {"intent":"CONFIRM_YES","entities":null,"message":"주문이 확정되었습니다! 결제 페이지로 이동할게요."}

                        User: "결제할게요"
                        {"intent":"PROCEED_CHECKOUT","entities":null,"message":"결제 페이지로 이동합니다!"}

                        User: "주문 완료"
                        {"intent":"PROCEED_CHECKOUT","entities":null,"message":"주문 완료! 결제 페이지로 이동할게요."}

                        ## Multiple Dinners at Once (중요!)
                        User: "발렌타인 디너랑 샴페인 축제 디너 주세요"
                        {"intent":"ORDER_MENU","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null,"pendingMenuName":"Champagne Feast Dinner"},"message":"두 가지 메뉴를 말씀하셨네요! 먼저 Valentine Dinner부터 진행할게요. 스타일을 선택해주세요! (Simple, Grand, Deluxe)"}

                        ## Adding Same Dinner Again (같은 디너 추가 주문)
                        User: "샴페인 축제 디너 하나 더"
                        {"intent":"ORDER_MENU","entities":{"menuName":"Champagne Feast Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"샴페인 축제 디너 하나 더 추가할게요! 스타일을 선택해주세요. (Grand, Deluxe)"}

                        User: "같은거 하나 더 주문할게"
                        {"intent":"ORDER_MENU","entities":{"menuName":"SAME_AS_PREVIOUS","styleName":null,"quantity":null,"addressIndex":null},"message":"같은 메뉴 하나 더 추가할게요! 스타일을 선택해주세요."}

                        User: "발렌타인 디너 하나 더 추가"
                        {"intent":"ORDER_MENU","entities":{"menuName":"Valentine Dinner","styleName":null,"quantity":null,"addressIndex":null},"message":"발렌타인 디너 하나 더 추가할게요! 스타일을 선택해주세요. (Simple, Grand, Deluxe)"}

                        ## Style Selection After Adding Dinner (디너 추가 후 스타일 선택)
                        User: "디럭스"
                        {"intent":"SELECT_STYLE","entities":{"menuName":null,"styleName":"Deluxe Style","quantity":null,"addressIndex":null},"message":"Deluxe Style로 선택했어요! 몇 개로 드릴까요?"}

                        User: "디럭스로"
                        {"intent":"SELECT_STYLE","entities":{"menuName":null,"styleName":"Deluxe Style","quantity":null,"addressIndex":null},"message":"Deluxe Style 선택 완료! 몇 개로 드릴까요?"}

                        User: "그랜드"
                        {"intent":"SELECT_STYLE","entities":{"menuName":null,"styleName":"Grand Style","quantity":null,"addressIndex":null},"message":"Grand Style로 선택했어요! 몇 개로 드릴까요?"}

                        ## Korean Additional Menu Items (한글 추가 메뉴)
                        User: "샐러드 2개 추가"
                        {"intent":"ADD_ADDITIONAL_MENU","entities":{"menuItemName":"salad","quantity":2},"message":"샐러드 2개 추가했어요! 다른 추가 메뉴도 있으신가요?"}

                        User: "샐러드 추가해줘"
                        {"intent":"ADD_ADDITIONAL_MENU","entities":{"menuItemName":"salad","quantity":1},"message":"샐러드 1개 추가할게요! 다른 추가 메뉴도 있으신가요?"}

                        User: "와인 한 병"
                        {"intent":"ADD_ADDITIONAL_MENU","entities":{"menuItemName":"wine","quantity":1},"message":"와인 1병 추가했어요! 다른 추가 메뉴도 있으신가요?"}

                        User: "스테이크 하나 더"
                        {"intent":"ADD_ADDITIONAL_MENU","entities":{"menuItemName":"steak","quantity":1},"message":"스테이크 1개 추가했어요! 다른 추가 메뉴도 있으신가요?"}

                        User: "수프 추가요"
                        {"intent":"ADD_ADDITIONAL_MENU","entities":{"menuItemName":"soup","quantity":1},"message":"수프 1개 추가했어요! 다른 추가 메뉴도 있으신가요?"}

                        User: "빵이랑 샐러드 추가"
                        {"intent":"ADD_ADDITIONAL_MENU","entities":{"menuItemName":"bread","quantity":1},"message":"빵 추가할게요! 샐러드도 추가하시겠어요?"}

                        ## Short Responses (짧은 응답)
                        User: "응"
                        {"intent":"CONFIRM_YES","entities":null,"message":"알겠어요!"}

                        User: "네"
                        {"intent":"CONFIRM_YES","entities":null,"message":"알겠습니다!"}

                        User: "아니"
                        {"intent":"CONFIRM_NO","entities":null,"message":"알겠어요!"}

                        User: "아니요"
                        {"intent":"CONFIRM_NO","entities":null,"message":"알겠습니다!"}

                        User: "없어"
                        {"intent":"NO_ADDITIONAL_MENU","entities":null,"message":"알겠어요! 메모나 요청사항이 있으신가요?"}

                        User: "없어요"
                        {"intent":"NO_ADDITIONAL_MENU","entities":null,"message":"추가 메뉴 없이 진행할게요! 메모나 요청사항이 있으신가요?"}

                        User: "됐어"
                        {"intent":"NO_MORE_DINNER","entities":null,"message":"디너 주문 완료! 추가 메뉴(스테이크, 와인 등)를 더 주문하시겠어요?"}

                        User: "됐어요"
                        {"intent":"NO_MORE_DINNER","entities":null,"message":"디너 주문 완료했어요! 추가 메뉴를 더 주문하시겠어요?"}

                        User: "괜찮아요"
                        {"intent":"NO_MEMO","entities":null,"message":"메모 없이 진행할게요! 주문을 확정하시겠어요?"}

                        User: "그냥 진행해줘"
                        {"intent":"CONFIRM_YES","entities":null,"message":"바로 진행할게요!"}

                        ## Context-aware Short Responses (문맥에 따른 짧은 응답)
                        ★ IMPORTANT: Short responses like "없어", "아니", "응" should be interpreted based on context:
                        - In ASKING_MORE_DINNER state: "없어"/"아니" → NO_MORE_DINNER
                        - In SELECTING_ADDITIONAL_MENU state: "없어"/"아니" → NO_ADDITIONAL_MENU
                        - In ENTERING_MEMO state: "없어"/"아니" → NO_MEMO
                        - In CONFIRMING state: "응"/"네" → CONFIRM_YES, "아니" → CONFIRM_NO
                        """,

                menuMatcher.getMenuListForPrompt(),
                menuMatcher.getStyleListForPrompt(),
                orderSummary.toString(),
                addressInfo.toString());
    }

    private LlmResponseDto parseLlmResponse(String rawResponse) {
        try {
            String jsonContent = rawResponse.trim();
            if (jsonContent.startsWith("```json"))
                jsonContent = jsonContent.substring(7);
            if (jsonContent.startsWith("```"))
                jsonContent = jsonContent.substring(3);
            if (jsonContent.endsWith("```"))
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
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
        String memo = null;  // 메모/요청사항

        // 진행 중인 아이템 찾기 (quantity == 0)
        OrderItemDto pendingItem = findPendingItem(updatedOrder);
        int pendingIdx = pendingItem != null ? updatedOrder.indexOf(pendingItem) : -1;

        switch (intent) {
            case GREETING -> {
                // ★ 주소가 없으면 먼저 주소 선택 요청
                if (finalSelectedAddress == null || finalSelectedAddress.isEmpty()) {
                    if (!userAddresses.isEmpty()) {
                        message = "안녕하세요! Mr.Daeback입니다. 🍽️\n먼저 배달받으실 주소를 선택해주세요!\n"
                                + formatAddressList(userAddresses);
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
                if (pendingItem != null && entities != null && entities.getQuantity() != null
                        && entities.getMenuName() == null) {
                    if (pendingItem.getServingStyleId() != null) {
                        OrderItemDto updated = cartManager.setQuantity(pendingItem, entities.getQuantity());
                        updatedOrder.set(pendingIdx, updated);
                        nextState = OrderFlowState.ASKING_MORE_DINNER;
                        uiAction = UiAction.UPDATE_ORDER_LIST;
                        message = updated.getDinnerName() + " " + entities.getQuantity() + "개 주문 완료! 다른 디너 메뉴도 추가하시겠어요?";
                    } else {
                        message = "먼저 스타일을 선택해주세요!";
                        nextState = OrderFlowState.SELECTING_STYLE;
                    }
                    break;
                }

                // 진행 중인 아이템이 있고, 스타일만 말한 경우
                if (pendingItem != null && entities != null && entities.getStyleName() != null
                        && entities.getMenuName() == null) {
                    var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
                    if (styleOpt.isPresent()) {
                        OrderItemDto updated = cartManager.applyStyleToItem(pendingItem, styleOpt.get());
                        updatedOrder.set(pendingIdx, updated);
                        uiAction = UiAction.UPDATE_ORDER_LIST;

                        if (updated.getQuantity() == 0) {
                            nextState = OrderFlowState.SELECTING_QUANTITY;
                            message = styleOpt.get().getStyleName() + "로 선택하셨어요! "
                                    + buildQuantityQuestion(updated.getDinnerName());
                        } else {
                            nextState = OrderFlowState.ASKING_MORE_DINNER;
                            message = updated.getDinnerName() + " " + styleOpt.get().getStyleName()
                                    + " 적용 완료! 다른 디너 메뉴도 추가하시겠어요?";
                        }
                    } else {
                        message = "죄송해요, '" + entities.getStyleName() + "' 스타일을 찾을 수 없어요.";
                        nextState = OrderFlowState.SELECTING_STYLE;
                    }
                    break;
                }

                if (entities != null && entities.getMenuName() != null) {
                    // ★ "하나 더", "추가" 패턴 감지 - 이 경우 quantity를 무시하고 스타일 선택부터
                    boolean isAddMorePattern = userMessage.contains("하나 더") || userMessage.contains("하나더")
                            || userMessage.contains("한 개 더") || userMessage.contains("한개 더")
                            || (userMessage.contains("추가") && !userMessage.contains("추가 메뉴"));

                    // 진행 중인 아이템이 있으면 먼저 완성하도록 안내
                    // 단, 새 메뉴 추가 요청이면 pendingItem 무시하고 새 아이템 추가
                    if (pendingItem != null && !isAddMorePattern) {
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
                            // ★ 스타일 제한 검사 (샴페인 축제 디너 + Simple Style 불가)
                            if (!menuMatcher.isStyleAvailableForDinner(newItem.getDinnerName(), entities.getStyleName())) {
                                message = newItem.getDinnerName() + "는 Simple Style을 제공하지 않아요. Grand Style 또는 Deluxe Style 중에서 선택해주세요!";
                                nextState = OrderFlowState.SELECTING_STYLE;
                                updatedOrder.add(newItem);
                                uiAction = UiAction.UPDATE_ORDER_LIST;
                                break;
                            }
                            var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
                            if (styleOpt.isPresent()) {
                                newItem = cartManager.applyStyleToItem(newItem, styleOpt.get());
                            }
                        }

                        // ★ 수량도 함께 지정된 경우 (단, "하나 더" 패턴이면 수량 무시)
                        if (entities.getQuantity() != null && entities.getQuantity() > 0 && !isAddMorePattern) {
                            newItem = cartManager.setQuantity(newItem, entities.getQuantity());
                        }

                        updatedOrder.add(newItem);
                        uiAction = UiAction.UPDATE_ORDER_LIST;

                        // 다음 상태 결정
                        if (newItem.getServingStyleId() == null) {
                            nextState = OrderFlowState.SELECTING_STYLE;
                            String availableStyles = menuMatcher.getAvailableStylesForDinner(dinnerOpt.get().getDinnerName());
                            message = dinnerOpt.get().getDinnerName()
                                    + " 추가할게요! 스타일을 선택해주세요. (" + availableStyles + ")";
                        } else if (newItem.getQuantity() == 0) {
                            nextState = OrderFlowState.SELECTING_QUANTITY;
                            message = newItem.getDinnerName() + " " + newItem.getServingStyleName() + "이요! "
                                    + buildQuantityQuestion(newItem.getDinnerName());
                        } else {
                            nextState = OrderFlowState.ASKING_MORE_DINNER;
                            message = newItem.getDinnerName() + " " + newItem.getServingStyleName() + " "
                                    + newItem.getQuantity() + "개 주문 완료! 다른 디너 메뉴도 추가하시겠어요?";
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
                        // ★ 스타일 제한 검사 (샴페인 축제 디너 + Simple Style 불가)
                        if (!menuMatcher.isStyleAvailableForDinner(pendingItem.getDinnerName(), entities.getStyleName())) {
                            message = pendingItem.getDinnerName() + "는 Simple Style을 제공하지 않아요. Grand Style 또는 Deluxe Style 중에서 선택해주세요!";
                            nextState = OrderFlowState.SELECTING_STYLE;
                            break;
                        }
                        var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
                        if (styleOpt.isPresent()) {
                            OrderItemDto updated = cartManager.applyStyleToItem(pendingItem, styleOpt.get());
                            updatedOrder.set(pendingIdx, updated);
                            uiAction = UiAction.UPDATE_ORDER_LIST;

                            if (updated.getQuantity() == 0) {
                                nextState = OrderFlowState.SELECTING_QUANTITY;
                                message = styleOpt.get().getStyleName() + "로 선택하셨어요! "
                                        + buildQuantityQuestion(updated.getDinnerName());
                            } else {
                                nextState = OrderFlowState.ASKING_MORE_DINNER;
                                message = updated.getDinnerName() + " " + styleOpt.get().getStyleName()
                                        + " 주문 완료! 다른 디너 메뉴도 추가하시겠어요?";
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
                            nextState = OrderFlowState.ASKING_MORE_DINNER;
                            uiAction = UiAction.UPDATE_ORDER_LIST;
                            message = updated.getDinnerName() + " " + entities.getQuantity()
                                    + "개 주문 완료! 다른 디너 메뉴도 추가하시겠어요?";
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

            // ★ 추가 디너 주문 확인
            case ADD_MORE_DINNER -> {
                nextState = OrderFlowState.SELECTING_MENU;
                message = "네! 어떤 디너를 추가하시겠어요?";
            }

            case NO_MORE_DINNER -> {
                // 디너 주문 끝 → 추가 메뉴 선택으로
                nextState = OrderFlowState.SELECTING_ADDITIONAL_MENU;
                message = "디너 주문 완료! 추가 메뉴(스테이크, 와인 등)를 더 주문하시겠어요?";
            }

            // ★ 추가 메뉴 아이템 (스테이크, 와인 등 개별 메뉴)
            case ADD_ADDITIONAL_MENU -> {
                // TODO: 추가 메뉴 아이템 처리 로직 (현재는 메시지만)
                nextState = OrderFlowState.SELECTING_ADDITIONAL_MENU;
                message = llmResponse.getMessage() != null ? llmResponse.getMessage()
                        : "추가 메뉴를 담았어요! 또 추가하실 메뉴 있으세요?";
            }

            case NO_ADDITIONAL_MENU -> {
                // 추가 메뉴 끝 → 메모 입력으로
                nextState = OrderFlowState.ENTERING_MEMO;
                message = "추가 메뉴 없이 진행할게요! 메모나 요청사항이 있으신가요? (예: 일회용 수저, 문 앞에 놔주세요)";
            }

            // ★ 메모/요청사항
            case SET_MEMO -> {
                if (entities != null && entities.getMemo() != null) {
                    memo = entities.getMemo();
                }
                nextState = OrderFlowState.CONFIRMING;
                uiAction = UiAction.SHOW_CONFIRM_MODAL;
                message = (memo != null ? "'" + memo + "' 메모 완료! " : "") + "주문을 확정하시겠어요?\n"
                        + buildOrderSummary(updatedOrder, finalSelectedAddress);
            }

            case NO_MEMO -> {
                nextState = OrderFlowState.CONFIRMING;
                uiAction = UiAction.SHOW_CONFIRM_MODAL;
                message = "메모 없이 진행할게요! 주문을 확정하시겠어요?\n"
                        + buildOrderSummary(updatedOrder, finalSelectedAddress);
            }

            // ★ 결제 진행
            case PROCEED_CHECKOUT -> {
                // 미완성 아이템 제거 (수량 0 또는 스타일 미선택)
                updatedOrder.removeIf(item -> item.getQuantity() == 0 || item.getServingStyleId() == null);
                if (updatedOrder.isEmpty()) {
                    message = "장바구니가 비어있어요!";
                    nextState = OrderFlowState.SELECTING_MENU;
                } else {
                    nextState = OrderFlowState.CHECKOUT_READY;
                    uiAction = UiAction.PROCEED_TO_CHECKOUT;
                    message = "결제 페이지로 이동합니다!";
                }
            }

            case EDIT_ORDER -> {
                if (entities != null && entities.getMenuName() != null) {
                    int targetIdx = -1;
                    for (int i = 0; i < updatedOrder.size(); i++) {
                        if (updatedOrder.get(i).getDinnerName().equalsIgnoreCase(entities.getMenuName())
                                || menuMatcher.isMatchingMenu(updatedOrder.get(i).getDinnerName(),
                                        entities.getMenuName())) {
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
                                message = targetItem.getDinnerName() + " 스타일을 " + styleOpt.get().getStyleName()
                                        + "로 변경했어요!";
                                uiAction = UiAction.UPDATE_ORDER_LIST;
                            }
                        }

                        nextState = OrderFlowState.ASKING_MORE_DINNER;
                    } else {
                        message = "'" + entities.getMenuName() + "' 메뉴가 장바구니에 없어요.";
                        nextState = OrderFlowState.ASKING_MORE_DINNER;
                    }
                } else {
                    message = "어떤 메뉴를 수정할까요?";
                    nextState = OrderFlowState.ASKING_MORE_DINNER;
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
                        nextState = updatedOrder.isEmpty() ? OrderFlowState.SELECTING_MENU : OrderFlowState.ASKING_MORE_DINNER;
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
                            nextState = updatedOrder.isEmpty() ? OrderFlowState.SELECTING_MENU
                                    : OrderFlowState.ASKING_MORE_DINNER;
                        } else {
                            message = "'" + menuName + "' 메뉴가 장바구니에 없어요.";
                            nextState = OrderFlowState.ASKING_MORE_DINNER;
                        }
                    }
                } else {
                    message = "어떤 메뉴를 삭제할까요?";
                    nextState = OrderFlowState.ASKING_MORE_DINNER;
                }
            }

            case ADD_TO_CART, CONFIRM_NO -> {
                // 미완성 아이템 제거 (수량 0 또는 스타일 미선택)
                updatedOrder.removeIf(item -> item.getQuantity() == 0 || item.getServingStyleId() == null);

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
                // ★ 확정 → CHECKOUT_READY 상태 → 프론트에서 Cart API 호출 후 리디렉션
                // 미완성 아이템 제거 (수량 0 또는 스타일 미선택)
                updatedOrder.removeIf(item -> item.getQuantity() == 0 || item.getServingStyleId() == null);

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
                    // ★ 확정 시 CHECKOUT_READY로 전환 → 프론트에서 Cart API 호출
                    nextState = OrderFlowState.CHECKOUT_READY;
                    uiAction = UiAction.PROCEED_TO_CHECKOUT;
                    message = "주문이 확정되었습니다! 결제 페이지로 이동합니다.";
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
                .flowState(nextState)
                .uiAction(uiAction)
                .currentOrder(updatedOrder)
                .totalPrice(totalPrice)
                .selectedAddress(finalSelectedAddress)
                .memo(memo)
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
        if (intentStr == null)
            return UserIntent.UNKNOWN;
        try {
            return UserIntent.valueOf(intentStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserIntent.UNKNOWN;
        }
    }

    /**
     * 진행 중인 아이템 찾기
     * - 수량이 0인 아이템 (완전히 미완성)
     * - 또는 스타일이 선택되지 않은 아이템 (스타일 선택 대기)
     */
    private OrderItemDto findPendingItem(List<OrderItemDto> orderItems) {
        // 1순위: 수량이 0인 아이템 (스타일/수량 모두 미선택)
        for (OrderItemDto item : orderItems) {
            if (item.getQuantity() == 0) {
                return item;
            }
        }
        // 2순위: 스타일이 선택되지 않은 아이템 (스타일만 미선택)
        for (OrderItemDto item : orderItems) {
            if (item.getServingStyleId() == null) {
                return item;
            }
        }
        return null;
    }

    /**
     * 음성 주문 결제 처리
     * - Product 생성 → Cart 생성 → Order 생성 (checkout)
     * - 프론트에서 currentOrder 그대로 전송받아 처리
     */
    @Transactional
    public VoiceCheckoutResponse checkout(VoiceCheckoutRequest request, UUID userId) {
        try {
            log.info("Voice checkout started for user: {}", userId);

            if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
                return VoiceCheckoutResponse.failure("주문할 상품이 없습니다.");
            }

            // 1. Product 생성
            List<String> productIds = new ArrayList<>();
            for (VoiceCheckoutRequest.OrderItemRequest item : request.getOrderItems()) {
                if (item.getDinnerId() == null || item.getServingStyleId() == null || item.getQuantity() <= 0) {
                    log.warn("Invalid order item: {}", item);
                    continue;
                }

                CreateProductRequest productRequest = CreateProductRequest.builder()
                        .dinnerId(item.getDinnerId())
                        .servingStyleId(item.getServingStyleId())
                        .quantity(item.getQuantity())
                        .memo(request.getMemo())
                        .address(request.getDeliveryAddress())
                        .build();

                ProductResponseDto product = productService.createProduct(productRequest);
                productIds.add(product.getId().toString());
                log.info("Created product: {} for dinner: {}", product.getId(), item.getDinnerId());
            }

            if (productIds.isEmpty()) {
                return VoiceCheckoutResponse.failure("유효한 주문 상품이 없습니다.");
            }

            // 2. Cart 생성
            List<CreateCartRequest.CartItemRequest> cartItems = new ArrayList<>();
            for (String productId : productIds) {
                CreateCartRequest.CartItemRequest cartItem = new CreateCartRequest.CartItemRequest();
                cartItem.setProductId(productId);
                cartItem.setQuantity(1);  // Product에 이미 수량이 반영되어 있음
                cartItems.add(cartItem);
            }

            CreateCartRequest cartRequest = new CreateCartRequest();
            cartRequest.setItems(cartItems);
            cartRequest.setDeliveryAddress(request.getDeliveryAddress());
            cartRequest.setDeliveryMethod(DeliveryMethod.Delivery);
            cartRequest.setMemo(request.getMemo());

            CartResponseDto cart = cartService.createCart(cartRequest);
            log.info("Created cart: {}", cart.getId());

            // 3. Checkout (Cart → Order)
            OrderResponseDto order = cartService.checkout(UUID.fromString(cart.getId()));
            log.info("Order created: {} with orderNumber: {}", order.getId(), order.getOrderNumber());

            return VoiceCheckoutResponse.success(
                    order.getId().toString(),
                    order.getOrderNumber(),
                    order.getGrandTotal()
            );

        } catch (Exception e) {
            log.error("Voice checkout failed", e);
            return VoiceCheckoutResponse.failure(e.getMessage());
        }
    }
}
