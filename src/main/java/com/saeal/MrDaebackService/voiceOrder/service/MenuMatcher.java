package com.saeal.MrDaebackService.voiceOrder.service;

import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.dinner.service.DinnerService;
import com.saeal.MrDaebackService.servingStyle.dto.response.ServingStyleResponseDto;
import com.saeal.MrDaebackService.servingStyle.service.ServingStyleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 디너/스타일 이름 매칭 서비스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuMatcher {

    private final DinnerService dinnerService;
    private final ServingStyleService servingStyleService;

    // 캐시
    private List<DinnerResponseDto> cachedDinners;
    private List<ServingStyleResponseDto> cachedStyles;

    // 한글-영어 디너 이름 매핑
    private static final Map<String, String> KOREAN_DINNER_NAMES = Map.of(
            "Valentine Dinner", "발렌타인 디너",
            "French Dinner", "프렌치 디너",
            "English Dinner", "잉글리시 디너",
            "Champagne Feast dinner", "샴페인 축제 디너"
    );

    // 한글-영어 스타일 이름 매핑
    private static final Map<String, String> KOREAN_STYLE_NAMES = Map.of(
            "Simple Style", "심플",
            "Grand Style", "그랜드",
            "Deluxe Style", "디럭스"
    );

    // ★ 메뉴 아이템 한글-영어 매핑 (양방향 검색용)
    private static final Map<String, List<String>> MENU_ITEM_KEYWORDS = Map.ofEntries(
            // 고기류
            Map.entry("steak", List.of("스테이크", "steak", "스테익")),
            Map.entry("beef", List.of("소고기", "beef", "비프")),
            Map.entry("pork", List.of("돼지고기", "pork", "포크")),
            Map.entry("chicken", List.of("치킨", "chicken", "닭고기", "닭")),
            Map.entry("lamb", List.of("양고기", "lamb", "램")),
            // 해산물
            Map.entry("lobster", List.of("랍스터", "lobster", "바닷가재")),
            Map.entry("shrimp", List.of("새우", "shrimp", "쉬림프")),
            Map.entry("salmon", List.of("연어", "salmon", "사몬")),
            Map.entry("fish", List.of("생선", "fish", "피쉬")),
            // 음료
            Map.entry("wine", List.of("와인", "wine", "포도주")),
            Map.entry("champagne", List.of("샴페인", "champagne", "샴페인 와인")),
            Map.entry("juice", List.of("주스", "juice", "쥬스")),
            // 채소/사이드
            Map.entry("salad", List.of("샐러드", "salad", "셀러드")),
            Map.entry("soup", List.of("수프", "soup", "스프")),
            Map.entry("bread", List.of("빵", "bread", "브레드")),
            Map.entry("pasta", List.of("파스타", "pasta")),
            Map.entry("rice", List.of("라이스", "rice", "밥")),
            Map.entry("potato", List.of("감자", "potato", "포테이토")),
            Map.entry("vegetable", List.of("채소", "vegetable", "야채")),
            // 디저트
            Map.entry("dessert", List.of("디저트", "dessert", "후식")),
            Map.entry("cake", List.of("케이크", "cake", "케익")),
            Map.entry("icecream", List.of("아이스크림", "ice cream", "icecream"))
    );

    /**
     * 캐시 로드
     */
    public void loadCache() {
        if (cachedDinners == null) {
            cachedDinners = dinnerService.getAllDinners();
        }
        if (cachedStyles == null) {
            cachedStyles = servingStyleService.getAllServingStyles();
        }
    }

    /**
     * 메뉴 이름으로 Dinner 찾기 (부분 매칭 지원)
     */
    public Optional<DinnerResponseDto> findDinnerByName(String menuName) {
        loadCache();
        if (menuName == null) return Optional.empty();

        String normalizedInput = menuName.trim().toLowerCase()
                .replace(" ", "")
                .replace("피스트", "feast")
                .replace("축제", "feast");

        // 1. 정확히 일치하는 경우
        Optional<DinnerResponseDto> exactMatch = cachedDinners.stream()
                .filter(d -> d.isActive() && d.getDinnerName().equalsIgnoreCase(menuName.trim()))
                .findFirst();
        
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        // 2. 부분 매칭 (대소문자 무시, 공백 무시)
        return cachedDinners.stream()
                .filter(d -> {
                    if (!d.isActive()) return false;
                    String normalizedDinner = d.getDinnerName().toLowerCase().replace(" ", "");
                    return normalizedDinner.contains(normalizedInput) || 
                           normalizedInput.contains(normalizedDinner) ||
                           isMatchingMenu(d.getDinnerName(), menuName);
                })
                .findFirst();
    }

    /**
     * 스타일 이름으로 ServingStyle 찾기
     */
    public Optional<ServingStyleResponseDto> findStyleByName(String styleName) {
        loadCache();
        if (styleName == null) return Optional.empty();

        return cachedStyles.stream()
                .filter(s -> s.isActive() && s.getStyleName().equalsIgnoreCase(styleName.trim()))
                .findFirst();
    }

    /**
     * 활성 디너 목록 (프롬프트용) - 한글 이름 포함
     */
    public String getMenuListForPrompt() {
        loadCache();
        return cachedDinners.stream()
                .filter(DinnerResponseDto::isActive)
                .map(d -> {
                    String koreanName = KOREAN_DINNER_NAMES.getOrDefault(d.getDinnerName(), d.getDinnerName());
                    return String.format("- %s (영문: %s) - %,d원: %s",
                            koreanName,
                            d.getDinnerName(),
                            d.getBasePrice().intValue(),
                            d.getDescription() != null ? d.getDescription() : "");
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 활성 스타일 목록 (프롬프트용) - 한글 이름 포함
     */
    public String getStyleListForPrompt() {
        loadCache();
        return cachedStyles.stream()
                .filter(ServingStyleResponseDto::isActive)
                .map(s -> {
                    String koreanName = KOREAN_STYLE_NAMES.getOrDefault(s.getStyleName(), s.getStyleName());
                    return String.format("- %s (영문: %s) - +%,d원",
                            koreanName,
                            s.getStyleName(),
                            s.getExtraPrice().intValue());
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 영문 디너 이름을 한글로 변환
     */
    public String toKoreanDinnerName(String englishName) {
        return KOREAN_DINNER_NAMES.getOrDefault(englishName, englishName);
    }

    /**
     * 영문 스타일 이름을 한글로 변환
     */
    public String toKoreanStyleName(String englishName) {
        return KOREAN_STYLE_NAMES.getOrDefault(englishName, englishName);
    }

    /**
     * 두 메뉴 이름이 매칭되는지 확인 (대소문자 무시, 부분 매칭)
     */
    public boolean isMatchingMenu(String dinnerName, String inputName) {
        if (dinnerName == null || inputName == null) return false;
        String d = dinnerName.toLowerCase().trim();
        String i = inputName.toLowerCase().trim();
        return d.equals(i) || d.contains(i) || i.contains(d);
    }

    /**
     * 샴페인 축제 디너인지 확인
     */
    public boolean isChampagneDinner(String dinnerName) {
        if (dinnerName == null) return false;
        String lower = dinnerName.toLowerCase();
        return lower.contains("champagne") || lower.contains("샴페인") || lower.contains("축제");
    }

    /**
     * 해당 디너에 스타일이 적용 가능한지 확인
     * - 샴페인 축제 디너는 Simple 스타일 불가
     */
    public boolean isStyleAvailableForDinner(String dinnerName, String styleName) {
        if (dinnerName == null || styleName == null) return true;

        // 샴페인 축제 디너는 Simple 스타일 불가
        if (isChampagneDinner(dinnerName)) {
            String lowerStyle = styleName.toLowerCase();
            if (lowerStyle.contains("simple") || lowerStyle.contains("심플")) {
                return false;
            }
        }
        return true;
    }

    /**
     * 해당 디너에 사용 가능한 스타일 목록 (한글)
     */
    public String getAvailableStylesForDinner(String dinnerName) {
        if (isChampagneDinner(dinnerName)) {
            return "그랜드, 디럭스";
        }
        return "심플, 그랜드, 디럭스";
    }

    /**
     * ★ 메뉴 아이템 이름이 매칭되는지 확인 (한글/영문 양방향)
     * 사용자 입력(한글)과 DB의 메뉴 아이템 이름(영문 가능)을 매칭
     */
    public boolean isMatchingMenuItem(String dbMenuItemName, String userInput) {
        if (dbMenuItemName == null || userInput == null) return false;

        String normalizedDb = dbMenuItemName.toLowerCase().trim();
        String normalizedInput = userInput.toLowerCase().trim();

        // 1. 직접 매칭 (부분 포함 검사)
        if (normalizedDb.contains(normalizedInput) || normalizedInput.contains(normalizedDb)) {
            return true;
        }

        // 2. 키워드 매핑을 통한 매칭
        for (Map.Entry<String, List<String>> entry : MENU_ITEM_KEYWORDS.entrySet()) {
            List<String> keywords = entry.getValue();

            // DB 이름이 이 카테고리에 해당하는지 확인
            boolean dbMatchesCategory = keywords.stream()
                    .anyMatch(kw -> normalizedDb.contains(kw.toLowerCase()));

            // 사용자 입력이 이 카테고리에 해당하는지 확인
            boolean inputMatchesCategory = keywords.stream()
                    .anyMatch(kw -> normalizedInput.contains(kw.toLowerCase()));

            // 둘 다 같은 카테고리에 해당하면 매칭
            if (dbMatchesCategory && inputMatchesCategory) {
                return true;
            }
        }

        return false;
    }
}
