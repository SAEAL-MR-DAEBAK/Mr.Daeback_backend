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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CUSTOMIZE_MENU, NO_CUSTOMIZE Intent 처리
 * - 여러 구성요소 한번에 변경 가능
 * - 예: "커피 1포트, 샴페인 2병 추가해줘"
 */
@Component
@Slf4j
public class CustomizeMenuHandler extends AbstractIntentHandler {

    // 구성요소 수량 패턴: "커피 1포트", "샴페인 2병", "스테이크 2개"
    private static final Pattern COMPONENT_QTY_PATTERN = Pattern.compile(
            "(스테이크|샐러드|수프|빵|와인|샴페인|커피|디저트|케이크|아이스크림|바게트빵|바게트|파스타|라이스|에그|스크램블)\\s*(\\d+)\\s*(개|병|잔|조각|포트)?",
            Pattern.CASE_INSENSITIVE
    );

    public CustomizeMenuHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.CUSTOMIZE_MENU || intent == UserIntent.NO_CUSTOMIZE;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        UserIntent intent = UserIntent.valueOf(context.getLlmResponse().getIntent().toUpperCase());

        if (intent == UserIntent.NO_CUSTOMIZE) {
            return IntentResult.builder()
                    .message("구성요소 변경 없이 진행할게요! 결제하시려면 '결제할게요'라고 말씀해주세요!")
                    .nextState(OrderFlowState.ORDERING)
                    .build();
        }

        // CUSTOMIZE_MENU 처리
        List<OrderItemDto> orderItems = context.getOrderItems();
        String userMessage = context.getUserMessage();

        if (orderItems.isEmpty()) {
            return IntentResult.of("먼저 메뉴를 주문해주세요!", OrderFlowState.ORDERING);
        }

        // 여러 구성요소 수량 변경 추출
        List<ComponentChange> changes = extractAllComponentChanges(userMessage);
        log.info("[CustomizeMenuHandler] Extracted {} component changes from: {}", changes.size(), userMessage);

        if (changes.isEmpty()) {
            // 단순 제외 처리 (예: "스테이크 빼줘")
            String menuItemName = extractMenuItemFromMessage(userMessage);
            if (menuItemName != null && isRemoveKeyword(userMessage)) {
                return handleRemoveItem(orderItems, menuItemName, userMessage);
            }

            return IntentResult.of(
                    "구성요소를 변경하시려면 '커피 1포트 추가해줘' 또는 '스테이크 빼줘'라고 말씀해주세요!",
                    OrderFlowState.ORDERING
            );
        }

