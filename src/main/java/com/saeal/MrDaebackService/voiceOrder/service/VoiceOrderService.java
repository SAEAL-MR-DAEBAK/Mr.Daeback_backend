package com.saeal.MrDaebackService.voiceOrder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saeal.MrDaebackService.cart.dto.request.CreateCartRequest;
import com.saeal.MrDaebackService.cart.dto.response.CartResponseDto;
import com.saeal.MrDaebackService.cart.enums.DeliveryMethod;
import com.saeal.MrDaebackService.cart.service.CartService;
import com.saeal.MrDaebackService.order.dto.response.OrderResponseDto;
import com.saeal.MrDaebackService.product.dto.request.CreateProductRequest;
import com.saeal.MrDaebackService.product.dto.request.CreateAdditionalMenuProductRequest;
import com.saeal.MrDaebackService.product.dto.response.ProductResponseDto;
import com.saeal.MrDaebackService.product.service.ProductService;
import com.saeal.MrDaebackService.menuItems.domain.MenuItems;
import com.saeal.MrDaebackService.menuItems.repository.MenuItemsRepository;
import com.saeal.MrDaebackService.user.domain.User;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import com.saeal.MrDaebackService.voiceOrder.dto.LlmResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.ChatMessageDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.VoiceCheckoutRequest;
import com.saeal.MrDaebackService.voiceOrder.dto.response.ChatResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.VoiceCheckoutResponse;
import com.saeal.MrDaebackService.voiceOrder.enums.UiAction;
import com.saeal.MrDaebackService.voiceOrder.enums.UserIntent;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentContext;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentHandlerRegistry;
import com.saeal.MrDaebackService.voiceOrder.service.intent.IntentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saeal.MrDaebackService.product.dto.request.UpdateProductMenuItemRequest;
import com.saeal.MrDaebackService.product.dto.response.ProductMenuItemResponseDto;

import java.math.BigDecimal;
import java.util.*;

