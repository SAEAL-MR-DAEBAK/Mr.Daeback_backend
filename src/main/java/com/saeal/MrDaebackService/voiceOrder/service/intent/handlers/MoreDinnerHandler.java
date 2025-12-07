package com.saeal.MrDaebackService.voiceOrder.service.intent.handlers;

import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
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
 * ADD_MORE_DINNER, NO_MORE_DINNER Intent 처리
 */
@Component
public class MoreDinnerHandler extends AbstractIntentHandler {

    public MoreDinnerHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.ADD_MORE_DINNER || intent == UserIntent.NO_MORE_DINNER;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        UserIntent intent = UserIntent.valueOf(context.getLlmResponse().getIntent().toUpperCase());

        if (intent == UserIntent.ADD_MORE_DINNER) {
            return IntentResult.of("네! 어떤 디너를 추가하시겠어요?", OrderFlowState.SELECTING_MENU);
        }

        // NO_MORE_DINNER: 구성요소 커스터마이징 단계로
        List<OrderItemDto> orderItems = context.getOrderItems();
        StringBuilder itemList = new StringBuilder();

        for (int i = 0; i < orderItems.size(); i++) {
            OrderItemDto item = orderItems.get(i);
            itemList.append(String.format("%d. %s", i + 1, item.getDinnerName()));
            if (item.getServingStyleName() != null) {
                itemList.append(" (").append(item.getServingStyleName()).append(")");
            }
            if (item.getItemIndex() > 0) {
                itemList.append(" #").append(item.getItemIndex());
            }
            if (item.getExcludedItems() != null && !item.getExcludedItems().isEmpty()) {
                itemList.append(" [").append(String.join(", ", item.getExcludedItems())).append(" 제외]");
            }
            itemList.append("\n");
        }

        String message = "디너 주문 완료! 각 디너의 구성 요소를 변경하시겠어요?\n\n"
                + "현재 주문:\n" + itemList
                + "\n변경하시려면 '1번 스테이크 빼줘' 또는 '전체 스테이크 빼줘'라고 말씀해주세요.\n"
                + "변경 없으면 '괜찮아요' 또는 '없어요'라고 해주세요!";

        return IntentResult.of(message, OrderFlowState.CUSTOMIZING_MENU);
    }
}
