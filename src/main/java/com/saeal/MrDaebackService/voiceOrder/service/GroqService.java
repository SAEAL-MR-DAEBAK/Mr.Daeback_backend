package com.saeal.MrDaebackService.voiceOrder.service;

import com.saeal.MrDaebackService.voiceOrder.config.GroqConfig;
import com.saeal.MrDaebackService.voiceOrder.dto.GroqChatResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.GroqSttResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private final GroqConfig groqConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 음성을 텍스트로 변환 (STT)
     */
    public String transcribe(byte[] audioData, String format) {
        String url = groqConfig.getBaseUrl() + "/audio/transcriptions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(groqConfig.getApiKey());

        // 파일 이름 결정
        String fileName = "audio." + (format != null ? format : "webm");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioData) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        body.add("model", groqConfig.getSttModel());
        body.add("language", "ko");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<GroqSttResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    GroqSttResponseDto.class
            );

            if (response.getBody() != null) {
                return response.getBody().getText();
            }
            return "";
        } catch (Exception e) {
            log.error("[STT] 변환 실패: {}", e.getMessage());
            throw new RuntimeException("음성 인식 실패", e);
        }
    }

    /**
     * LLM 채팅 완성
     */
    public String chat(String systemPrompt, List<Map<String, String>> conversationHistory, String userMessage) {
        String url = groqConfig.getBaseUrl() + "/chat/completions";
        String apiKey = groqConfig.getApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 메시지 구성
        List<Map<String, String>> messages = new ArrayList<>();

        // 시스템 프롬프트
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // 대화 히스토리
        if (conversationHistory != null) {
            messages.addAll(conversationHistory);
        }

        // 현재 사용자 메시지
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqConfig.getLlmModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1024);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<GroqChatResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    GroqChatResponseDto.class
            );

            if (response.getBody() != null) {
                return response.getBody().getContent();
            }
            return "";
        } catch (Exception e) {
            log.error("[LLM] 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 응답 생성 실패", e);
        }
    }
}
