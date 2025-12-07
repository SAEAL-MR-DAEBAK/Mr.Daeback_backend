package com.saeal.MrDaebackService.voiceOrder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private String dinnerId;
    private String dinnerName;
    private String servingStyleId;
    private String servingStyleName;
    private int quantity;
    private int basePrice;      // dinner 기본 가격 (스타일 변경 시 필요)
    private int unitPrice;
    private int totalPrice;

    // ★ 커스터마이징: 제외할 구성요소 목록 (예: ["steak", "salad"])
    @Builder.Default
    private List<String> excludedItems = new ArrayList<>();

    // ★ 구성요소 목록 (예: {"스테이크": 1, "샐러드": 1})
    @Builder.Default
    private Map<String, Integer> components = new LinkedHashMap<>();

    // ★ 개별 상품 식별자 (수량 2개 이상일 때 각각 구분용)
    private int itemIndex;  // 0부터 시작 (예: Valentine Dinner #1, #2)

    /**
     * 제외 아이템 추가
     */
    public void addExcludedItem(String itemName) {
        if (excludedItems == null) {
            excludedItems = new ArrayList<>();
        }
        if (!excludedItems.contains(itemName.toLowerCase())) {
            excludedItems.add(itemName.toLowerCase());
        }
    }

    /**
     * 제외 아이템 제거 (다시 포함)
     */
    public void removeExcludedItem(String itemName) {
        if (excludedItems != null) {
            excludedItems.remove(itemName.toLowerCase());
        }
    }

    /**
     * 구성요소 수량 설정
     */
    public void setComponentQuantity(String itemName, int quantity) {
        if (components == null) {
            components = new LinkedHashMap<>();
        }
        // 기존 키 찾기 (대소문자 무시)
        String existingKey = findComponentKey(itemName);
        if (existingKey != null) {
            if (quantity <= 0) {
                components.remove(existingKey);
            } else {
                components.put(existingKey, quantity);
            }
        } else if (quantity > 0) {
            components.put(itemName, quantity);
        }
    }

    /**
     * 구성요소 수량 증가
     */
    public void increaseComponentQuantity(String itemName, int amount) {
        if (components == null) {
            components = new LinkedHashMap<>();
        }
        String existingKey = findComponentKey(itemName);
        if (existingKey != null) {
            int currentQty = components.getOrDefault(existingKey, 0);
            components.put(existingKey, currentQty + amount);
        } else {
            components.put(itemName, amount);
        }
    }

    /**
     * 구성요소 수량 감소
     */
    public void decreaseComponentQuantity(String itemName, int amount) {
        if (components == null) return;
        String existingKey = findComponentKey(itemName);
        if (existingKey != null) {
            int currentQty = components.getOrDefault(existingKey, 0);
            int newQty = Math.max(0, currentQty - amount);
            if (newQty <= 0) {
                components.remove(existingKey);
            } else {
                components.put(existingKey, newQty);
            }
        }
    }

    /**
     * 구성요소 키 찾기 (대소문자/부분매칭)
     */
    private String findComponentKey(String itemName) {
        if (components == null || itemName == null) return null;
        String lowerInput = itemName.toLowerCase();
        for (String key : components.keySet()) {
            if (key.toLowerCase().contains(lowerInput) || lowerInput.contains(key.toLowerCase())) {
                return key;
            }
        }
        return null;
    }

    /**
     * 구성요소 수량 가져오기
     */
    public int getComponentQuantity(String itemName) {
        if (components == null) return 0;
        String existingKey = findComponentKey(itemName);
        return existingKey != null ? components.getOrDefault(existingKey, 0) : 0;
    }

    /**
     * 커스터마이징 정보 포함한 표시 이름
     */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        sb.append(dinnerName);
        if (servingStyleName != null) {
            sb.append(" (").append(servingStyleName).append(")");
        }
        if (itemIndex > 0) {
            sb.append(" #").append(itemIndex);
        }
        // 구성요소 표시
        sb.append(getComponentsDisplay());
        return sb.toString();
    }

    /**
     * 구성요소 표시 문자열 (제외된 것 제외)
     */
    public String getComponentsDisplay() {
        if (components == null || components.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(" [");
        boolean first = true;

        for (Map.Entry<String, Integer> entry : components.entrySet()) {
            String itemName = entry.getKey();
            // 제외된 아이템은 건너뛰기
            if (excludedItems != null && excludedItems.stream()
                    .anyMatch(ex -> ex.equalsIgnoreCase(itemName))) {
                continue;
            }

            if (!first) {
                sb.append(", ");
            }
            sb.append(itemName).append(" : ").append(entry.getValue()).append("개");
            first = false;
        }

        if (first) {
            // 모든 아이템이 제외된 경우
            return "";
        }

        sb.append("]");
        return sb.toString();
    }
}
