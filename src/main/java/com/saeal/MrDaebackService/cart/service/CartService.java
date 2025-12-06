package com.saeal.MrDaebackService.cart.service;

import com.saeal.MrDaebackService.cart.domain.Cart;
import com.saeal.MrDaebackService.cart.enums.CartStatus;
import com.saeal.MrDaebackService.cart.dto.request.CreateCartRequest;
import com.saeal.MrDaebackService.cart.dto.response.CartResponseDto;
import com.saeal.MrDaebackService.cart.repository.CartRepository;
import com.saeal.MrDaebackService.order.dto.response.OrderResponseDto;
import com.saeal.MrDaebackService.order.service.OrderService;
import com.saeal.MrDaebackService.order.domain.Order;
import com.saeal.MrDaebackService.product.domain.Product;
import com.saeal.MrDaebackService.product.repository.ProductRepository;
import com.saeal.MrDaebackService.user.domain.User;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import com.saeal.MrDaebackService.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

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

        cart.setSubtotal(subtotal);
        cart.setGrandTotal(subtotal.add(cart.getDeliveryFee()).subtract(cart.getDiscountAmount()));

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
        return OrderResponseDto.from(savedOrder);
    }
}
