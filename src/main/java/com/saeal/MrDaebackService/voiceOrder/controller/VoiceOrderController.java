package com.saeal.MrDaebackService.voiceOrder.controller;

import com.saeal.MrDaebackService.security.JwtUserDetails;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.ChatResponseDto;
import com.saeal.MrDaebackService.voiceOrder.service.VoiceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice-order")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Voice Order", description = "음성 주문 API")
public class VoiceOrderController {

    private final VoiceOrderService voiceOrderService;

    @PostMapping("/chat")
    @Operation(summary = "음성/텍스트 채팅", description = "음성 또는 텍스트로 주문 대화를 진행합니다 (Stateless)")
    public ResponseEntity<ChatResponseDto> chat(
            @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal JwtUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        ChatResponseDto response = voiceOrderService.processChat(
                request,
                userDetails.getId()
        );

        return ResponseEntity.ok(response);
    }
}
