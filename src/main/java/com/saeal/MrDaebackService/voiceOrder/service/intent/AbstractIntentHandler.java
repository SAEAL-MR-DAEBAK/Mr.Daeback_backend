package com.saeal.MrDaebackService.voiceOrder.service.intent;

import com.saeal.MrDaebackService.voiceOrder.dto.LlmResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import com.saeal.MrDaebackService.voiceOrder.service.CartManager;
import com.saeal.MrDaebackService.voiceOrder.service.MenuMatcher;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intent 핸들러 공통 기능 추상 클래스
 */
@RequiredArgsConstructor
public abstract class AbstractIntentHandler implements IntentHandler {

    protected final MenuMatcher menuMatcher;
    protected final CartManager cartManager;

    /**
     * 주소 목록 포맷팅
     */
    protected String formatAddressList(List<String> addresses) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addresses.size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, addresses.get(i)));
        }
        return sb.toString().trim();
    }

    /**
     * 주문 요약 생성
     */
    protected String buildOrderSummary(List<OrderItemDto> orderItems, String address) {
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (OrderItemDto item : orderItems) {
            sb.append(String.format("• %s", item.getDinnerName()));

            if (item.getServingStyleName() != null) {
                sb.append(" (").append(item.getServingStyleName()).append(")");
            }

            if (item.getItemIndex() > 0) {
                sb.append(" #").append(item.getItemIndex());
            }

            if (item.getExcludedItems() != null && !item.getExcludedItems().isEmpty()) {
                sb.append(" [").append(String.join(", ", item.getExcludedItems())).append(" 제외]");
            }

            sb.append(String.format(" = %,d원\n", item.getTotalPrice()));
            total += item.getTotalPrice();
        }
        sb.append(String.format("\n총 금액: %,d원", total));
        if (address != null && !address.isEmpty()) {
            sb.append(String.format("\n배달 주소: %s", address));
        }
        return sb.toString();
    }

    /**
     * 수량 질문 생성
     */
    protected String buildQuantityQuestion(String dinnerName) {
        if (dinnerName != null && dinnerName.toLowerCase().contains("champagne")) {
            return "1개가 2인분이에요. 몇 개로 드릴까요?";
        }
        return "몇 개로 드릴까요?";
    }

    /**
     * 사용자 메시지에서 아이템 번호 추출
     */
    protected Integer extractItemIndexFromMessage(String message) {
        if (message == null) return null;

        Pattern numPattern = Pattern.compile("(\\d+)번");
        Matcher matcher = numPattern.matcher(message);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (message.contains("첫번째") || message.contains("첫 번째") || message.contains("첫째")) return 1;
        if (message.contains("두번째") || message.contains("두 번째") || message.contains("둘째")) return 2;
        if (message.contains("세번째") || message.contains("세 번째") || message.contains("셋째")) return 3;

        return null;
    }

    /**
     * LLM 엔티티에서 값 안전하게 추출
     */
    protected LlmResponseDto.ExtractedEntities getEntities(IntentContext context) {
        return context.getLlmResponse() != null ? context.getLlmResponse().getEntities() : null;
    }

    /**
     * LLM 메시지 추출 (fallback 포함)
     */
    protected String getLlmMessage(IntentContext context, String fallback) {
        if (context.getLlmResponse() != null && context.getLlmResponse().getMessage() != null) {
            return context.getLlmResponse().getMessage();
        }
        return fallback;
    }
}
