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
 * ORDER_MENU Intent 처리
 */
@Component
public class OrderMenuHandler extends AbstractIntentHandler {

    public OrderMenuHandler(MenuMatcher menuMatcher, CartManager cartManager) {
        super(menuMatcher, cartManager);
    }

    @Override
    public boolean canHandle(UserIntent intent) {
        return intent == UserIntent.ORDER_MENU;
    }

    @Override
    public IntentResult handle(IntentContext context) {
        LlmResponseDto.ExtractedEntities entities = getEntities(context);
        List<OrderItemDto> orderItems = context.getOrderItems();
        OrderItemDto pendingItem = context.getPendingItem();
        int pendingIdx = context.getPendingItemIndex();
        String userMessage = context.getUserMessage();

        // 주소 체크
        if (context.getSelectedAddress() == null || context.getSelectedAddress().isEmpty()) {
            if (!context.getUserAddresses().isEmpty()) {
                return IntentResult.of(
                        "먼저 배달 주소를 선택해주세요!\n" + formatAddressList(context.getUserAddresses()),
                        OrderFlowState.SELECTING_ADDRESS
                );
            } else {
                return IntentResult.of(
                        "저장된 배달 주소가 없어요. 마이페이지에서 주소를 먼저 추가해주세요!",
                        OrderFlowState.IDLE
                );
            }
        }

        // 진행 중인 아이템이 있고, 수량만 말한 경우
        if (pendingItem != null && entities != null && entities.getQuantity() != null && entities.getMenuName() == null) {
            if (pendingItem.getServingStyleId() != null) {
                OrderItemDto updated = cartManager.setQuantity(pendingItem, entities.getQuantity());
                orderItems.set(pendingIdx, updated);
                return IntentResult.builder()
                        .message(updated.getDinnerName() + " " + entities.getQuantity() + "개 주문 완료! 다른 디너 메뉴도 추가하시겠어요?")
                        .nextState(OrderFlowState.ASKING_MORE_DINNER)
                        .uiAction(UiAction.UPDATE_ORDER_LIST)
                        .updatedOrder(orderItems)
                        .build();
            } else {
                return IntentResult.of("먼저 스타일을 선택해주세요!", OrderFlowState.SELECTING_STYLE);
            }
        }

        // 진행 중인 아이템이 있고, 스타일만 말한 경우
        if (pendingItem != null && entities != null && entities.getStyleName() != null && entities.getMenuName() == null) {
            return handleStyleSelection(context, pendingItem, pendingIdx, entities.getStyleName());
        }

        // 새 메뉴 주문
        if (entities != null && entities.getMenuName() != null) {
            return handleNewMenuOrder(context, entities, userMessage);
        }

        return IntentResult.of("어떤 메뉴를 주문하시겠어요?", OrderFlowState.SELECTING_MENU);
    }

    private IntentResult handleStyleSelection(IntentContext context, OrderItemDto pendingItem, int pendingIdx,
                                              String styleName) {
        List<OrderItemDto> orderItems = context.getOrderItems();

        // 스타일 제한 검사
        if (!menuMatcher.isStyleAvailableForDinner(pendingItem.getDinnerName(), styleName)) {
            return IntentResult.of(
                    pendingItem.getDinnerName() + "는 Simple Style을 제공하지 않아요. Grand Style 또는 Deluxe Style 중에서 선택해주세요!",
                    OrderFlowState.SELECTING_STYLE
            );
        }

        var styleOpt = menuMatcher.findStyleByName(styleName);
        if (styleOpt.isPresent()) {
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
                        .message(updated.getDinnerName() + " " + styleOpt.get().getStyleName() + " 적용 완료! 다른 디너 메뉴도 추가하시겠어요?")
                        .nextState(OrderFlowState.ASKING_MORE_DINNER)
                        .uiAction(UiAction.UPDATE_ORDER_LIST)
                        .updatedOrder(orderItems)
                        .build();
            }
        }

