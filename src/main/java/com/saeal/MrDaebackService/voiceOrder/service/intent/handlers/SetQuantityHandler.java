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

import java.util.ArrayList;
import java.util.List;

/**
 * SET_QUANTITY Intent 처리
 * @deprecated 스타일 선택 시 자동으로 수량 1개로 설정 (간소화된 플로우)
 */
@Deprecated
@Component
public class SetQuantityHandler extends AbstractIntentHandler {

    public SetQuantityHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.SET_QUANTITY;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        OrderItemDto pendingItem = context.getPendingItem();
        int pendingIdx = context.getPendingItemIndex();
        List<OrderItemDto> orderItems = context.getOrderItems();

        if (entities == null || entities.getQuantity() == null) {
            return IntentResult.of("먼저 메뉴를 선택해주세요!", OrderFlowState.SELECTING_MENU);
        }

        if (pendingItem == null || pendingIdx < 0) {
            return IntentResult.of("먼저 메뉴를 선택해주세요!", OrderFlowState.SELECTING_MENU);
        }

        if (pendingItem.getServingStyleId() == null) {
            return IntentResult.of("먼저 스타일을 선택해주세요!", OrderFlowState.SELECTING_STYLE);
        }

        int qty = entities.getQuantity();

        // 수량 2개 이상이면 개별 상품으로 분리 (각각 커스터마이징 가능)
        if (qty >= 2) {
            orderItems.remove(pendingIdx);

            for (int i = 1; i <= qty; i++) {
                OrderItemDto individualItem = OrderItemDto.builder()
                        .dinnerId(pendingItem.getDinnerId())
                        .dinnerName(pendingItem.getDinnerName())
                        .servingStyleId(pendingItem.getServingStyleId())
                        .servingStyleName(pendingItem.getServingStyleName())
                        .quantity(1)
                        .basePrice(pendingItem.getBasePrice())
                        .unitPrice(pendingItem.getUnitPrice())
                        .totalPrice(pendingItem.getUnitPrice())
                        .itemIndex(i)
                        .excludedItems(new ArrayList<>())
                        .build();
                orderItems.add(individualItem);
            }

            return IntentResult.builder()
                    .message(pendingItem.getDinnerName() + " " + qty + "개를 개별 상품으로 추가했어요! (각각 커스터마이징 가능) 다른 디너 메뉴도 추가하시겠어요?")
                    .nextState(OrderFlowState.ASKING_MORE_DINNER)
                    .uiAction(UiAction.UPDATE_ORDER_LIST)
                    .updatedOrder(orderItems)
                    .build();
        } else {
            // 1개면 기존 방식대로
            OrderItemDto updated = cartManager.setQuantity(pendingItem, qty);
            updated.setItemIndex(0);
            if (updated.getExcludedItems() == null) {
                updated.setExcludedItems(new ArrayList<>());
            }
            orderItems.set(pendingIdx, updated);

            return IntentResult.builder()
                    .message(updated.getDinnerName() + " " + qty + "개 주문 완료! 다른 디너 메뉴도 추가하시겠어요?")
                    .nextState(OrderFlowState.ASKING_MORE_DINNER)
                    .uiAction(UiAction.UPDATE_ORDER_LIST)
                    .updatedOrder(orderItems)
                    .build();
        }
    }
}
