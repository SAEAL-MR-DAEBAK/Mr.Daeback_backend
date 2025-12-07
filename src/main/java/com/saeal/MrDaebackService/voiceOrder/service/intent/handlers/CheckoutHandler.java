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

import java.util.List;

/**
 * PROCEED_CHECKOUT Intent 처리
 */
@Component
public class CheckoutHandler extends AbstractIntentHandler {

    public CheckoutHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.PROCEED_CHECKOUT;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        List<OrderItemDto> orderItems = context.getOrderItems();

        // 미완성 디너 아이템 제거 (추가 메뉴는 스타일 필요 없으므로 제외하지 않음)
        orderItems.removeIf(item -> item.getDinnerId() != null &&
                (item.getQuantity() == 0 || item.getServingStyleId() == null));

        if (orderItems.isEmpty()) {
            return IntentResult.of("장바구니가 비어있어요!", OrderFlowState.SELECTING_MENU);
        }

        return IntentResult.builder()
                .message("결제를 진행하겠습니다.")
                .nextState(OrderFlowState.CHECKOUT_READY)
                .uiAction(UiAction.PROCEED_TO_CHECKOUT)
                .updatedOrder(orderItems)
                .build();
    }
}
