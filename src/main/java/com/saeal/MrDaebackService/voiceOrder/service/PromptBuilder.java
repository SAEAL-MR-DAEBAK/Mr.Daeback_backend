package com.saeal.MrDaebackService.voiceOrder.service;

import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM 시스템 프롬프트 생성 담당 (간소화 버전)
 *
 * ★ 새 플로우:
 * 1. 첫 인사 (기념일/배달시간 자동 추출) → ORDER_START
 * 2. 메뉴 선택 → ORDER_MENU
 * 3. 스타일 선택 → ORDER_MENU (자동 1개 추가)
 * 4. 구성요소 변경 → CUSTOMIZE_MENU
 * 5. 결제 → PROCEED_CHECKOUT
 */
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final MenuMatcher menuMatcher;

    /**
     * LLM 시스템 프롬프트 생성
     */
    public String build(List<OrderItemRequestDto> currentOrder,
                        String selectedAddress,
                        List<String> userAddresses,
                        String currentFlowState) {

        String orderSummary = buildOrderSummary(currentOrder);
        String addressInfo = buildAddressInfo(selectedAddress);

        return String.format(
                BASE_PROMPT_TEMPLATE,
                menuMatcher.getMenuListForPrompt(),
                orderSummary,
                addressInfo
        );
    }

    private String buildOrderSummary(List<OrderItemRequestDto> currentOrder) {
        if (currentOrder == null || currentOrder.isEmpty()) {
            return "## Current Order: 없음";
        }

        StringBuilder sb = new StringBuilder("## Current Order\n");
        for (int i = 0; i < currentOrder.size(); i++) {
            OrderItemRequestDto item = currentOrder.get(i);
            sb.append(String.format("%d. %s (%s) x%d = %,d원\n",
                    i + 1,
                    item.getDinnerName(),
                    item.getServingStyleName() != null ? item.getServingStyleName() : "스타일 미선택",
                    item.getQuantity(),
                    item.getTotalPrice()));
        }
        return sb.toString();
    }

    private String buildAddressInfo(String selectedAddress) {
        if (selectedAddress != null && !selectedAddress.isEmpty()) {
            return "## 배달 주소: " + selectedAddress;
        }
        return "## 배달 주소: 자동 설정됨";
    }

    // ============================================================
    // 기본 프롬프트 템플릿 (초간소화 버전)
    // ============================================================
    private static final String BASE_PROMPT_TEMPLATE = """
            RESPOND ONLY WITH A SINGLE JSON OBJECT. NO OTHER TEXT.

            {"intent":"INTENT_NAME","entities":{},"message":"한글 응답"}

            ## INTENT TYPES (use ONLY these)
            - ORDER_START: 첫 인사, 기념일 언급, 배달시간 언급
            - ORDER_MENU: 메뉴 선택 또는 스타일 선택
            - CUSTOMIZE_MENU: 구성요소 변경 요청
            - PROCEED_CHECKOUT: "응", "네", "ㅇ", "결제", "확인", "주문"
            - CANCEL_ORDER: "취소", "안 할래"
            - ASK_MENU_INFO: 메뉴 정보 질문
            - GREETING: 단순 인사 ("안녕", "하이")

            ## MENUS (entities.menuName은 반드시 영문!)
            - 발렌타인/발렌타인 디너 → "Valentine Dinner"
            - 프렌치/프렌치 디너 → "French Dinner"
            - 잉글리시/잉글리시 디너 → "English Dinner"
            - 샴페인/샴페인 축제 디너 → "Champagne Feast Dinner"

            ## STYLES (entities.styleName은 반드시 영문!)
            - 심플/심플 스타일 → "Simple Style"
            - 그랜드/그랜드 스타일 → "Grand Style"
            - 디럭스/디럭스 스타일 → "Deluxe Style"

            ⚠️ Champagne Feast Dinner는 Simple Style 불가!

            ## Menu Details
            %s

            %s
            %s

            ## EXAMPLES

            User: "안녕하세요" / "주문할게요"
            {"intent":"GREETING","entities":{},"message":"안녕하세요!"}

            User: "모레가 친구 생일이에요"
            {"intent":"ORDER_START","entities":{"occasionType":"생일","deliveryDate":"모레"},"message":""}

            User: "발렌타인 디너 주세요"
            {"intent":"ORDER_MENU","entities":{"menuName":"Valentine Dinner"},"message":""}

            User: "그랜드 스타일"
            {"intent":"ORDER_MENU","entities":{"styleName":"Grand Style"},"message":""}

            User: "커피 1포트 추가해줘" / "샴페인 2병으로 변경해줘"
            {"intent":"CUSTOMIZE_MENU","entities":{"menuItemName":"coffee","menuItemQuantity":1},"message":""}

            User: "응" / "네" / "결제할게요" / "ㅇ"
            {"intent":"PROCEED_CHECKOUT","entities":{},"message":""}

            User: "취소할게요"
            {"intent":"CANCEL_ORDER","entities":{},"message":""}

            CRITICAL RULES:
            1. OUTPUT ONLY JSON - no explanations, no markdown
            2. menuName/styleName MUST be English
            3. message can be empty (backend generates responses)
            4. 스타일 선택 시 수량 묻지 않음 (자동 1개)
            """;
}
