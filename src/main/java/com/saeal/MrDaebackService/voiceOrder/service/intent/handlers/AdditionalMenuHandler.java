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
 * ADD_ADDITIONAL_MENU, NO_ADDITIONAL_MENU Intent 처리
 */
@Component
public class AdditionalMenuHandler extends AbstractIntentHandler {

    public AdditionalMenuHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.ADD_ADDITIONAL_MENU || intent == UserIntent.NO_ADDITIONAL_MENU;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        UserIntent intent = UserIntent.valueOf(context.getLlmResponse().getIntent().toUpperCase());

        if (intent == UserIntent.NO_ADDITIONAL_MENU) {
            return IntentResult.of(
                    "추가 메뉴 주문을 완료했어요! 메모나 요청사항이 있으신가요? (예: 일회용 수저, 문 앞에 놔주세요)",
                    OrderFlowState.ENTERING_MEMO
            );
        }

        // ADD_ADDITIONAL_MENU 처리
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        List<OrderItemDto> orderItems = context.getOrderItems();

        // menuItemName, item, 또는 menuName에서 추가 메뉴 이름 추출
        String additionalItemName = null;
        if (entities != null) {
            additionalItemName = entities.getEffectiveMenuItemName();  // menuItemName 또는 item
            if (additionalItemName == null || additionalItemName.isEmpty()) {
                additionalItemName = entities.getMenuName();  // fallback: menuName 사용
            }
        }

        if (additionalItemName == null || additionalItemName.isEmpty()) {
            return IntentResult.of(
                    getLlmMessage(context, "어떤 추가 메뉴를 원하시나요? (예: 스테이크 추가, 와인 추가)"),
                    OrderFlowState.SELECTING_ADDITIONAL_MENU
            );
        }

        // ★ 수량 추출: quantity 또는 menuItemQuantity 사용
        Integer rawQty = entities.getQuantity();
        if (rawQty == null || rawQty <= 0) {
            rawQty = entities.getMenuItemQuantity();  // fallback: menuItemQuantity 사용
        }
        int qty = (rawQty != null && rawQty > 0) ? rawQty : 1;

        // 추가 메뉴를 별도의 OrderItemDto로 생성
        OrderItemDto additionalItem = OrderItemDto.builder()
                .dinnerId(null)
                .dinnerName("추가: " + additionalItemName)
                .servingStyleId(null)
                .servingStyleName(null)
                .quantity(qty)
                .basePrice(0)
                .unitPrice(0)
                .totalPrice(0)
                .itemIndex(0)
                .excludedItems(new ArrayList<>())
                .build();

        orderItems.add(additionalItem);

        return IntentResult.builder()
                .message(additionalItemName + " " + qty + "개를 추가했어요! 다른 추가 메뉴도 필요하신가요?")
                .nextState(OrderFlowState.SELECTING_ADDITIONAL_MENU)
                .uiAction(UiAction.UPDATE_ORDER_LIST)
                .updatedOrder(orderItems)
                .build();
    }
}
