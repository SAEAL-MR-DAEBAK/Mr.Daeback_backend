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
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CUSTOMIZE_MENU, NO_CUSTOMIZE Intent 처리
 */
@Component
public class CustomizeMenuHandler extends AbstractIntentHandler {

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
            return IntentResult.of(
                    "구성 요소 변경 완료! 추가 메뉴(스테이크, 와인 등)를 더 주문하시겠어요?",
                    OrderFlowState.SELECTING_ADDITIONAL_MENU
            );
        }

        // CUSTOMIZE_MENU 처리
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        List<OrderItemDto> orderItems = context.getOrderItems();
        String userMessage = context.getUserMessage();

        // getEffectiveMenuItemName()으로 item 또는 menuItemName 필드 확인
        String menuItemName = (entities != null) ? entities.getEffectiveMenuItemName() : null;

        // 엔티티에 없으면 사용자 메시지에서 직접 추출
        if (menuItemName == null || menuItemName.isEmpty()) {
            menuItemName = extractMenuItemFromMessage(userMessage);
        }

        if (menuItemName == null || menuItemName.isEmpty()) {
            return IntentResult.of(
                    getLlmMessage(context, "구성 요소를 변경했어요! 더 변경하실 내용이 있으신가요?"),
                    OrderFlowState.CUSTOMIZING_MENU
            );
        }

        String itemToExclude = menuItemName.toLowerCase();
        Integer targetIndex = extractItemIndexFromMessage(userMessage);

        String message;
        if (targetIndex != null && targetIndex > 0 && targetIndex <= orderItems.size()) {
            // 특정 번호의 아이템에만 적용
            OrderItemDto targetItem = orderItems.get(targetIndex - 1);
            targetItem.addExcludedItem(itemToExclude);

            // ★ 가격 업데이트: -unitPrice × defaultQuantity
            int itemPrice = menuMatcher.getMenuItemPrice(targetItem.getDinnerId(), itemToExclude);
            int defaultQty = menuMatcher.getMenuItemDefaultQuantity(targetItem.getDinnerId(), itemToExclude);
            int priceReduction = itemPrice * defaultQty;
            targetItem.setUnitPrice(targetItem.getUnitPrice() - priceReduction);
            targetItem.setTotalPrice(targetItem.getUnitPrice() * targetItem.getQuantity());

            message = targetIndex + "번 " + targetItem.getDinnerName() + "에서 " + itemToExclude + "을(를) 뺐어요! 더 변경하실 부분 있으세요?";
        } else {
            // 전체 아이템에 적용
            int appliedCount = 0;
            for (OrderItemDto item : orderItems) {
                item.addExcludedItem(itemToExclude);

                // ★ 가격 업데이트: -unitPrice × defaultQuantity
                int itemPrice = menuMatcher.getMenuItemPrice(item.getDinnerId(), itemToExclude);
                int defaultQty = menuMatcher.getMenuItemDefaultQuantity(item.getDinnerId(), itemToExclude);
                int priceReduction = itemPrice * defaultQty;
                item.setUnitPrice(item.getUnitPrice() - priceReduction);
                item.setTotalPrice(item.getUnitPrice() * item.getQuantity());

                appliedCount++;
            }
            message = "모든 디너(" + appliedCount + "개)에서 " + itemToExclude + "을(를) 뺐어요! 더 변경하실 부분 있으세요?";
        }

        // 현재 상태 표시
        StringBuilder currentStatus = new StringBuilder("\n\n현재 주문:\n");
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItemDto item = orderItems.get(i);
            currentStatus.append(String.format("%d. %s", i + 1, item.getDisplayName())).append("\n");
        }
        message += currentStatus.toString();

        return IntentResult.builder()
                .message(message)
                .nextState(OrderFlowState.CUSTOMIZING_MENU)
                .uiAction(UiAction.UPDATE_ORDER_LIST)
                .updatedOrder(orderItems)
                .build();
    }

    /**
     * 사용자 메시지에서 구성요소 이름 추출
     */
    private String extractMenuItemFromMessage(String message) {
        if (message == null) return null;

        String[] menuItems = {"스테이크", "샐러드", "수프", "빵", "와인", "샴페인", "커피",
                "디저트", "케이크", "아이스크림", "바게트", "파스타", "라이스"};

        for (String item : menuItems) {
            if (message.contains(item)) {
                return item;
            }
        }
        return null;
    }
}
