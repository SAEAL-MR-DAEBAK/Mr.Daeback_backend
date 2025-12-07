package com.saeal.MrDaebackService.voiceOrder.service.intent.handlers;

import com.saeal.MrDaebackService.voiceOrder.dto.LlmResponseDto;
import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;
import com.saeal.MrDaebackService.voiceOrder.service.CartManager;
import com.saeal.MrDaebackService.voiceOrder.service.MenuMatcher;
import com.saeal.MrDaebackService.voiceOrder.service.intent.AbstractIntentHandler;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentContext;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SELECT_ADDRESS Intent 처리
 */
@Component
public class SelectAddressHandler extends AbstractIntentHandler {

    public SelectAddressHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.SELECT_ADDRESS;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        List<String> userAddresses = context.getUserAddresses();

        if (entities != null && entities.getAddressIndex() != null) {
            int idx = entities.getAddressIndex() - 1;
            if (idx >= 0 && idx < userAddresses.size()) {
                String selectedAddress = userAddresses.get(idx);
                String message = selectedAddress + "로 배달해드릴게요! 🎉\n\n" +
                        "배달 받으실 날짜와 시간을 알려주세요!\n" +
                        "예) 오늘 저녁 7시, 내일 오후 6시, 12월 25일 저녁 7시";
                return IntentResult.builder()
                        .message(message)
                        .nextState(OrderFlowState.ASKING_DELIVERY_TIME)
                        .selectedAddress(selectedAddress)
                        .build();
            } else {
                return IntentResult.of(
                        "올바른 주소 번호를 선택해주세요. (1~" + userAddresses.size() + ")",
                        OrderFlowState.SELECTING_ADDRESS
                );
            }
        }

        // addressIndex가 없는 경우
        if (!userAddresses.isEmpty()) {
            return IntentResult.of(
                    "주소 번호를 말씀해주세요!\n\n" + formatAddressList(userAddresses),
                    OrderFlowState.SELECTING_ADDRESS
            );
        } else {
            return IntentResult.of(
                    "등록된 주소가 없어요. 마이페이지에서 주소를 먼저 추가해주세요!",
                    OrderFlowState.SELECTING_ADDRESS
            );
        }
    }
}
