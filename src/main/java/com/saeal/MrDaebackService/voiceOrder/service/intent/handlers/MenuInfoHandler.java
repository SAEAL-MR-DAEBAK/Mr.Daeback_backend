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
 * ASK_MENU_INFO Intent 처리
 */
@Component
public class MenuInfoHandler extends AbstractIntentHandler {

    public MenuInfoHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.ASK_MENU_INFO;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        String userMessage = context.getUserMessage();

        // 주소 관련 요청인지 확인
        boolean isAddressRelatedRequest = userMessage.contains("주소") || userMessage.contains("배달");

        if (isAddressRelatedRequest) {
            if (!context.getUserAddresses().isEmpty()) {
                String message = "등록된 주소 목록입니다:\n" + formatAddressList(context.getUserAddresses())
                        + "\n\n번호를 말씀해주시면 해당 주소로 배달해드릴게요!";
                return IntentResult.of(message, OrderFlowState.SELECTING_ADDRESS);
            } else {
                return IntentResult.of(
                        "등록된 주소가 없어요. 마이페이지에서 주소를 먼저 추가해주세요!",
                        OrderFlowState.IDLE
                );
            }
        }

        // 메뉴 정보 문의 - LLM 메시지 그대로 전달
        return IntentResult.of(
                getLlmMessage(context, "어떤 메뉴가 궁금하신가요?"),
                OrderFlowState.SELECTING_MENU
        );
    }
}
