package com.saeal.MrDaebackService.voiceOrder.service.intent;

import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;

/**
 * Intent 처리 인터페이스
 * 각 Intent 유형별로 구현체를 만들어 처리 로직을 분리
 */
public interface IntentHandler {

    /**
     * 이 핸들러가 처리할 수 있는 Intent인지 확인
     */
    boolean canHandle(UserIntent intent);

    /**
     * Intent 처리 실행
     */
    IntentResult handle(IntentContext context);
}
