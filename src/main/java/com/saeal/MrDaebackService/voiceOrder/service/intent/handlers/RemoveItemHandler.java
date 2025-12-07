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
 * REMOVE_ITEM Intent 처리
 */
@Component
public class RemoveItemHandler extends AbstractIntentHandler {

    public RemoveItemHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.REMOVE_ITEM;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        List<OrderItemDto> orderItems = context.getOrderItems();

        if (orderItems.isEmpty()) {
            return IntentResult.of("장바구니가 비어있어요!", OrderFlowState.IDLE);
        }

        if (entities == null || entities.getMenuName() == null) {
            return IntentResult.of("어떤 메뉴를 삭제할까요?", OrderFlowState.ASKING_MORE_DINNER);
        }

        String menuName = entities.getMenuName();

        // "LAST"인 경우 마지막 아이템 삭제
        if ("LAST".equalsIgnoreCase(menuName)) {
            OrderItemDto removed = orderItems.remove(orderItems.size() - 1);
            OrderFlowState nextState = orderItems.isEmpty() ? OrderFlowState.SELECTING_MENU : OrderFlowState.ASKING_MORE_DINNER;

            return IntentResult.builder()
                    .message(removed.getDinnerName() + "을(를) 삭제했어요!")
                    .nextState(nextState)
                    .uiAction(UiAction.UPDATE_ORDER_LIST)
                    .updatedOrder(orderItems)
                    .build();
        }

        // 특정 메뉴 삭제
        int targetIdx = findTargetItemIndex(orderItems, menuName);

        if (targetIdx < 0) {
            return IntentResult.of("'" + menuName + "' 메뉴가 장바구니에 없어요.", OrderFlowState.ASKING_MORE_DINNER);
        }

        OrderItemDto removed = orderItems.remove(targetIdx);
        OrderFlowState nextState = orderItems.isEmpty() ? OrderFlowState.SELECTING_MENU : OrderFlowState.ASKING_MORE_DINNER;

        return IntentResult.builder()
                .message(removed.getDinnerName() + "을(를) 삭제했어요!")
                .nextState(nextState)
                .uiAction(UiAction.UPDATE_ORDER_LIST)
                .updatedOrder(orderItems)
                .build();
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
}
