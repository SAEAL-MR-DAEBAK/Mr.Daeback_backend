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
 * CONFIRM_YES, CONFIRM_NO, ADD_TO_CART Intent 처리
 * @deprecated CheckoutHandler에서 결제 처리 통합 (간소화된 플로우)
 */
@Deprecated
@Component
public class ConfirmHandler extends AbstractIntentHandler {

    public ConfirmHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.CONFIRM_YES
                || intent == UserIntent.CONFIRM_NO
                || intent == UserIntent.ADD_TO_CART;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        UserIntent intent = UserIntent.valueOf(context.getLlmResponse().getIntent().toUpperCase());
        List<OrderItemDto> orderItems = context.getOrderItems();

        if (intent == UserIntent.CONFIRM_NO) {
            return handleConfirmNo(context, orderItems);
        }

        // CONFIRM_YES 또는 ADD_TO_CART
        return handleConfirmYes(context, orderItems);
    }

    private IntentResult handleConfirmNo(IntentContext context, List<OrderItemDto> orderItems) {
        // 미완성 디너 아이템 제거 (추가 메뉴는 스타일 필요 없으므로 제외하지 않음)
        orderItems.removeIf(item -> item.getDinnerId() != null &&
                (item.getQuantity() == 0 || item.getServingStyleId() == null));

        if (!orderItems.isEmpty()) {
            if (context.getSelectedAddress() == null || context.getSelectedAddress().isEmpty()) {
                if (!context.getUserAddresses().isEmpty()) {
                    return IntentResult.of(
                            "배달 주소를 선택해주세요!\n" + formatAddressList(context.getUserAddresses()),
                            OrderFlowState.SELECTING_ADDRESS
                    );
                } else {
                    return IntentResult.of("저장된 주소가 없어요. 마이페이지에서 주소를 추가해주세요.", OrderFlowState.IDLE);
                }
            }

            String message = "주문을 확정하시겠어요?\n" + buildOrderSummary(orderItems, context.getSelectedAddress());
            return IntentResult.builder()
                    .message(message)
                    .nextState(OrderFlowState.CONFIRMING)
                    .uiAction(UiAction.SHOW_CONFIRM_MODAL)
                    .updatedOrder(orderItems)
                    .build();
        }

        return IntentResult.of("장바구니가 비어있어요. 먼저 메뉴를 선택해주세요!", OrderFlowState.SELECTING_MENU);
    }

    private IntentResult handleConfirmYes(IntentContext context, List<OrderItemDto> orderItems) {
        // 미완성 디너 아이템 제거 (추가 메뉴는 스타일 필요 없으므로 제외하지 않음)
        orderItems.removeIf(item -> item.getDinnerId() != null &&
                (item.getQuantity() == 0 || item.getServingStyleId() == null));

        if (orderItems.isEmpty()) {
            return IntentResult.of("장바구니가 비어있어요!", OrderFlowState.SELECTING_MENU);
        }

        if (context.getSelectedAddress() == null || context.getSelectedAddress().isEmpty()) {
            if (!context.getUserAddresses().isEmpty()) {
                return IntentResult.of(
                        "배달 주소를 선택해주세요!\n" + formatAddressList(context.getUserAddresses()),
                        OrderFlowState.SELECTING_ADDRESS
                );
            } else {
                return IntentResult.of("저장된 주소가 없어요.", OrderFlowState.IDLE);
            }
        }

        // 확정 → CHECKOUT_READY로 전환
        return IntentResult.builder()
                .message("주문이 확정되었습니다! 이제 결제 단계입니다. 결제를 원하실까요?")
                .nextState(OrderFlowState.CHECKOUT_READY)
                .uiAction(UiAction.PROCEED_TO_CHECKOUT)
                .updatedOrder(orderItems)
                .build();
    }
}
