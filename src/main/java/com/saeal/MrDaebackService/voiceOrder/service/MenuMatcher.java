package com.saeal.MrDaebackService.voiceOrder.service;

import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.dinner.service.DinnerService;
import com.saeal.MrDaebackService.menuItems.dto.MenuItemResponseDto;
import com.saeal.MrDaebackService.menuItems.service.MenuItemsService;
import com.saeal.MrDaebackService.servingStyle.dto.response.ServingStyleResponseDto;
import com.saeal.MrDaebackService.servingStyle.service.ServingStyleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 메뉴/스타일 이름 매칭 서비스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuMatcher {

    private final DinnerService dinnerService;
    private final ServingStyleService servingStyleService;
    private final MenuItemsService menuItemsService;

    // 캐시
    private List<DinnerResponseDto> cachedDinners;
    private List<ServingStyleResponseDto> cachedStyles;
    private List<MenuItemResponseDto> cachedMenuItems;

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
        if (cachedMenuItems == null) {
            cachedMenuItems = menuItemsService.getAllMenuItems();
        }
    }

    /**
     * 한글 디너 이름을 영어로 변환
     */
    public String convertKoreanToEnglish(String koreanName) {
        if (koreanName == null) return null;
        
        String normalized = koreanName.trim();
        
        // 한글 디너 이름 매핑
        if (normalized.contains("발렌타인") || normalized.contains("발렌타인 디너")) {
            return "Valentine Dinner";
        }
        if (normalized.contains("프렌치") || normalized.contains("프렌치 디너")) {
            return "French Dinner";
        }
        if (normalized.contains("잉글리시") || normalized.contains("잉글리시 디너") || normalized.contains("영어")) {
            return "English Dinner";
        }
        // 샴페인 관련: 공백 유무와 관계없이 매칭
        if (normalized.contains("샴페인")) {
            // "샴페인축제디너", "샴페인 축제 디너", "샴페인피스트" 등 모두 처리
            if (normalized.contains("축제") || normalized.contains("피스트") || normalized.contains("feast")) {
                return "Champagne Feast";
            }
            // "샴페인"만 있어도 Champagne Feast로 매핑
            return "Champagne Feast";
        }
        
        // 매핑되지 않으면 원본 반환
        return normalized;
    }

    /**
     * 영어 디너 이름을 한글로 변환 (역변환)
     */
    private String convertEnglishToKorean(String englishName) {
        if (englishName == null) return null;
        
        String normalized = englishName.trim();
        
        // 영어 디너 이름 매핑
        if (normalized.equalsIgnoreCase("Valentine Dinner")) {
            return "발렌타인 디너";
        }
        if (normalized.equalsIgnoreCase("French Dinner")) {
            return "프렌치 디너";
        }
        if (normalized.equalsIgnoreCase("English Dinner")) {
            return "잉글리시 디너";
        }
        if (normalized.equalsIgnoreCase("Champagne Feast")) {
            return "샴페인 축제 디너";
        }
        
        return normalized;
    }

    /**
     * 메뉴 이름으로 Dinner 찾기 (한글/영어 모두 지원)
     */
    public Optional<DinnerResponseDto> findDinnerByName(String menuName) {
        loadCache();
        if (menuName == null) {
            log.warn("[MenuMatcher] menuName이 null입니다!");
            return Optional.empty();
        }

        String normalizedName = menuName.trim();
        log.info("[MenuMatcher] ========== 메뉴 찾기 시작 ==========");
        log.info("[MenuMatcher] 찾는 메뉴명: '{}'", normalizedName);
        
        // 디버깅: 실제 DB에 있는 메뉴명 목록 출력
        log.info("[MenuMatcher] DB에 저장된 활성 메뉴 목록 (총 {}개):", 
                cachedDinners.stream().filter(DinnerResponseDto::isActive).count());
        cachedDinners.stream()
                .filter(DinnerResponseDto::isActive)
                .forEach(d -> log.info("[MenuMatcher]   - '{}' (ID: {})", d.getDinnerName(), d.getId()));
        
        // 한글 이름을 영어로 변환
        String englishName = convertKoreanToEnglish(normalizedName);
        log.info("[MenuMatcher] 영어로 변환: '{}'", englishName);
        
        // 1. 영어 이름으로 정확히 일치하는 것 찾기
        Optional<DinnerResponseDto> result = cachedDinners.stream()
                .filter(d -> d.isActive() && d.getDinnerName().equalsIgnoreCase(englishName))
                .findFirst();
        
        if (result.isPresent()) {
            log.info("[MenuMatcher] 정확 일치 발견: {}", result.get().getDinnerName());
            return result;
        }
        
        // 2. 원본 이름으로 정확히 일치하는 것 찾기
        result = cachedDinners.stream()
                .filter(d -> d.isActive() && d.getDinnerName().equalsIgnoreCase(normalizedName))
                .findFirst();
        
        if (result.isPresent()) {
            log.info("[MenuMatcher] 원본 이름 일치 발견: {}", result.get().getDinnerName());
            return result;
        }
        
        // 3. 영어 이름을 한글로 변환해서 찾기 (역변환)
        String koreanName = convertEnglishToKorean(englishName);
        if (!koreanName.equals(englishName)) {
            result = cachedDinners.stream()
                    .filter(d -> d.isActive() && d.getDinnerName().equalsIgnoreCase(koreanName))
                    .findFirst();
            
            if (result.isPresent()) {
                log.info("[MenuMatcher] 한글 변환 후 일치 발견: {}", result.get().getDinnerName());
                return result;
            }
        }
        
        // 4. 부분 매칭 시도 (영어 이름 기준)
        String lowerEnglishName = englishName.toLowerCase();
        result = cachedDinners.stream()
                .filter(d -> {
                    String dbName = d.getDinnerName().toLowerCase();
                    boolean matches = dbName.contains(lowerEnglishName) || lowerEnglishName.contains(dbName);
                    if (matches) {
                        log.debug("[MenuMatcher] 부분 매칭: '{}' <-> '{}'", lowerEnglishName, dbName);
                    }
                    return d.isActive() && matches;
                })
                .findFirst();
        
        if (result.isPresent()) {
            log.info("[MenuMatcher] 부분 매칭 발견: {}", result.get().getDinnerName());
            return result;
        }
        
        // 5. 한글 이름으로 부분 매칭 시도
        if (koreanName != null && !koreanName.equals(englishName)) {
            String lowerKoreanName = koreanName.toLowerCase();
            result = cachedDinners.stream()
                    .filter(d -> {
                        String dbName = d.getDinnerName().toLowerCase();
                        boolean matches = dbName.contains(lowerKoreanName) || lowerKoreanName.contains(dbName);
                        return d.isActive() && matches;
                    })
                    .findFirst();
            
            if (result.isPresent()) {
                log.info("[MenuMatcher] 한글 부분 매칭 발견: {}", result.get().getDinnerName());
                return result;
            }
        }
        
        // 6. 키워드 기반 매칭 (샴페인, 발렌타인 등)
        String lowerInput = normalizedName.toLowerCase();
        if (lowerInput.contains("champagne") || lowerInput.contains("샴페인") || lowerInput.contains("feast")) {
            result = cachedDinners.stream()
                    .filter(d -> {
                        String dbName = d.getDinnerName().toLowerCase();
                        boolean matches = dbName.contains("champagne") || dbName.contains("샴페인") || dbName.contains("feast");
                        if (matches) {
                            log.debug("[MenuMatcher] 키워드 매칭 후보: '{}' (입력: '{}')", dbName, lowerInput);
                        }
                        return d.isActive() && matches;
                    })
                    .findFirst();
            
            if (result.isPresent()) {
                log.info("[MenuMatcher] 키워드 매칭 (샴페인) 발견: '{}' (입력: '{}')", result.get().getDinnerName(), normalizedName);
                return result;
            }
        }
        
        // 7. 모든 활성 메뉴명 출력 (디버깅)
        log.warn("[MenuMatcher] 메뉴를 찾을 수 없음: '{}' (변환된 영어명: '{}')", normalizedName, englishName);
        log.warn("[MenuMatcher] 사용 가능한 메뉴 목록:");
        cachedDinners.stream()
                .filter(DinnerResponseDto::isActive)
                .forEach(d -> log.warn("  - '{}'", d.getDinnerName()));
        
        return Optional.empty();
    }

    /**
     * 한국어 스타일 이름을 영어로 변환
     */
    private String convertKoreanStyleToEnglish(String koreanStyle) {
        if (koreanStyle == null) return null;
        String normalized = koreanStyle.trim().toLowerCase();
        
        // "디럭스", "딜럭스", "딜럭스 스타일" 등 → "Deluxe Style"
        if (normalized.contains("디럭스") || normalized.contains("딜럭스") || normalized.contains("deluxe")) {
            return "Deluxe Style";
        }
        // "그랜드", "그랜드 스타일" 등 → "Grand Style"
        if (normalized.contains("그랜드") || normalized.contains("grand")) {
            return "Grand Style";
        }
        // "심플", "심플 스타일", "심플한" 등 → "Simple Style"
        if (normalized.contains("심플") || normalized.contains("simple")) {
            return "Simple Style";
        }
        
        return koreanStyle; // 변환할 수 없으면 원본 반환
    }

    /**
     * 스타일 이름으로 ServingStyle 찾기 (한국어 지원)
     */
    public Optional<ServingStyleResponseDto> findStyleByName(String styleName) {
        loadCache();
        if (styleName == null) return Optional.empty();

        String normalizedName = styleName.trim();
        log.info("[MenuMatcher] 스타일 찾기 시작: '{}'", normalizedName);
        
        // 1. 정확 일치 (원본 이름)
        Optional<ServingStyleResponseDto> result = cachedStyles.stream()
                .filter(s -> s.isActive() && s.getStyleName().equalsIgnoreCase(normalizedName))
                .findFirst();
        
        if (result.isPresent()) {
            log.info("[MenuMatcher] 스타일 정확 일치 발견: {}", result.get().getStyleName());
            return result;
        }
        
        // 2. 한국어 이름을 영어로 변환 후 일치
        String englishStyleName = convertKoreanStyleToEnglish(normalizedName);
        if (!englishStyleName.equals(normalizedName)) {
            log.info("[MenuMatcher] 한국어 스타일 이름을 영어로 변환: '{}' → '{}'", normalizedName, englishStyleName);
            result = cachedStyles.stream()
                    .filter(s -> s.isActive() && s.getStyleName().equalsIgnoreCase(englishStyleName))
                    .findFirst();
            
            if (result.isPresent()) {
                log.info("[MenuMatcher] 한국어 변환 후 스타일 일치 발견: {}", result.get().getStyleName());
                return result;
            }
        }
        
        // 3. 부분 매칭 (대소문자 무시)
        String lowerName = normalizedName.toLowerCase();
        result = cachedStyles.stream()
                .filter(s -> {
                    String dbName = s.getStyleName().toLowerCase();
                    boolean matches = dbName.contains(lowerName) || lowerName.contains(dbName);
                    return s.isActive() && matches;
                })
                .findFirst();
        
        if (result.isPresent()) {
            log.info("[MenuMatcher] 스타일 부분 매칭 발견: {}", result.get().getStyleName());
            return result;
        }
        
        log.warn("[MenuMatcher] 스타일을 찾을 수 없음: '{}'", normalizedName);
        return Optional.empty();
    }

    /**
     * 영어 디너 이름에 대한 한글 이름 매핑
     */
    private String getKoreanName(String englishName) {
        if (englishName == null) return null;
        
        String normalized = englishName.trim();
        if (normalized.equalsIgnoreCase("Valentine Dinner")) {
            return "발렌타인 디너";
        }
        if (normalized.equalsIgnoreCase("French Dinner")) {
            return "프렌치 디너";
        }
        if (normalized.equalsIgnoreCase("English Dinner")) {
            return "잉글리시 디너";
        }
        if (normalized.equalsIgnoreCase("Champagne Feast")) {
            return "샴페인 축제 디너";
        }
        return normalized;
    }

    /**
     * 활성 메뉴 목록 (프롬프트용 - 한글 이름 포함)
     */
    public String getMenuListForPrompt() {
        loadCache();
        return cachedDinners.stream()
                .filter(DinnerResponseDto::isActive)
                .map(d -> {
                    String koreanName = getKoreanName(d.getDinnerName());
                    if (koreanName != null && !koreanName.equals(d.getDinnerName())) {
                        return String.format("- %s (%s) (%d원): %s",
                                d.getDinnerName(),
                                koreanName,
                                d.getBasePrice().intValue(),
                                d.getDescription() != null ? d.getDescription() : "");
                    }
                    return String.format("- %s (%d원): %s",
                            d.getDinnerName(),
                            d.getBasePrice().intValue(),
                            d.getDescription() != null ? d.getDescription() : "");
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 활성 스타일 목록 (프롬프트용)
     */
    public String getStyleListForPrompt() {
        loadCache();
        return cachedStyles.stream()
                .filter(ServingStyleResponseDto::isActive)
                .map(s -> String.format("- %s (+%d원)",
                        s.getStyleName(),
                        s.getExtraPrice().intValue()))
                .collect(Collectors.joining("\n"));
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
     * 메뉴 아이템 이름으로 찾기 (정확 일치 우선, 부분 매칭 지원)
     */
    public Optional<MenuItemResponseDto> findMenuItemByName(String menuItemName) {
        loadCache();
        if (menuItemName == null) return Optional.empty();
        
        String trimmedName = menuItemName.trim();
        
        // 1. 정확 일치 우선
        Optional<MenuItemResponseDto> exactMatch = cachedMenuItems.stream()
                .filter(mi -> mi.getName().equalsIgnoreCase(trimmedName))
                .findFirst();
        
        if (exactMatch.isPresent()) {
            return exactMatch;
        }
        
        // 2. 부분 매칭 (한글 이름 포함)
        Optional<MenuItemResponseDto> partialMatch = cachedMenuItems.stream()
                .filter(mi -> mi.getName().toLowerCase().contains(trimmedName.toLowerCase()) ||
                             trimmedName.toLowerCase().contains(mi.getName().toLowerCase()))
                .findFirst();
        
        return partialMatch;
    }

    /**
     * 활성 메뉴 아이템 목록 (프롬프트용)
     */
    public String getMenuItemListForPrompt() {
        loadCache();
        List<String> items = cachedMenuItems.stream()
                .filter(mi -> mi.getStock() > 0) // 재고가 있는 것만
                .map(mi -> String.format("- %s (%d원, 재고: %d)",
                        mi.getName(),
                        mi.getUnitPrice() != null ? mi.getUnitPrice().intValue() : 0,
                        mi.getStock()))
                .collect(Collectors.toList());
        
        if (items.isEmpty()) {
            return "(현재 추가 가능한 메뉴 아이템이 없습니다)";
        }
        
        return String.join("\n", items);
    }
}