        return IntentResult.of("죄송해요, '" + styleName + "' 스타일을 찾을 수 없어요.", OrderFlowState.SELECTING_STYLE);
    }

    private IntentResult handleNewMenuOrder(IntentContext context, LlmResponseDto.ExtractedEntities entities,
                                            String userMessage) {
        List<OrderItemDto> orderItems = context.getOrderItems();
        OrderItemDto pendingItem = context.getPendingItem();

        // "하나 더", "추가" 패턴 감지
        boolean isAddMorePattern = userMessage.contains("하나 더") || userMessage.contains("하나더")
                || userMessage.contains("한 개 더") || userMessage.contains("한개 더")
                || (userMessage.contains("추가") && !userMessage.contains("추가 메뉴"));

        // 진행 중인 아이템이 있으면 먼저 완성하도록 안내 (새 메뉴 추가 요청이 아닌 경우)
        if (pendingItem != null && !isAddMorePattern) {
            if (pendingItem.getServingStyleId() == null) {
                return IntentResult.of(pendingItem.getDinnerName() + "의 스타일을 먼저 선택해주세요!", OrderFlowState.SELECTING_STYLE);
            } else {
                return IntentResult.of(pendingItem.getDinnerName() + "의 수량을 먼저 선택해주세요!", OrderFlowState.SELECTING_QUANTITY);
            }
        }

        var dinnerOpt = menuMatcher.findDinnerByName(entities.getMenuName());
        if (dinnerOpt.isEmpty()) {
            return IntentResult.of("죄송해요, '" + entities.getMenuName() + "' 메뉴를 찾을 수 없어요.", OrderFlowState.SELECTING_MENU);
        }

        // 임시 아이템 생성
        OrderItemDto newItem = cartManager.addMenuWithoutQuantity(dinnerOpt.get());

        // 스타일도 함께 지정된 경우
        if (entities.getStyleName() != null) {
            if (!menuMatcher.isStyleAvailableForDinner(newItem.getDinnerName(), entities.getStyleName())) {
                orderItems.add(newItem);
                return IntentResult.builder()
                        .message(newItem.getDinnerName() + "는 Simple Style을 제공하지 않아요. Grand Style 또는 Deluxe Style 중에서 선택해주세요!")
                        .nextState(OrderFlowState.SELECTING_STYLE)
                        .uiAction(UiAction.UPDATE_ORDER_LIST)
                        .updatedOrder(orderItems)
                        .build();
            }

            var styleOpt = menuMatcher.findStyleByName(entities.getStyleName());
            if (styleOpt.isPresent()) {
                newItem = cartManager.applyStyleToItem(newItem, styleOpt.get());
            }
        }

        // 수량도 함께 지정된 경우 (단, "하나 더" 패턴이면 수량 무시)
        if (entities.getQuantity() != null && entities.getQuantity() > 0 && !isAddMorePattern) {
            newItem = cartManager.setQuantity(newItem, entities.getQuantity());
        }

        orderItems.add(newItem);

        // 다음 상태 결정
        if (newItem.getServingStyleId() == null) {
            String availableStyles = menuMatcher.getAvailableStylesForDinner(dinnerOpt.get().getDinnerName());
            return IntentResult.builder()
                    .message(dinnerOpt.get().getDinnerName() + " 추가할게요! 스타일을 선택해주세요. (" + availableStyles + ")")
                    .nextState(OrderFlowState.SELECTING_STYLE)
                    .uiAction(UiAction.UPDATE_ORDER_LIST)
                    .updatedOrder(orderItems)
                    .build();
        } else if (newItem.getQuantity() == 0) {
            return IntentResult.builder()
                    .message(newItem.getDinnerName() + " " + newItem.getServingStyleName() + "이요! " + buildQuantityQuestion(newItem.getDinnerName()))
                    .nextState(OrderFlowState.SELECTING_QUANTITY)
                    .uiAction(UiAction.UPDATE_ORDER_LIST)
                    .updatedOrder(orderItems)
                    .build();
        } else {
            return IntentResult.builder()
                    .message(newItem.getDinnerName() + " " + newItem.getServingStyleName() + " " + newItem.getQuantity() + "개 주문 완료! 다른 디너 메뉴도 추가하시겠어요?")
                    .nextState(OrderFlowState.ASKING_MORE_DINNER)
                    .uiAction(UiAction.UPDATE_ORDER_LIST)
                    .updatedOrder(orderItems)
                    .build();
        }
    }
}
