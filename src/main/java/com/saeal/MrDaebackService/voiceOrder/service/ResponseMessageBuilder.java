package com.saeal.MrDaebackService.voiceOrder.service;

import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 응답 메시지 생성 담당
 * - 상태 요약, 주문 요약 등 공통 메시지 포맷팅
 */
@Component
public class ResponseMessageBuilder {

    /**
     * 상태 요약 생성 (📍 현재 단계, 🛒 현재 주문, ⏰ 배달 시간)
     */
    public String buildStatusSummary(OrderFlowState state, List<OrderItemDto> orderItems,
                                     LocalDateTime deliveryTime, String occasionType) {
        String stateKorean = getStateKorean(state);
        String orderSummary = buildOrderSummaryShort(orderItems);

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append(String.format("📍 현재 단계: %s\n", stateKorean));
        sb.append(String.format("🛒 현재 주문: %s", orderSummary));

        // 배달 시간 표시
        if (deliveryTime != null) {
            sb.append(String.format("\n⏰ 배달 시간: %s", formatDeliveryTime(deliveryTime)));
        }

        // 기념일 종류 표시
        if (occasionType != null && !occasionType.isEmpty()) {
            sb.append(String.format("\n🎉 기념일: %s", occasionType));
        }

        return sb.toString();
    }

    /**
     * 상태 요약 생성 (기본 - 배달 시간 없음)
     */
    public String buildStatusSummary(OrderFlowState state, List<OrderItemDto> orderItems) {
        return buildStatusSummary(state, orderItems, null, null);
    }

    /**
     * 메시지에 상태 요약 추가 (배달 시간 포함)
     */
    public String appendStatusSummary(String message, OrderFlowState state, List<OrderItemDto> orderItems,
                                      LocalDateTime deliveryTime, String occasionType) {
        return message + "\n\n" + buildStatusSummary(state, orderItems, deliveryTime, occasionType);
    }

    /**
     * 메시지에 상태 요약 추가 (기본 - 배달 시간 없음)
     */
    public String appendStatusSummary(String message, OrderFlowState state, List<OrderItemDto> orderItems) {
        return appendStatusSummary(message, state, orderItems, null, null);
    }

    /**
     * 배달 시간 포맷팅
     */
    private String formatDeliveryTime(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalDate today = LocalDate.now();

        String dateStr;
        if (date.equals(today)) {
            dateStr = "오늘";
        } else if (date.equals(today.plusDays(1))) {
            dateStr = "내일";
        } else if (date.equals(today.plusDays(2))) {
            dateStr = "모레";
        } else {
            dateStr = date.format(DateTimeFormatter.ofPattern("M월 d일"));
        }

        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        String period = hour < 12 ? "오전" : "오후";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);

        String timeStr;
        if (minute == 0) {
            timeStr = String.format("%s %d시", period, displayHour);
        } else {
            timeStr = String.format("%s %d시 %d분", period, displayHour, minute);
        }

        return dateStr + " " + timeStr;
    }

    /**
     * 주문 상태 한글 변환
     */
    private String getStateKorean(OrderFlowState state) {
        return switch (state) {
            case IDLE -> "대기";
            case SELECTING_ADDRESS -> "주소 선택 중";
            case ASKING_OCCASION -> "기념일 확인 중";
            case ASKING_DELIVERY_TIME -> "배달 시간 확인 중";
            case SELECTING_MENU -> "메뉴 선택 중";
            case SELECTING_STYLE -> "스타일 선택 중";
            case SELECTING_QUANTITY -> "수량 선택 중";
            case ASKING_MORE_DINNER -> "추가 디너 확인 중";
            case CUSTOMIZING_MENU -> "구성요소 변경 중";
            case SELECTING_ADDITIONAL_MENU -> "추가 메뉴 선택 중";
            case ENTERING_MEMO -> "메모 입력 중";
            case CONFIRMING -> "주문 확인 중";
            case CHECKOUT_READY -> "결제 준비 완료";
        };
    }

    /**
     * 주문 요약 (짧은 버전)
     * 사용자 요청: Champagne Feast dinner (Deluxe Style) #1 [스테이크 : 1개, 샐러드 : 1개]
     */
    private String buildOrderSummaryShort(List<OrderItemDto> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return "없음";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItemDto item = orderItems.get(i);
            if (i > 0) sb.append(", ");

            // 디너 이름
            sb.append(item.getDinnerName());

            // 스타일 이름
            if (item.getServingStyleName() != null) {
                sb.append(" (").append(item.getServingStyleName()).append(")");
            }

            // 아이템 인덱스 (#1, #2 등) - 항상 표시 (1부터 시작)
            // itemIndex가 0이면 첫 번째 아이템으로 간주하여 #1 표시
            int displayIndex = item.getItemIndex() > 0 ? item.getItemIndex() : (i + 1);
            sb.append(" #").append(displayIndex);

            // 구성요소 표시 (OrderItemDto의 getComponentsDisplay 사용)
            String componentsDisplay = item.getComponentsDisplay();
            if (!componentsDisplay.isEmpty()) {
                sb.append(componentsDisplay);
            }
        }

        return sb.toString();
    }

    /**
     * 주문 요약 (전체 버전 - 가격 포함)
     */
    public String buildOrderSummaryFull(List<OrderItemDto> orderItems, String address) {
        StringBuilder sb = new StringBuilder();
        int total = 0;

        for (int i = 0; i < orderItems.size(); i++) {
            OrderItemDto item = orderItems.get(i);
            sb.append(String.format("• %s", item.getDinnerName()));

            if (item.getServingStyleName() != null) {
                sb.append(" (").append(item.getServingStyleName()).append(")");
            }

            // 아이템 인덱스 (#1, #2 등) - 항상 표시
            int displayIndex = item.getItemIndex() > 0 ? item.getItemIndex() : (i + 1);
            sb.append(" #").append(displayIndex);

            // 구성요소 표시
            String componentsDisplay = item.getComponentsDisplay();
            if (!componentsDisplay.isEmpty()) {
                sb.append(componentsDisplay);
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
     * 주소 목록 포맷팅
     */
    public String formatAddressList(List<String> addresses) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addresses.size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, addresses.get(i)));
        }
        return sb.toString().trim();
    }
}