/**
 * 음성/텍스트 주문 처리 서비스 (리팩토링 버전)
 *
 * ★ 주문 흐름:
 * 1. SELECTING_ADDRESS → 배달 주소 선택
 * 2. SELECTING_MENU → 디너 메뉴 선택
 * 3. SELECTING_STYLE → 서빙 스타일 선택
 * 4. SELECTING_QUANTITY → 수량 선택
 * 5. ASKING_MORE_DINNER → 추가 디너 주문 여부 확인
 * 6. CUSTOMIZING_MENU → 구성요소 커스터마이징
 * 7. SELECTING_ADDITIONAL_MENU → 추가 메뉴
 * 8. ENTERING_MEMO → 메모/요청사항 입력
 * 9. CONFIRMING → 최종 확인
 * 10. CHECKOUT_READY → 결제 준비 완료
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceOrderService {

    private final GroqService groqService;
    private final PromptBuilder promptBuilder;
    private final IntentHandlerRegistry intentHandlerRegistry;
    private final ResponseMessageBuilder responseMessageBuilder;
    private final CartManager cartManager;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ProductService productService;
    private final CartService cartService;
    private final MenuItemsRepository menuItemsRepository;
    private final MenuMatcher menuMatcher;

    /**
     * 채팅 메시지 처리 (간소화 버전)
     * - 주소는 첫 번째 주소로 자동 선택
     * - 기념일/배달시간은 첫 메시지에서 자동 추출
     */
    public ChatResponseDto processChat(ChatRequestDto request, UUID userId) {
        // 1. 입력 데이터 추출
        String userMessage = extractUserMessage(request);
        List<Map<String, String>> history = convertHistory(request.getConversationHistory());
        List<OrderItemRequestDto> currentOrder = Optional.ofNullable(request.getCurrentOrder())
                .orElse(new ArrayList<>());
        List<String> userAddresses = getUserAddresses(userId);
        String occasionType = request.getOccasionType();
        java.time.LocalDateTime requestedDeliveryTime = request.getRequestedDeliveryTime();

        // ★ 주소 자동 선택: 첫 번째 주소를 기본으로 사용
        String selectedAddress = request.getSelectedAddress();
        if ((selectedAddress == null || selectedAddress.isEmpty()) && !userAddresses.isEmpty()) {
            selectedAddress = userAddresses.get(0);
            log.info("주소 자동 선택: {}", selectedAddress);
        }

        // 2. LLM 호출
        String systemPrompt = promptBuilder.build(currentOrder, selectedAddress, userAddresses, null);
        List<Map<String, String>> recentHistory = getRecentHistory(history, 4);
        String llmRawResponse = groqService.chat(systemPrompt, recentHistory, userMessage);

        // 3. JSON 파싱
        LlmResponseDto llmResponse = parseLlmResponse(llmRawResponse);

        // 4. Intent 처리
        ChatResponseDto response = processIntent(userMessage, llmResponse, currentOrder, selectedAddress,
                userAddresses, occasionType, requestedDeliveryTime);

        return response;
    }

    /**
     * Intent 처리 - 핸들러 레지스트리에 위임 (간소화 버전)
     * - 주소 체크 제거 (자동 선택됨)
     */
    private ChatResponseDto processIntent(String userMessage, LlmResponseDto llmResponse,
            List<OrderItemRequestDto> currentOrder,
            String selectedAddress,
            List<String> userAddresses,
            String occasionType,
            java.time.LocalDateTime requestedDeliveryTime) {

        UserIntent intent = parseIntent(llmResponse.getIntent());
        List<OrderItemDto> orderItems = cartManager.convertToOrderItemDtoList(currentOrder);

        // ★ 주소 체크 제거 - 이미 자동 선택됨

        // 진행 중인 아이템 찾기
        OrderItemDto pendingItem = findPendingItem(orderItems);
        int pendingIdx = pendingItem != null ? orderItems.indexOf(pendingItem) : -1;

        // IntentContext 생성
        IntentContext context = IntentContext.builder()
                .userMessage(userMessage)
                .llmResponse(llmResponse)
                .orderItems(orderItems)
                .selectedAddress(selectedAddress)
                .userAddresses(userAddresses)
                .pendingItem(pendingItem)
                .pendingItemIndex(pendingIdx)
                .occasionType(occasionType)
                .requestedDeliveryTime(requestedDeliveryTime)
                .build();

        // 핸들러 실행
        IntentResult result = intentHandlerRegistry.process(intent, context);

        // 결과로 응답 생성
        return buildResponse(userMessage, result, orderItems, selectedAddress, occasionType, requestedDeliveryTime);
    }

    /**
     * 응답 생성
     */
    private ChatResponseDto buildResponse(String userMessage, IntentResult result,
            List<OrderItemDto> defaultOrderItems, String defaultAddress,
            String defaultOccasionType, java.time.LocalDateTime defaultDeliveryTime) {

        List<OrderItemDto> orderItems = result.getUpdatedOrder() != null ? result.getUpdatedOrder() : defaultOrderItems;
        String selectedAddress = result.getSelectedAddress() != null ? result.getSelectedAddress() : defaultAddress;
        String occasionType = result.getOccasionType() != null ? result.getOccasionType() : defaultOccasionType;
        java.time.LocalDateTime deliveryTime = result.getRequestedDeliveryTime() != null
                ? result.getRequestedDeliveryTime() : defaultDeliveryTime;

        // 상태 요약 추가 (배달 시간, 기념일 포함)
        String finalMessage = responseMessageBuilder.appendStatusSummary(
                result.getMessage(),
                result.getNextState(),
                orderItems,
                deliveryTime,
                occasionType);

        int totalPrice = cartManager.calculateTotalPrice(orderItems);

        return ChatResponseDto.builder()
                .userMessage(userMessage)
                .assistantMessage(finalMessage)
                .flowState(result.getNextState())
                .uiAction(result.getUiAction() != null ? result.getUiAction() : UiAction.NONE)
                .currentOrder(orderItems)
                .totalPrice(totalPrice)
                .selectedAddress(selectedAddress)
                .memo(result.getMemo())
                .occasionType(occasionType)
                .requestedDeliveryTime(deliveryTime)
                .build();
    }


    // ============================================================
    // 헬퍼 메서드
    // ============================================================

    private String extractUserMessage(ChatRequestDto request) {
        if (request.getAudioBase64() != null && !request.getAudioBase64().isEmpty()) {
            byte[] audioData = Base64.getDecoder().decode(request.getAudioBase64());
            return groqService.transcribe(audioData, request.getAudioFormat());
        }
        return request.getMessage();
    }

    private List<Map<String, String>> convertHistory(List<ChatMessageDto> history) {
        List<Map<String, String>> result = new ArrayList<>();
        if (history != null) {
            for (ChatMessageDto msg : history) {
                result.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }
        return result;
    }

    private List<Map<String, String>> getRecentHistory(List<Map<String, String>> history, int maxTurns) {
        if (history.size() > maxTurns) {
            return history.subList(history.size() - maxTurns, history.size());
        }
        return history;
    }

    private List<String> getUserAddresses(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getAddresses() != null && !user.getAddresses().isEmpty()) {
                return new ArrayList<>(user.getAddresses());
            }
        } catch (Exception e) {
            log.warn("사용자 주소 조회 실패: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private LlmResponseDto parseLlmResponse(String rawResponse) {
        try {
            String jsonContent = rawResponse.trim();
            log.debug("LLM Raw Response: {}", jsonContent);

            // ```json ... ``` 블록 제거
            if (jsonContent.contains("```json")) {
                int start = jsonContent.indexOf("```json") + 7;
                int end = jsonContent.indexOf("```", start);
                if (end > start) {
                    jsonContent = jsonContent.substring(start, end).trim();
                }
            } else if (jsonContent.contains("```")) {
                int start = jsonContent.indexOf("```") + 3;
                int end = jsonContent.indexOf("```", start);
                if (end > start) {
                    jsonContent = jsonContent.substring(start, end).trim();
                }
            }

            // ★ JSON 객체만 추출 (다른 텍스트 완전 제거)
            int jsonStart = jsonContent.indexOf("{");
            int jsonEnd = findMatchingBrace(jsonContent, jsonStart);
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonContent = jsonContent.substring(jsonStart, jsonEnd + 1);
            }

            log.debug("Extracted JSON: {}", jsonContent);
            return objectMapper.readValue(jsonContent, LlmResponseDto.class);
        } catch (JsonProcessingException e) {
            log.warn("LLM 응답 JSON 파싱 실패: {} - Raw: {}", e.getMessage(), rawResponse);
            LlmResponseDto fallback = new LlmResponseDto();
            fallback.setIntent("ASK_MENU_INFO");
            fallback.setMessage("죄송해요, 다시 말씀해주세요!");
            return fallback;
        }
    }

    /**
     * 매칭되는 닫는 중괄호 찾기 (중첩된 JSON 처리)
     */
    private int findMatchingBrace(String content, int openIndex) {
        if (openIndex < 0 || openIndex >= content.length()) return -1;

        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = openIndex; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return content.lastIndexOf("}");
    }

    private UserIntent parseIntent(String intentStr) {
        if (intentStr == null)
            return UserIntent.UNKNOWN;
        try {
            return UserIntent.valueOf(intentStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserIntent.UNKNOWN;
        }
    }

    private OrderItemDto findPendingItem(List<OrderItemDto> orderItems) {
        // ★ 추가 메뉴(dinnerId == null)는 스타일/수량이 필요 없으므로 제외
        // 수량이 0인 디너 아이템 (완전히 미완성)
        for (OrderItemDto item : orderItems) {
            if (item.getDinnerId() != null && item.getQuantity() == 0) {
                return item;
            }
        }
        // 스타일이 선택되지 않은 디너 아이템
        for (OrderItemDto item : orderItems) {
            if (item.getDinnerId() != null && item.getServingStyleId() == null) {
                return item;
            }
        }
        return null;
    }

    // ============================================================
    // Checkout 처리
    // ============================================================

    /**
     * 음성 주문 결제 처리
     *
     * ★ GUI와 동일한 가격 계산 로직:
     * 1. Product 생성 (totalPrice = basePrice + styleExtraPrice)
     * 2. ProductMenuItem 수량 업데이트 (커스터마이징 반영)
     * 3. unitPrice 계산 (basePrice + 메뉴차액)
     * 4. Cart 생성 (unitPrice 포함)
     * 5. Order 생성
     */
    @Transactional
    public VoiceCheckoutResponse checkout(VoiceCheckoutRequest request, UUID userId) {
        try {
            if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
                return VoiceCheckoutResponse.failure("주문할 상품이 없습니다.");
            }

            // 1. Product 생성 및 커스터마이징 반영
            List<ProductWithPrice> productsWithPrices = createProductsWithCustomization(request);

            // 2. 추가 메뉴 Product 생성
            List<ProductWithPrice> additionalMenuProducts = createAdditionalMenuProducts(request);
            productsWithPrices.addAll(additionalMenuProducts);

            if (productsWithPrices.isEmpty()) {
                return VoiceCheckoutResponse.failure("유효한 주문 상품이 없습니다.");
            }

            // 3. Cart 생성 (커스터마이징 반영된 unitPrice 포함)
            CartResponseDto cart = createCartWithPrices(request, productsWithPrices);

            // 4. Checkout
            OrderResponseDto order = cartService.checkout(UUID.fromString(cart.getId()));

            return VoiceCheckoutResponse.success(
                    order.getId().toString(),
                    order.getOrderNumber(),
                    order.getGrandTotal());
        } catch (Exception e) {
            log.error("Voice checkout failed", e);
            return VoiceCheckoutResponse.failure(e.getMessage());
        }
    }

    /**
     * Product 생성 및 커스터마이징 반영
     * - ProductMenuItem 수량 업데이트
     * - 커스터마이징 반영된 unitPrice 계산
     */
    private List<ProductWithPrice> createProductsWithCustomization(VoiceCheckoutRequest request) {
        List<ProductWithPrice> result = new ArrayList<>();

        for (VoiceCheckoutRequest.OrderItemRequest item : request.getOrderItems()) {
            if (item.getDinnerId() == null || item.getQuantity() <= 0) {
                log.warn("[Checkout] Skipping item - dinnerId: {}, quantity: {}", item.getDinnerId(), item.getQuantity());
                continue;
            }

            // ★ servingStyleId가 없으면 기본 스타일(Grand Style) 사용
            String servingStyleId = item.getServingStyleId();
            if (servingStyleId == null || servingStyleId.isEmpty()) {
                var defaultStyle = menuMatcher.findStyleByName("Grand Style");
                if (defaultStyle.isPresent()) {
                    servingStyleId = defaultStyle.get().getId().toString();
                    log.info("[Checkout] Using default style: Grand Style for {}", item.getDinnerName());
                } else {
                    log.warn("[Checkout] No default style found, skipping item: {}", item.getDinnerName());
                    continue;
                }
            }

            // 1. Product 생성
            CreateProductRequest productRequest = CreateProductRequest.builder()
                    .dinnerId(item.getDinnerId())
                    .servingStyleId(servingStyleId)
                    .quantity(1) // 각 Product는 1개 단위
                    .memo(request.getMemo())
                    .address(request.getDeliveryAddress())
                    .build();

            ProductResponseDto product = productService.createProduct(productRequest);
            UUID productId = UUID.fromString(product.getId().toString());

            // 2. 커스터마이징 반영 (ProductMenuItem 수량 업데이트)
            BigDecimal menuItemDiff = BigDecimal.ZERO;
            boolean hasComponents = item.getComponents() != null && !item.getComponents().isEmpty();
            boolean hasExcludedItems = item.getExcludedItems() != null && !item.getExcludedItems().isEmpty();

            log.info("[Checkout] Item: {}, components: {}, excludedItems: {}",
                    item.getDinnerName(), item.getComponents(), item.getExcludedItems());

            if (hasComponents || hasExcludedItems) {
                menuItemDiff = applyCustomization(productId, product, item.getComponents(), item.getExcludedItems());
                log.info("[Checkout] Applied customization, menuItemDiff: {}", menuItemDiff);
            }

            // 3. unitPrice 계산: basePrice + styleExtraPrice + menuItemDiff
            BigDecimal baseUnitPrice = product.getTotalPrice(); // dinner.basePrice + style.extraPrice
            BigDecimal finalUnitPrice = baseUnitPrice.add(menuItemDiff);

            // quantity 개수만큼 반복 추가
            for (int i = 0; i < item.getQuantity(); i++) {
                if (i == 0) {
                    // 첫 번째는 이미 생성된 Product 사용
                    result.add(new ProductWithPrice(productId.toString(), finalUnitPrice));
                } else {
                    // 두 번째부터는 새로운 Product 생성
                    ProductResponseDto additionalProduct = productService.createProduct(productRequest);
                    UUID additionalProductId = UUID.fromString(additionalProduct.getId().toString());

                    // 동일한 커스터마이징 적용
                    if (hasComponents || hasExcludedItems) {
                        applyCustomization(additionalProductId, additionalProduct, item.getComponents(),
                                item.getExcludedItems());
                    }

                    result.add(new ProductWithPrice(additionalProductId.toString(), finalUnitPrice));
                }
            }
        }

        return result;
    }

    /**
     * 커스터마이징 적용 (ProductMenuItem 수량 업데이트 + 새로운 구성요소 추가)
     *
     * @return 메뉴 아이템 차액 (양수: 추가, 음수: 감소)
     */
    private BigDecimal applyCustomization(UUID productId, ProductResponseDto product,
            Map<String, Integer> components,
            List<String> excludedItems) {
        BigDecimal totalDiff = BigDecimal.ZERO;

        // ProductMenuItem 목록 가져오기
        List<ProductMenuItemResponseDto> productMenuItems = product.getProductMenuItems();

        // 처리된 component 키 추적 (새로운 구성요소 추가 시 사용)
        Set<String> processedComponentKeys = new HashSet<>();

        // 1. 기존 ProductMenuItem 처리 (수량 변경, 제외)
        if (productMenuItems != null && !productMenuItems.isEmpty()) {
            for (ProductMenuItemResponseDto pmi : productMenuItems) {
                String menuItemName = pmi.getMenuItemName();
                UUID menuItemId = UUID.fromString(pmi.getMenuItemId());
                int defaultQty = pmi.getQuantity();
                BigDecimal unitPrice = pmi.getUnitPrice();

                // 제외된 아이템 처리
                if (excludedItems != null && isMenuItemExcluded(menuItemName, excludedItems)) {
                    // 수량을 0으로 설정
                    UpdateProductMenuItemRequest updateRequest = new UpdateProductMenuItemRequest();
                    updateRequest.setQuantity(0);
                    productService.updateProductMenuItem(productId, menuItemId, updateRequest);

                    // 차액 계산 (기본 수량 × 단가 만큼 감소)
                    totalDiff = totalDiff.subtract(unitPrice.multiply(BigDecimal.valueOf(defaultQty)));
                    continue;
                }

                // components에서 현재 수량 찾기
                String matchedKey = findComponentKey(menuItemName, components);
                if (matchedKey != null) {
                    processedComponentKeys.add(matchedKey);
                    int currentQty = components.get(matchedKey);

                    if (currentQty != defaultQty) {
                        // 수량 업데이트
                        UpdateProductMenuItemRequest updateRequest = new UpdateProductMenuItemRequest();
                        updateRequest.setQuantity(currentQty);
                        productService.updateProductMenuItem(productId, menuItemId, updateRequest);

                        // 차액 계산: (현재수량 - 기본수량) × 단가
                        int qtyDiff = currentQty - defaultQty;
                        BigDecimal priceDiff = unitPrice.multiply(BigDecimal.valueOf(qtyDiff));
                        totalDiff = totalDiff.add(priceDiff);
                    }
                }
            }
        }

        // 2. 새로운 구성요소 추가 (Product에 없는 MenuItem)
        if (components != null && !components.isEmpty()) {
            for (Map.Entry<String, Integer> entry : components.entrySet()) {
                String componentKey = entry.getKey();
                int quantity = entry.getValue();

                // 이미 처리된 경우 건너뛰기
                if (processedComponentKeys.contains(componentKey)) {
                    continue;
                }

                // MenuItem 찾기
                MenuItems menuItem = findMenuItemByName(componentKey);
                if (menuItem == null) {
                    log.warn("MenuItem not found for component: {}", componentKey);
                    continue;
                }

                // Product에 새로운 MenuItem 추가
                try {
                    productService.addProductMenuItem(productId, menuItem.getId(), quantity);

                    // 차액 계산: 새로운 구성요소 전체 가격 추가
                    BigDecimal unitPrice = menuItem.getUnitPrice();
                    BigDecimal priceDiff = unitPrice.multiply(BigDecimal.valueOf(quantity));
                    totalDiff = totalDiff.add(priceDiff);

                    log.info("Added new component to product: {} x{}, priceDiff: {}",
                            componentKey, quantity, priceDiff);
                } catch (Exception e) {
                    log.error("Failed to add component {} to product: {}", componentKey, e.getMessage());
                }
            }
        }

        return totalDiff;
    }

    /**
     * components에서 해당 메뉴 아이템의 키 찾기 (부분 매칭)
     */
    private String findComponentKey(String menuItemName, Map<String, Integer> components) {
        if (menuItemName == null || components == null)
            return null;
        String lowerName = menuItemName.toLowerCase();

        for (String key : components.keySet()) {
            String lowerKey = key.toLowerCase();
            if (lowerName.contains(lowerKey) || lowerKey.contains(lowerName)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 메뉴 아이템이 제외 목록에 있는지 확인 (부분 매칭)
     */
    private boolean isMenuItemExcluded(String menuItemName, List<String> excludedItems) {
        if (menuItemName == null || excludedItems == null)
            return false;
        String lowerName = menuItemName.toLowerCase();
        return excludedItems.stream()
                .anyMatch(ex -> lowerName.contains(ex.toLowerCase()) || ex.toLowerCase().contains(lowerName));
    }

    /**
     * Cart 생성 (커스터마이징 반영된 unitPrice 포함)
     */
    private CartResponseDto createCartWithPrices(VoiceCheckoutRequest request,
            List<ProductWithPrice> productsWithPrices) {
        List<CreateCartRequest.CartItemRequest> cartItems = new ArrayList<>();

        for (ProductWithPrice pwp : productsWithPrices) {
            CreateCartRequest.CartItemRequest cartItem = new CreateCartRequest.CartItemRequest();
            cartItem.setProductId(pwp.productId);
            cartItem.setQuantity(1);
            cartItem.setUnitPrice(pwp.unitPrice); // ★ 커스터마이징 반영된 가격
            cartItems.add(cartItem);
        }

        CreateCartRequest cartRequest = new CreateCartRequest();
        cartRequest.setItems(cartItems);
        cartRequest.setDeliveryAddress(request.getDeliveryAddress());
        cartRequest.setDeliveryMethod(DeliveryMethod.Delivery);
        cartRequest.setMemo(request.getMemo());
        cartRequest.setRequestedDeliveryTime(request.getRequestedDeliveryTime());
        cartRequest.setOccasionType(request.getOccasionType());

        return cartService.createCart(cartRequest);
    }

    /**
     * 추가 메뉴 Product 생성
     */
    private List<ProductWithPrice> createAdditionalMenuProducts(VoiceCheckoutRequest request) {
        List<ProductWithPrice> result = new ArrayList<>();

        if (request.getAdditionalMenuItems() == null || request.getAdditionalMenuItems().isEmpty()) {
            return result;
        }

        for (VoiceCheckoutRequest.AdditionalMenuItem item : request.getAdditionalMenuItems()) {
            String menuItemName = item.getMenuItemName();
            int quantity = item.getQuantity();

            if (menuItemName == null || menuItemName.isEmpty() || quantity <= 0) {
                continue;
            }

            // 이름으로 MenuItem 찾기
            MenuItems menuItem = findMenuItemByName(menuItemName);
            if (menuItem == null) {
                continue;
            }

            // 추가 메뉴 Product 생성
            CreateAdditionalMenuProductRequest productRequest = new CreateAdditionalMenuProductRequest();
            productRequest.setMenuItemId(menuItem.getId().toString());
            productRequest.setQuantity(quantity);
            productRequest.setMemo(request.getMemo());
            productRequest.setAddress(request.getDeliveryAddress());

            ProductResponseDto product = productService.createAdditionalMenuProduct(productRequest);
            BigDecimal unitPrice = menuItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity));

            result.add(new ProductWithPrice(product.getId(), unitPrice));
        }

        return result;
    }

    /**
     * 이름으로 MenuItem 찾기 (다양한 매칭 시도)
     */
    private MenuItems findMenuItemByName(String name) {
        if (name == null)
            return null;

        // 1. 정확한 이름 매칭 (대소문자 무시)
        List<MenuItems> exactList = menuItemsRepository.findByNameIgnoreCase(name);
        if (!exactList.isEmpty()) {
            return exactList.get(0);
        }

        // 2. 부분 매칭 (대소문자 무시) - 목록에서 첫 번째 선택
        List<MenuItems> partialList = menuItemsRepository.findByNameContainingIgnoreCase(name);
        if (!partialList.isEmpty()) {
            return partialList.get(0);
        }

        // 3. 한글/영어 변환 매칭
        String[] korToEng = {
                "스테이크", "steak",
                "샐러드", "salad",
                "와인", "wine",
                "샴페인", "champagne",
                "빵", "bread",
                "바게트빵", "baguette",
                "커피", "coffee",
                "에그 스크램블", "egg scramble"
        };

        String lowerName = name.toLowerCase();
        for (int i = 0; i < korToEng.length; i += 2) {
            String kor = korToEng[i].toLowerCase();
            String eng = korToEng[i + 1].toLowerCase();

            if (lowerName.contains(kor)) {
                List<MenuItems> byEng = menuItemsRepository.findByNameContainingIgnoreCase(eng);
                if (!byEng.isEmpty())
                    return byEng.get(0);
            }
            if (lowerName.contains(eng)) {
                List<MenuItems> byKor = menuItemsRepository.findByNameContainingIgnoreCase(kor);
                if (!byKor.isEmpty())
                    return byKor.get(0);
            }
        }

        return null;
    }

    /**
     * Product ID와 계산된 unitPrice를 담는 내부 클래스
     */
    private static class ProductWithPrice {
        final String productId;
        final BigDecimal unitPrice;

        ProductWithPrice(String productId, BigDecimal unitPrice) {
            this.productId = productId;
            this.unitPrice = unitPrice;
        }
    }
}
