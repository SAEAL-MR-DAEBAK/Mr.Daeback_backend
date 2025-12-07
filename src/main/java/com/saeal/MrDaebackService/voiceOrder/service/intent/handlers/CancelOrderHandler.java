package com.saeal.MrDaebackService.voiceOrder.service.intent.handlers;

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

import java.util.ArrayList;
import java.util.List;

/**
 * CANCEL_ORDER Intent 처리
 */
@Component
public class CancelOrderHandler extends AbstractIntentHandler {

    public CancelOrderHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.CANCEL_ORDER;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        List<OrderItemDto> emptyOrder = new ArrayList<>();

        return IntentResult.builder()
                .message("주문이 취소되었어요. 새로운 주문을 시작해주세요!")
                .nextState(OrderFlowState.IDLE)
                .uiAction(UiAction.SHOW_CANCEL_CONFIRM)
                .updatedOrder(emptyOrder)
                .build();
    }
}
