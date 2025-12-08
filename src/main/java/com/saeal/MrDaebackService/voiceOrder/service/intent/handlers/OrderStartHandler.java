package com.saeal.MrDaebackService.voiceOrder.service.intent.handlers;

import com.saeal.MrDaebackService.voiceOrder.dto.LlmResponseDto;
import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;
import com.saeal.MrDaebackService.voiceOrder.service.CartManager;
import com.saeal.MrDaebackService.voiceOrder.service.MenuMatcher;
import com.saeal.MrDaebackService.voiceOrder.service.intent.AbstractIntentHandler;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentContext;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ORDER_START Intent 처리 (간소화된 플로우)
 * - 첫 인사 또는 기념일/배달시간 언급 시 자동 추출
 * - "모레가 친구 생일이에요" → occasionType: 생일, deliveryDate: 모레
 */
@Component
public class OrderStartHandler extends AbstractIntentHandler {

    public OrderStartHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.ORDER_START;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        String userMessage = context.getUserMessage();

        // 기념일 추출
        String occasionType = null;
        if (entities != null && entities.getOccasionType() != null) {
            occasionType = entities.getOccasionType();
        } else {
            occasionType = extractOccasionFromMessage(userMessage);
        }

        // 배달 시간 추출
        LocalDateTime deliveryTime = null;
        if (entities != null) {
            deliveryTime = parseDeliveryTime(entities.getDeliveryDate(), entities.getDeliveryTime());
        }
        if (deliveryTime == null) {
            deliveryTime = parseDeliveryTimeFromMessage(userMessage);
        }

        // 응답 메시지 생성
        StringBuilder message = new StringBuilder();

        if (occasionType != null) {
            message.append(getOccasionGreeting(occasionType));
        } else {
            message.append("안녕하세요! Mr.Daeback입니다. 🍽️\n\n");
        }

        if (deliveryTime != null) {
            message.append(formatDeliveryTime(deliveryTime)).append(" 배달로 준비해드릴게요!\n\n");
        }

        message.append("어떤 디너를 주문하시겠어요?\n");
        message.append("예) 그랜드 샴페인 축제 디너 2개, 디럭스 발렌타인 디너 1개");

        return IntentResult.builder()
                .message(message.toString())
                .nextState(OrderFlowState.ORDERING)
                .occasionType(occasionType)
                .requestedDeliveryTime(deliveryTime)
                .build();
    }

    /**
     * 기념일에 따른 인사 메시지
     */
    private String getOccasionGreeting(String occasionType) {
        String lower = occasionType.toLowerCase();

        if (lower.contains("생일")) {
            return "생일 축하드려요! 🎂\n\n";
        } else if (lower.contains("프로포즈") || lower.contains("청혼")) {
            return "프로포즈 준비시군요! 💍 응원할게요!\n\n";
        } else if (lower.contains("결혼") || lower.contains("기념일")) {
            return "결혼기념일 축하드려요! 💑\n\n";
        } else if (lower.contains("승진") || lower.contains("축하")) {
            return "축하드려요! 🎊\n\n";
        } else {
            return "특별한 날을 위해 준비해드릴게요! ✨\n\n";
        }
    }

    /**
     * 메시지에서 기념일 추출
     */
    private String extractOccasionFromMessage(String message) {
        if (message == null) return null;

        String[] occasions = {"생일", "결혼기념일", "기념일", "프로포즈", "청혼", "승진", "취업", "졸업", "합격"};
        for (String occasion : occasions) {
            if (message.contains(occasion)) {
                return occasion;
            }
        }
        return null;
    }

    /**
     * 배달 시간 파싱 (날짜 + 시간)
     */
    private LocalDateTime parseDeliveryTime(String dateStr, String timeStr) {
        if (dateStr == null && timeStr == null) return null;

        LocalDate date = parseDate(dateStr);
        LocalTime time = parseTime(timeStr);

        if (date == null && time == null) return null;
        if (date == null) date = LocalDate.now().plusDays(1);
        if (time == null) time = LocalTime.of(18, 0);

        return LocalDateTime.of(date, time);
    }

    /**
     * 메시지에서 직접 배달 시간 파싱
     */
    private LocalDateTime parseDeliveryTimeFromMessage(String message) {
        if (message == null) return null;

        LocalDate date = parseDate(message);
        LocalTime time = parseTime(message);

        if (date == null && time == null) return null;
        if (date == null) date = LocalDate.now().plusDays(1);
        if (time == null) time = LocalTime.of(18, 0);

        return LocalDateTime.of(date, time);
    }

    /**
     * 날짜 파싱
     */
    private LocalDate parseDate(String text) {
        if (text == null) return null;
        LocalDate today = LocalDate.now();

        if (text.contains("오늘")) return today;
        if (text.contains("내일")) return today.plusDays(1);
        if (text.contains("모레")) return today.plusDays(2);
        if (text.contains("글피")) return today.plusDays(3);

        // 요일 파싱
        String[] days = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};
        for (int i = 0; i < days.length; i++) {
            if (text.contains(days[i])) {
                int targetDayOfWeek = i + 1;
                int currentDayOfWeek = today.getDayOfWeek().getValue();
                int daysToAdd = (targetDayOfWeek - currentDayOfWeek + 7) % 7;
                if (daysToAdd == 0) daysToAdd = 7;
                return today.plusDays(daysToAdd);
            }
        }

        // MM월 DD일 패턴
        Pattern datePattern = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");
        Matcher matcher = datePattern.matcher(text);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            int year = today.getYear();
            if (month < today.getMonthValue() || (month == today.getMonthValue() && day < today.getDayOfMonth())) {
                year++;
            }
            return LocalDate.of(year, month, day);
        }

        return null;
    }

    /**
     * 시간 파싱
     */
    private LocalTime parseTime(String text) {
        if (text == null) return null;

        Pattern timePattern = Pattern.compile("(오전|오후|저녁|아침|점심)?\\s*(\\d{1,2})시\\s*(\\d{1,2})?분?");
        Matcher matcher = timePattern.matcher(text);
        if (matcher.find()) {
            String period = matcher.group(1);
            int hour = Integer.parseInt(matcher.group(2));
            int minute = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

            if (period != null) {
                if ((period.equals("오후") || period.equals("저녁")) && hour < 12) {
                    hour += 12;
                } else if (period.equals("오전") && hour == 12) {
                    hour = 0;
                }
            } else if (hour >= 1 && hour <= 9) {
                hour += 12;
            }

            return LocalTime.of(hour, minute);
        }

        return null;
    }

    /**
     * 배달 시간 포맷팅
     */
    private String formatDeliveryTime(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
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

        int hour = time.getHour();
        int minute = time.getMinute();
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
}
