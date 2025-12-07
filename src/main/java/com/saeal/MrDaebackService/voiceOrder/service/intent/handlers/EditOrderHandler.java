package com.saeal.MrDaebackService.voiceOrder.service.intent.handlers;

import com.saeal.MrDaebackService.voiceOrder.dto.LlmResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UiAction;
import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;
import com.saeal.MrDaebackService.voiceOrder.service.CartManager;
import com.saeal.MrDaebackService.voiceOrder.service.MenuMatcher;
import com.saeal.MrDaebackService.voiceOrder.service.intent.AbstractIntentHandler;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentContext;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EDIT_ORDER Intent 처리
 * - 수량 변경, 스타일 변경
 * - 구성요소 제외/추가 (예: "1번 스테이크 빼줘")
 * - 구성요소 수량 변경 (예: "1번 스테이크 2개로 해줘")
 */
@Component
@Slf4j
public class EditOrderHandler extends AbstractIntentHandler {

    // "1번", "#1", "첫번째" 등에서 인덱스 추출
    private static final Pattern INDEX_PATTERN = Pattern.compile("(\\d+)\\s*번|#(\\d+)|첫\\s*번째|두\\s*번째|세\\s*번째");

    // 구성요소 수량 패턴: "스테이크 2개", "와인 3개로", "바게트빵 6개", "샴페인 2병"
    private static final Pattern COMPONENT_QTY_PATTERN = Pattern.compile("(스테이크|샐러드|수프|빵|와인|샴페인|커피|디저트|케이크|아이스크림|바게트빵|바게트|파스타|라이스|에그|스크램블)\\s*을?를?\\s*(\\d+)\\s*(개|병|잔|조각)?");

    public EditOrderHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.EDIT_ORDER;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        List<OrderItemDto> orderItems = context.getOrderItems();
        String userMessage = context.getUserMessage();

        log.info("[EditOrderHandler] userMessage: {}, entities: {}", userMessage, entities);

        if (orderItems.isEmpty()) {
            return IntentResult.of("수정할 주문이 없어요. 먼저 메뉴를 주문해주세요!", OrderFlowState.SELECTING_MENU);
        }

        // 1. 대상 아이템 인덱스 찾기 (사용자 메시지에서 "1번", "#1" 등 추출)
        int targetIdx = findTargetIndexFromMessage(userMessage, orderItems.size());

        // entities에서 itemIndex가 있으면 사용
        if (targetIdx < 0 && entities != null && entities.getItemIndex() != null) {
            targetIdx = entities.getItemIndex() - 1; // 1-based to 0-based
        }

        // 메뉴 이름으로 찾기 (인덱스가 없는 경우)
        if (targetIdx < 0 && entities != null && entities.getMenuName() != null) {
            targetIdx = findTargetItemIndex(orderItems, entities.getMenuName());
        }

        // 여전히 못 찾으면 첫 번째 아이템
        if (targetIdx < 0) {
            targetIdx = 0;
        }

        if (targetIdx >= orderItems.size()) {
            return IntentResult.of("해당 번호의 주문이 없어요.", OrderFlowState.CUSTOMIZING_MENU);
        }

        OrderItemDto targetItem = orderItems.get(targetIdx);
        String message = "";

        // 2. 구성요소 제외/추가 처리
        String menuItemName = (entities != null) ? entities.getEffectiveMenuItemName() : null;

        // 사용자 메시지에서 구성요소 이름 추출 (entities에 없는 경우)
        if (menuItemName == null || menuItemName.isEmpty()) {
            menuItemName = extractMenuItemFromMessage(userMessage);
        }

