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
 * SELECT_STYLE Intent 처리
 * @deprecated OrderMenuHandler에서 스타일 선택까지 통합 처리 (간소화된 플로우)
 */
@Deprecated
@Component
public class SelectStyleHandler extends AbstractIntentHandler {

    public SelectStyleHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.SELECT_STYLE;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        OrderItemDto pendingItem = context.getPendingItem();
        int pendingIdx = context.getPendingItemIndex();
        List<OrderItemDto> orderItems = context.getOrderItems();

        if (entities == null || entities.getStyleName() == null) {
            return IntentResult.of("먼저 메뉴를 선택해주세요!", OrderFlowState.SELECTING_MENU);
        }

        if (pendingItem == null || pendingIdx < 0) {
            return IntentResult.of("먼저 메뉴를 선택해주세요!", OrderFlowState.SELECTING_MENU);
        }

        // 스타일 제한 검사
        if (!menuMatcher.isStyleAvailableForDinner(pendingItem.getDinnerName(), entities.getStyleName())) {
            return IntentResult.of(
                    pendingItem.getDinnerName() + "는 Simple Style을 제공하지 않아요. Grand Style 또는 Deluxe Style 중에서 선택해주세요!",
                    OrderFlowState.SELECTING_STYLE
            );
        }

        var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
        if (styleOpt.isEmpty()) {
            return IntentResult.of("죄송해요, '" + entities.getStyleName() + "' 스타일을 찾을 수 없어요.", OrderFlowState.SELECTING_STYLE);
        }

        OrderItemDto updated = cartManager.applyStyleToItem(pendingItem, styleOpt.get());
        orderItems.set(pendingIdx, updated);

        if (updated.getQuantity() == 0) {
            return IntentResult.builder()
                    .message(styleOpt.get().getStyleName() + "로 선택하셨어요! " + buildQuantityQuestion(updated.getDinnerName()))
                    .nextState(OrderFlowState.SELECTING_QUANTITY)
                    .uiAction(UiAction.UPDATE_ORDER_LIST)
                    .updatedOrder(orderItems)
                    .build();
        } else {
            return IntentResult.builder()
                    .message(updated.getDinnerName() + " " + styleOpt.get().getStyleName() + " 주문 완료! 다른 디너 메뉴도 추가하시겠어요?")
                    .nextState(OrderFlowState.ASKING_MORE_DINNER)
                    .uiAction(UiAction.UPDATE_ORDER_LIST)
                    .updatedOrder(orderItems)
                    .build();
        }
    }
}
