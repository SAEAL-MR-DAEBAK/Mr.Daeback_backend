package com.saeal.MrDaebackService.voiceOrder.service.intent;

import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Intent 핸들러 레지스트리
 * - 모든 IntentHandler를 관리하고 적절한 핸들러를 찾아 실행
 */
@Component
@Slf4j
public class IntentHandlerRegistry {

    private final List<IntentHandler> handlers;

    public IntentHandlerRegistry(List<IntentHandler> handlers) {
        this.handlers = handlers;
        log.info("Registered {} intent handlers", handlers.size());
    }

    /**
     * Intent에 맞는 핸들러를 찾아 실행
     */
    public IntentResult process(UserIntent intent, IntentContext context) {
        for (IntentHandler handler : handlers) {
            if (handler.canHandle(intent)) {
                log.debug("Processing intent {} with handler {}", intent, handler.getClass().getSimpleName());
                return handler.handle(context);
            }
        }

        // 매칭되는 핸들러가 없는 경우 기본 응답
        log.warn("No handler found for intent: {}", intent);
        return IntentResult.of("죄송해요, 이해하지 못했어요. 다시 말씀해주세요!", OrderFlowState.IDLE);
    }

    /**
     * 특정 Intent를 처리할 수 있는 핸들러가 있는지 확인
     */
    public boolean hasHandler(UserIntent intent) {
        return handlers.stream().anyMatch(h -> h.canHandle(intent));
    }
}