        // 2-1. 구성요소 수량 변경 체크 (다중 변경 지원: "바게트빵 6개, 샴페인 2병")
        List<ComponentQuantityChange> qtyChanges = extractAllComponentQuantitiesFromMessage(userMessage);
        if (!qtyChanges.isEmpty()) {
            StringBuilder changeMessages = new StringBuilder();
            int totalPriceDiff = 0;

            for (ComponentQuantityChange qtyChange : qtyChanges) {
                int oldQty = targetItem.getComponentQuantity(qtyChange.itemName);
                targetItem.setComponentQuantity(qtyChange.itemName, qtyChange.quantity);
                // 제외 목록에서 제거 (수량 설정하면 다시 포함)
                targetItem.removeExcludedItem(qtyChange.itemName);

                // ★ 가격 업데이트: (newQty - oldQty) × unitPrice
                int itemPrice = menuMatcher.getMenuItemPrice(targetItem.getDinnerId(), qtyChange.itemName);
                int priceDiff = (qtyChange.quantity - oldQty) * itemPrice;
                totalPriceDiff += priceDiff;

                if (changeMessages.length() > 0) {
                    changeMessages.append(", ");
                }
                changeMessages.append(qtyChange.itemName).append(" ").append(qtyChange.quantity).append("개");
                log.info("[EditOrderHandler] Changed {} quantity: {} -> {} for item #{}, priceDiff: {}",
                        qtyChange.itemName, oldQty, qtyChange.quantity, targetIdx + 1, priceDiff);
            }

            targetItem.setUnitPrice(targetItem.getUnitPrice() + totalPriceDiff);
            targetItem.setTotalPrice(targetItem.getUnitPrice() * targetItem.getQuantity());
            orderItems.set(targetIdx, targetItem);

            message = (targetIdx + 1) + "번 " + targetItem.getDinnerName() + "의 " +
                    changeMessages.toString() + "로 변경했어요!";
        }
        // 2-2. 구성요소 제외/추가 처리 (수량 변경이 없을 때)
        else if (menuItemName != null && !menuItemName.isEmpty()) {
            boolean isRemove = (entities != null && entities.isRemoveAction())
                    || isRemoveKeywordInMessage(userMessage);
            boolean isAdd = (entities != null && entities.isAddAction())
                    || isAddKeywordInMessage(userMessage);

            // ★ 추가가 우선 (더해줘, 추가 등의 키워드가 있으면 추가로 처리)
            if (isAdd && !isRemove) {
                // 구성요소 수량 증가 또는 제외 취소
                int currentQty = targetItem.getComponentQuantity(menuItemName);
                targetItem.setComponentQuantity(menuItemName, currentQty + 1);
                targetItem.removeExcludedItem(menuItemName);

                // ★ 가격 업데이트: +unitPrice (1개 추가)
                int itemPrice = menuMatcher.getMenuItemPrice(targetItem.getDinnerId(), menuItemName);
                targetItem.setUnitPrice(targetItem.getUnitPrice() + itemPrice);
                targetItem.setTotalPrice(targetItem.getUnitPrice() * targetItem.getQuantity());

                orderItems.set(targetIdx, targetItem);
                message = (targetIdx + 1) + "번 " + targetItem.getDinnerName() + "에 " + menuItemName + "를 추가했어요! (현재 " + (currentQty + 1) + "개)";
                log.info("[EditOrderHandler] Added {} to item #{}, new qty: {}, priceAdded: {}", menuItemName, targetIdx + 1, currentQty + 1, itemPrice);
            } else if (isRemove) {
                // 제외 처리
                targetItem.addExcludedItem(menuItemName);

                // ★ 가격 업데이트: -unitPrice × defaultQuantity (기본 수량만큼 감소)
                int itemPrice = menuMatcher.getMenuItemPrice(targetItem.getDinnerId(), menuItemName);
                int defaultQty = menuMatcher.getMenuItemDefaultQuantity(targetItem.getDinnerId(), menuItemName);
                int priceReduction = itemPrice * defaultQty;
                targetItem.setUnitPrice(targetItem.getUnitPrice() - priceReduction);
                targetItem.setTotalPrice(targetItem.getUnitPrice() * targetItem.getQuantity());

                orderItems.set(targetIdx, targetItem);
                message = (targetIdx + 1) + "번 " + targetItem.getDinnerName() + "에서 " + menuItemName + "를 빼드렸어요!";
                log.info("[EditOrderHandler] Excluded {} from item #{}, priceReduction: {}", menuItemName, targetIdx + 1, priceReduction);
            }
        }

        // 3. 수량 변경 (구성요소 제외/추가가 없을 때만)
        if (message.isEmpty() && entities != null && entities.getQuantity() != null && entities.getQuantity() > 0) {
            targetItem = cartManager.setQuantity(targetItem, entities.getQuantity());
            orderItems.set(targetIdx, targetItem);
            message = targetItem.getDinnerName() + " " + entities.getQuantity() + "개로 변경했어요!";
        }

