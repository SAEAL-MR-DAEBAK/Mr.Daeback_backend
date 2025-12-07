package com.saeal.MrDaebackService.voiceOrder.service.intent.handlers;

import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;
import com.saeal.MrDaebackService.voiceOrder.service.CartManager;
import com.saeal.MrDaebackService.voiceOrder.service.MenuMatcher;
import com.saeal.MrDaebackService.voiceOrder.service.intent.AbstractIntentHandler;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentContext;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentResult;
import org.springframework.stereotype.Component;

/**
 * GREETING Intent 처리
 */
@Component
public class GreetingHandler extends AbstractIntentHandler {

    public GreetingHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.GREETING;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        String selectedAddress = context.getSelectedAddress();

        if (selectedAddress == null || selectedAddress.isEmpty()) {
            if (!context.getUserAddresses().isEmpty()) {
                String message = "안녕하세요! Mr.Daeback입니다. 🍽️\n먼저 배달받으실 주소를 선택해주세요!\n"
                        + formatAddressList(context.getUserAddresses());
                return IntentResult.of(message, OrderFlowState.SELECTING_ADDRESS);
            } else {
                return IntentResult.of(
                        "안녕하세요! Mr.Daeback입니다. 저장된 배달 주소가 없어요. 마이페이지에서 주소를 먼저 추가해주세요!",
                        OrderFlowState.IDLE
                );
            }
        }

        return IntentResult.of(
                "안녕하세요! Mr.Daeback입니다. 배달 주소는 '" + selectedAddress + "'로 설정되어 있어요. 어떤 메뉴를 주문하시겠어요?",
                OrderFlowState.SELECTING_MENU
        );
    }
}
