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
 * SET_OCCASION, SET_DELIVERY_TIME, ASK_RECOMMENDATION Intent 처리
 * - 기념일 종류 설정
 * - 배달 시간 설정
 * - 디너 추천 요청 (기념일 질문으로 연결)
 */
@Component
public class OccasionHandler extends AbstractIntentHandler {

    public OccasionHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.SET_OCCASION
                || intent == UserIntent.SET_DELIVERY_TIME
                || intent == UserIntent.ASK_RECOMMENDATION;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        UserIntent intent = parseIntent(context);

        if (intent == UserIntent.ASK_RECOMMENDATION) {
            return handleAskRecommendation(context);
        } else if (intent == UserIntent.SET_OCCASION) {
            return handleSetOccasion(context);
        } else if (intent == UserIntent.SET_DELIVERY_TIME) {
            return handleSetDeliveryTime(context);
        }

        return IntentResult.of("무엇을 도와드릴까요?", OrderFlowState.SELECTING_MENU);
    }

    /**
     * 디너 추천 요청 처리 → 기념일 질문
     */
    private IntentResult handleAskRecommendation(IntentContext context) {
        String message = "특별한 날을 위한 디너를 준비해드릴게요! 🎉\n\n" +
                "어떤 기념일이신가요?\n" +
                "예) 생일, 결혼기념일, 프로포즈, 승진 축하, 특별한 날 등";

        return IntentResult.of(message, OrderFlowState.ASKING_OCCASION);
    }

    /**
     * 기념일 종류 설정 → 배달 시간 질문
     */
    private IntentResult handleSetOccasion(IntentContext context) {
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        String occasionType = null;

        if (entities != null && entities.getOccasionType() != null) {
            occasionType = entities.getOccasionType();
        } else {
            // 메시지에서 기념일 추출 시도
            occasionType = extractOccasionFromMessage(context.getUserMessage());
        }

        if (occasionType == null || occasionType.isEmpty()) {
            return IntentResult.of(
                    "어떤 기념일이신지 알려주세요! (예: 생일, 결혼기념일, 프로포즈 등)",
                    OrderFlowState.ASKING_OCCASION
            );
        }

        String recommendation = getRecommendationByOccasion(occasionType);
        String message = occasionType + " 축하드려요! 🎊\n\n" +
                recommendation + "\n\n" +
                "배달 받으실 날짜와 시간을 알려주세요!\n" +
                "예) 내일 저녁 7시, 12월 25일 오후 6시";

        return IntentResult.builder()
                .message(message)
                .nextState(OrderFlowState.ASKING_DELIVERY_TIME)
                .occasionType(occasionType)
                .build();
    }

    /**
     * 배달 시간 설정 → 메뉴 선택으로 이동
     */
    private IntentResult handleSetDeliveryTime(IntentContext context) {
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        LocalDateTime deliveryTime = null;

        if (entities != null) {
            deliveryTime = parseDeliveryTime(entities.getDeliveryDate(), entities.getDeliveryTime());
        }

        if (deliveryTime == null) {
            // 메시지에서 직접 파싱 시도
            deliveryTime = parseDeliveryTimeFromMessage(context.getUserMessage());
        }

        if (deliveryTime == null) {
            return IntentResult.of(
                    "배달 시간을 알려주세요! 예) 내일 저녁 7시, 토요일 오후 6시",
                    OrderFlowState.ASKING_DELIVERY_TIME
            );
        }

        String formattedTime = formatDeliveryTime(deliveryTime);
        String message = formattedTime + "에 배달해드릴게요! ⏰\n\n" +
                "어떤 디너를 주문하시겠어요?\n" +
                "• 디너 추천이 필요하시면 '디너 추천해줘'라고 말씀해주세요!\n" +
                "• 또는 원하시는 메뉴를 바로 말씀해주세요.\n\n" +
                "메뉴: 발렌타인 디너, 프렌치 디너, 잉글리시 디너, 샴페인 축제 디너";

        return IntentResult.builder()
                .message(message)
                .nextState(OrderFlowState.SELECTING_MENU)
                .requestedDeliveryTime(deliveryTime)
                .build();
    }

    /**
     * 기념일 종류에 따른 추천 메시지
     */
    private String getRecommendationByOccasion(String occasionType) {
        String lower = occasionType.toLowerCase();

        if (lower.contains("생일")) {
            return "생일에는 '샴페인 축제 디너'를 추천드려요! 🎂\n샴페인과 함께 특별한 축하 분위기를 만들어보세요.";
        } else if (lower.contains("프로포즈") || lower.contains("청혼")) {
            return "프로포즈에는 '발렌타인 디너'를 추천드려요! 💍\n로맨틱한 분위기로 소중한 순간을 만들어보세요.";
        } else if (lower.contains("결혼") || lower.contains("기념일")) {
            return "결혼기념일에는 '프렌치 디너'를 추천드려요! 💑\n고급스러운 정찬으로 특별한 하루를 보내세요.";
        } else if (lower.contains("승진") || lower.contains("축하")) {
            return "축하드려요! '샴페인 축제 디너'로 화려하게 축하해보세요! 🎊";
        } else {
            return "특별한 날에는 '프렌치 디너' 또는 '샴페인 축제 디너'를 추천드려요! ✨";
        }
    }

    /**
     * 메시지에서 기념일 추출
     */
    private String extractOccasionFromMessage(String message) {
        if (message == null) return null;

        String[] occasions = {"생일", "결혼기념일", "기념일", "프로포즈", "청혼", "승진", "취업", "졸업", "합격", "특별한 날"};
        for (String occasion : occasions) {
            if (message.contains(occasion)) {
                return occasion;
            }
        }
        return message; // 메시지 전체를 기념일로 사용
    }

    /**
     * 배달 시간 파싱 (날짜 + 시간)
     */
    private LocalDateTime parseDeliveryTime(String dateStr, String timeStr) {
        if (dateStr == null && timeStr == null) return null;

        LocalDate date = parseDate(dateStr);
        LocalTime time = parseTime(timeStr);

        if (date == null) date = LocalDate.now().plusDays(1); // 기본: 내일
        if (time == null) time = LocalTime.of(18, 0); // 기본: 오후 6시

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
                int targetDayOfWeek = i + 1; // 1=Monday
                int currentDayOfWeek = today.getDayOfWeek().getValue();
                int daysToAdd = (targetDayOfWeek - currentDayOfWeek + 7) % 7;
                if (daysToAdd == 0) daysToAdd = 7; // 다음 주 같은 요일
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
                year++; // 다음 해
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

        // 오전/오후 N시 패턴
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
            } else if (hour < 12 && hour >= 1 && hour <= 9) {
                // 숫자만 있고 1~9시면 오후로 추정
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

        String timeStr;
        int hour = time.getHour();
        int minute = time.getMinute();
        String period = hour < 12 ? "오전" : "오후";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);

        if (minute == 0) {
            timeStr = String.format("%s %d시", period, displayHour);
        } else {
            timeStr = String.format("%s %d시 %d분", period, displayHour, minute);
        }

        return dateStr + " " + timeStr;
    }

    private UserIntent parseIntent(IntentContext context) {
        if (context.getLlmResponse() == null || context.getLlmResponse().getIntent() == null) {
            return UserIntent.UNKNOWN;
        }
        try {
            return UserIntent.valueOf(context.getLlmResponse().getIntent().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserIntent.UNKNOWN;
        }
    }
}