        // 4. 스타일 변경 (구성요소 제외/추가가 없을 때만)
        if (message.isEmpty() && entities != null && entities.getStyleName() != null) {
            var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
            if (styleOpt.isPresent()) {
                targetItem = cartManager.changeStyle(targetItem, styleOpt.get());
                orderItems.set(targetIdx, targetItem);
                message = targetItem.getDinnerName() + " 스타일을 " + styleOpt.get().getStyleName() + "로 변경했어요!";
            }
        }

        if (message.isEmpty()) {
            message = "어떻게 수정할까요?";
        }

        message += "\n\n다른 수정이 필요하시면 말씀해주세요. 완료하시려면 '완료' 또는 '다음'이라고 해주세요!";

        return IntentResult.builder()
                .message(message)
                .nextState(OrderFlowState.CUSTOMIZING_MENU)
                .uiAction(UiAction.UPDATE_ORDER_LIST)
                .updatedOrder(orderItems)
                .build();
    }

    /**
     * 사용자 메시지에서 대상 인덱스 추출 ("1번", "#1", "첫번째" 등)
     */
    private int findTargetIndexFromMessage(String message, int maxSize) {
        if (message == null) return -1;

        Matcher matcher = INDEX_PATTERN.matcher(message);
        if (matcher.find()) {
            String group1 = matcher.group(1); // "1번"에서 1
            String group2 = matcher.group(2); // "#1"에서 1

            if (group1 != null) {
                int idx = Integer.parseInt(group1) - 1;
                return (idx >= 0 && idx < maxSize) ? idx : -1;
            }
            if (group2 != null) {
                int idx = Integer.parseInt(group2) - 1;
                return (idx >= 0 && idx < maxSize) ? idx : -1;
            }

            // "첫번째", "두번째" 등
            if (message.contains("첫")) return 0;
            if (message.contains("두")) return maxSize > 1 ? 1 : -1;
            if (message.contains("세")) return maxSize > 2 ? 2 : -1;
        }
        return -1;
    }

    /**
     * 사용자 메시지에서 구성요소 이름 추출
     */
    private String extractMenuItemFromMessage(String message) {
        if (message == null) return null;

        // 흔한 구성요소 키워드 목록
        String[] menuItems = {"스테이크", "샐러드", "수프", "빵", "와인", "샴페인", "커피",
                "디저트", "케이크", "아이스크림", "바게트", "파스타", "라이스"};

        for (String item : menuItems) {
            if (message.contains(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 사용자 메시지에 제외 키워드가 있는지 확인
     */
    private boolean isRemoveKeywordInMessage(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("빼") || lower.contains("제외") || lower.contains("삭제")
                || lower.contains("없이") || lower.contains("remove");
    }

    /**
     * 사용자 메시지에 추가 키워드가 있는지 확인
     */
    private boolean isAddKeywordInMessage(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("더해") || lower.contains("추가") || lower.contains("넣어")
                || lower.contains("add") || lower.contains("더 줘");
    }

    private int findTargetItemIndex(List<OrderItemDto> orderItems, String menuName) {
        for (int i = 0; i < orderItems.size(); i++) {
            if (orderItems.get(i).getDinnerName().equalsIgnoreCase(menuName)
                    || menuMatcher.isMatchingMenu(orderItems.get(i).getDinnerName(), menuName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 사용자 메시지에서 모든 구성요소 수량 변경 정보 추출 (다중 변경 지원)
     * 예: "바게트빵을 6개로, 샴페인을 2병으로" -> [{itemName: "바게트빵", quantity: 6}, {itemName: "샴페인", quantity: 2}]
     */
    private List<ComponentQuantityChange> extractAllComponentQuantitiesFromMessage(String message) {
        List<ComponentQuantityChange> changes = new java.util.ArrayList<>();
        if (message == null) return changes;

        Matcher matcher = COMPONENT_QTY_PATTERN.matcher(message);
        while (matcher.find()) {
            String itemName = matcher.group(1);
            int quantity = Integer.parseInt(matcher.group(2));
            changes.add(new ComponentQuantityChange(itemName, quantity));
        }
        return changes;
    }

    /**
     * 구성요소 수량 변경 정보를 담는 내부 클래스
     */
    private static class ComponentQuantityChange {
        final String itemName;
        final int quantity;

        ComponentQuantityChange(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }
}
