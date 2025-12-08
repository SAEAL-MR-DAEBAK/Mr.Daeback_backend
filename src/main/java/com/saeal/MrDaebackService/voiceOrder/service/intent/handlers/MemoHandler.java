package com.saeal.MrDaebackService.voiceOrder.service.intent.handlers;

import com.saeal.MrDaebackService.voiceOrder.dto.LlmResponseDto;
import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UiAction;
import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;
import com.saeal.MrDaebackService.voiceOrder.service.CartManager;
import com.saeal.MrDaebackService.voiceOrder.service.MenuMatcher;
import com.saeal.MrDaebackService.voiceOrder.service.intent.AbstractIntentHandler;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentContext;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentResult;
import org.springframework.stereotype.Component;

/**
 * SET_MEMO, NO_MEMO Intent 처리
 * @deprecated 간소화된 플로우에서 메모 단계 제거
 */
@Deprecated
@Component
public class MemoHandler extends AbstractIntentHandler {

    public MemoHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.SET_MEMO || intent == UserIntent.NO_MEMO;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        UserIntent intent = UserIntent.valueOf(context.getLlmResponse().getIntent().toUpperCase());

        if (intent == UserIntent.NO_MEMO) {
            String message = "메모 없이 진행할게요! 주문을 확정하시겠어요?\n"
                    + buildOrderSummary(context.getOrderItems(), context.getSelectedAddress());

            return IntentResult.withAction(message, OrderFlowState.CONFIRMING, UiAction.SHOW_CONFIRM_MODAL);
        }

        // SET_MEMO 처리
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        String memo = (entities != null) ? entities.getMemo() : null;

        String message = (memo != null ? "'" + memo + "' 메모 완료! " : "") + "주문을 확정하시겠어요?\n"
                + buildOrderSummary(context.getOrderItems(), context.getSelectedAddress());

        return IntentResult.builder()
                .message(message)
                .nextState(OrderFlowState.CONFIRMING)
                .uiAction(UiAction.SHOW_CONFIRM_MODAL)
                .memo(memo)
                .build();
    }
}