        // 여러 구성요소 한번에 변경
        return handleMultipleChanges(orderItems, changes, userMessage);
    }

    /**
     * 여러 구성요소 변경 처리
     */
    private IntentResult handleMultipleChanges(List<OrderItemDto> orderItems, List<ComponentChange> changes, String userMessage) {
        Integer targetIndex = extractItemIndexFromMessage(userMessage);
        StringBuilder changeMessages = new StringBuilder();
        int totalPriceDiff = 0;

        // 대상 아이템 결정 (특정 번호 or 첫 번째 디너)
        OrderItemDto targetItem;
        int itemIdx;
        if (targetIndex != null && targetIndex > 0 && targetIndex <= orderItems.size()) {
            itemIdx = targetIndex - 1;
        } else {
            // 첫 번째 디너 아이템 찾기
            itemIdx = 0;
            for (int i = 0; i < orderItems.size(); i++) {
                if (orderItems.get(i).getDinnerId() != null) {
                    itemIdx = i;
                    break;
                }
            }
        }
        targetItem = orderItems.get(itemIdx);

        // 각 변경 적용
        for (ComponentChange change : changes) {
            int oldQty = targetItem.getComponentQuantity(change.itemName);
            int newQty = change.quantity;

            // 수량 설정
            targetItem.setComponentQuantity(change.itemName, newQty);
            // 제외 목록에서 제거 (수량 설정하면 다시 포함)
            targetItem.removeExcludedItem(change.itemName);

            // 가격 업데이트: (newQty - oldQty) × unitPrice
            int itemPrice = menuMatcher.getMenuItemPrice(targetItem.getDinnerId(), change.itemName);
            int priceDiff = (newQty - oldQty) * itemPrice;
            totalPriceDiff += priceDiff;

            if (changeMessages.length() > 0) {
                changeMessages.append(", ");
            }
            changeMessages.append(change.itemName).append(" ").append(newQty).append(change.unit);

            log.info("[CustomizeMenuHandler] Changed {} quantity: {} -> {} for item #{}, priceDiff: {}",
                    change.itemName, oldQty, newQty, itemIdx + 1, priceDiff);
        }

        // 가격 반영
        targetItem.setUnitPrice(targetItem.getUnitPrice() + totalPriceDiff);
        targetItem.setTotalPrice(targetItem.getUnitPrice() * targetItem.getQuantity());
        orderItems.set(itemIdx, targetItem);

        String message = targetItem.getDinnerName() + "에 " + changeMessages + "로 변경했어요! ✨\n\n" +
                "더 변경하시려면 말씀해주시고, 결제하시려면 '결제할게요'라고 해주세요!";

        return IntentResult.builder()
                .message(message)
                .nextState(OrderFlowState.ORDERING)
                .uiAction(UiAction.UPDATE_ORDER_LIST)
                .updatedOrder(orderItems)
                .build();
    }

    /**
     * 구성요소 제거 처리
     */
    private IntentResult handleRemoveItem(List<OrderItemDto> orderItems, String menuItemName, String userMessage) {
        Integer targetIndex = extractItemIndexFromMessage(userMessage);
        String itemToExclude = menuItemName.toLowerCase();

        String message;
        if (targetIndex != null && targetIndex > 0 && targetIndex <= orderItems.size()) {
            // 특정 번호의 아이템에만 적용
            OrderItemDto targetItem = orderItems.get(targetIndex - 1);
            targetItem.addExcludedItem(itemToExclude);

            // 가격 업데이트
            int itemPrice = menuMatcher.getMenuItemPrice(targetItem.getDinnerId(), itemToExclude);
            int defaultQty = menuMatcher.getMenuItemDefaultQuantity(targetItem.getDinnerId(), itemToExclude);
            int priceReduction = itemPrice * defaultQty;
            targetItem.setUnitPrice(targetItem.getUnitPrice() - priceReduction);
            targetItem.setTotalPrice(targetItem.getUnitPrice() * targetItem.getQuantity());

            message = targetIndex + "번 " + targetItem.getDinnerName() + "에서 " + itemToExclude + "을(를) 뺐어요!";
        } else {
            // 모든 아이템에 적용
            int appliedCount = 0;
            for (OrderItemDto item : orderItems) {
                if (item.getDinnerId() != null) {
                    item.addExcludedItem(itemToExclude);

                    int itemPrice = menuMatcher.getMenuItemPrice(item.getDinnerId(), itemToExclude);
                    int defaultQty = menuMatcher.getMenuItemDefaultQuantity(item.getDinnerId(), itemToExclude);
                    int priceReduction = itemPrice * defaultQty;
                    item.setUnitPrice(item.getUnitPrice() - priceReduction);
                    item.setTotalPrice(item.getUnitPrice() * item.getQuantity());

                    appliedCount++;
                }
            }
            message = "모든 디너(" + appliedCount + "개)에서 " + itemToExclude + "을(를) 뺐어요!";
        }

        message += "\n\n더 변경하시려면 말씀해주시고, 결제하시려면 '결제할게요'라고 해주세요!";

        return IntentResult.builder()
                .message(message)
                .nextState(OrderFlowState.ORDERING)
                .uiAction(UiAction.UPDATE_ORDER_LIST)
                .updatedOrder(orderItems)
                .build();
    }

    /**
     * 사용자 메시지에서 모든 구성요소 변경 정보 추출
     * 예: "커피 1포트, 샴페인 2병 추가해줘" -> [{커피, 1, 포트}, {샴페인, 2, 병}]
     */
    private List<ComponentChange> extractAllComponentChanges(String message) {
        List<ComponentChange> changes = new ArrayList<>();
        if (message == null) return changes;

        Matcher matcher = COMPONENT_QTY_PATTERN.matcher(message);
        while (matcher.find()) {
            String itemName = matcher.group(1);
            int quantity = Integer.parseInt(matcher.group(2));
            String unit = matcher.group(3) != null ? matcher.group(3) : "개";
            changes.add(new ComponentChange(itemName, quantity, unit));
            log.debug("[CustomizeMenuHandler] Found component: {} {} {}", itemName, quantity, unit);
        }
        return changes;
    }

    /**
     * 사용자 메시지에서 구성요소 이름 추출 (수량 없이)
     */
    private String extractMenuItemFromMessage(String message) {
        if (message == null) return null;

        String[] menuItems = {"스테이크", "샐러드", "수프", "빵", "와인", "샴페인", "커피",
                "디저트", "케이크", "아이스크림", "바게트", "파스타", "라이스", "에그"};

        for (String item : menuItems) {
            if (message.contains(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 사용자 메시지에 제거 키워드가 있는지 확인
     */
    private boolean isRemoveKeyword(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("빼") || lower.contains("제외") || lower.contains("삭제")
                || lower.contains("없이") || lower.contains("remove");
    }

    /**
     * 구성요소 변경 정보
     */
    private static class ComponentChange {
        final String itemName;
        final int quantity;
        final String unit;

        ComponentChange(String itemName, int quantity, String unit) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.unit = unit;
        }
    }
}
