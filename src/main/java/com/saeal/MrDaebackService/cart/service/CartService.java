package com.saeal.MrDaebackService.cart.service;

import com.saeal.MrDaebackService.cart.domain.Cart;
import com.saeal.MrDaebackService.cart.enums.CartStatus;
import com.saeal.MrDaebackService.cart.dto.request.CreateCartRequest;
import com.saeal.MrDaebackService.cart.dto.response.CartResponseDto;
import com.saeal.MrDaebackService.cart.dto.response.DiscountInfoResponseDto;
import com.saeal.MrDaebackService.cart.dto.response.DiscountSummaryResponseDto;
import com.saeal.MrDaebackService.cart.repository.CartRepository;
import com.saeal.MrDaebackService.order.dto.response.OrderResponseDto;
import com.saeal.MrDaebackService.order.service.OrderService;
import com.saeal.MrDaebackService.order.domain.Order;
import com.saeal.MrDaebackService.product.domain.Product;
import com.saeal.MrDaebackService.product.repository.ProductRepository;
import com.saeal.MrDaebackService.user.domain.User;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import com.saeal.MrDaebackService.user.service.UserService;
import com.saeal.MrDaebackService.user.enums.LoyaltyLevel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final BigDecimal SILVER_SPEND_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal GOLD_SPEND_THRESHOLD = new BigDecimal("1000000");
    private static final long SILVER_ORDER_THRESHOLD = 5L;
    private static final long GOLD_ORDER_THRESHOLD = 10L;

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final OrderService orderService;

    /**
     * 장바구니 생성
     * - GUI: CheckoutStep에서 결제 전 호출
     * - LLM: VoiceOrderService CONFIRM_ORDER에서 호출
     *
     * unitPrice: 프론트엔드에서 계산한 가격 사용 (없으면 Product.totalPrice 사용)
     */
    @Transactional
    public CartResponseDto createCart(CreateCartRequest request) {
        UUID userId = userService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Cart cart = Cart.builder()
                .user(user)
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryMethod(request.getDeliveryMethod())
                .memo(request.getMemo())
                .expiresAt(request.getExpiresAt())
                .discountAmount(BigDecimal.ZERO)
                .deliveryFee(BigDecimal.ZERO)
                .status(CartStatus.OPEN)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateCartRequest.CartItemRequest itemRequest : request.getItems()) {
            UUID productId = UUID.fromString(itemRequest.getProductId());
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

            Integer quantity = itemRequest.getQuantity();
            // 프론트엔드에서 전달한 unitPrice 사용 (없으면 Product.totalPrice 사용)
            BigDecimal unitPrice = itemRequest.getUnitPrice() != null
                    ? itemRequest.getUnitPrice()
                    : product.getTotalPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            subtotal = subtotal.add(lineTotal);

            cart.getProducts().add(product);
            cart.getProductQuantities().put(product.getId(), quantity);
            cart.getProductUnitPrices().put(product.getId(), unitPrice);
        }

        BigDecimal discountAmount = calculateDiscount(subtotal, user.getLoyaltyLevel());

        cart.setSubtotal(subtotal);
        cart.setDiscountAmount(discountAmount);
        cart.setGrandTotal(subtotal.add(cart.getDeliveryFee()).subtract(discountAmount));

        Cart saved = cartRepository.save(cart);
        return CartResponseDto.from(saved);
    }

    /**
     * 장바구니 결제 (Order 생성)
     * - GUI: CheckoutStep에서 결제 버튼 클릭 시 호출
     * - LLM: VoiceOrderService CONFIRM_ORDER에서 호출
     */
    @Transactional
    public OrderResponseDto checkout(UUID cartId) {
        UUID userId = userService.getCurrentUserId();
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        if (!cart.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Cart does not belong to current user");
        }
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new IllegalStateException("Cart is not open");
        }

        Order order = orderService.createOrderFromCart(cart);
        cart.setStatus(CartStatus.CHECKED_OUT);
        Order savedOrder = orderService.saveOrder(order);

        User user = cart.getUser();
        BigDecimal currentTotalSpent = user.getTotalSpent() != null
                ? user.getTotalSpent()
                : BigDecimal.ZERO;
        BigDecimal orderTotal = savedOrder.getGrandTotal() != null
                ? savedOrder.getGrandTotal()
                : BigDecimal.ZERO;
        user.setTotalSpent(currentTotalSpent.add(orderTotal));

        Long currentVisitCount = user.getVisitCount() != null ? user.getVisitCount() : 0L;
        user.setVisitCount(currentVisitCount + 1);
        updateLoyaltyLevel(user);

        return OrderResponseDto.from(savedOrder);
    }

    @Transactional
    public DiscountInfoResponseDto getDiscountInfoForCurrentUser() {
        UUID userId = userService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        LoyaltyLevel loyaltyLevel = user.getLoyaltyLevel() == null
                ? LoyaltyLevel.BRONZE
                : user.getLoyaltyLevel();

        BigDecimal rate = getDiscountRate(loyaltyLevel);
        BigDecimal percent = rate.multiply(new BigDecimal("100"));

        return new DiscountInfoResponseDto(loyaltyLevel.name(), rate, percent);
    }

    @Transactional
    public DiscountSummaryResponseDto getDiscountSummary(UUID cartId) {
        UUID userId = userService.getCurrentUserId();
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        if (!cart.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Cart does not belong to current user");
        }

        BigDecimal subtotal = cart.getSubtotal() == null ? BigDecimal.ZERO : cart.getSubtotal();
        BigDecimal discountAmount = cart.getDiscountAmount() == null ? BigDecimal.ZERO : cart.getDiscountAmount();
        BigDecimal rate = BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.ZERO) > 0 && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            rate = discountAmount.divide(subtotal, 4, RoundingMode.HALF_UP);
        }

        BigDecimal grandTotal = cart.getGrandTotal() == null ? BigDecimal.ZERO : cart.getGrandTotal();

        return new DiscountSummaryResponseDto(subtotal, grandTotal, rate);
    }

    private void updateLoyaltyLevel(User user) {
        LoyaltyLevel currentLevel = user.getLoyaltyLevel() != null
                ? user.getLoyaltyLevel()
                : LoyaltyLevel.BRONZE;

        long orderCount = user.getVisitCount() != null ? user.getVisitCount() : 0L;

        boolean meetsGold = orderCount >= GOLD_ORDER_THRESHOLD
                && user.getTotalSpent().compareTo(GOLD_SPEND_THRESHOLD) >= 0;
        boolean meetsSilver = orderCount >= SILVER_ORDER_THRESHOLD
                && user.getTotalSpent().compareTo(SILVER_SPEND_THRESHOLD) >= 0;

        LoyaltyLevel targetLevel = currentLevel;
        if (meetsGold) {
            targetLevel = LoyaltyLevel.GOLD;
        } else if (meetsSilver) {
            targetLevel = LoyaltyLevel.SILVER;
        }

        if (targetLevel.ordinal() > currentLevel.ordinal()) {
            user.setLoyaltyLevel(targetLevel);
        }
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, LoyaltyLevel loyaltyLevel) {
        if (subtotal == null || loyaltyLevel == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = getDiscountRate(loyaltyLevel);

        return subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getDiscountRate(LoyaltyLevel loyaltyLevel) {
        return switch (loyaltyLevel) {
            case GOLD -> new BigDecimal("0.10");
            case SILVER -> new BigDecimal("0.05");
            default -> BigDecimal.ZERO;
        };
    }
}
