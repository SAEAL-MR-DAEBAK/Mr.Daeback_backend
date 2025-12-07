package com.saeal.MrDaebackService.cart.repository;

import com.saeal.MrDaebackService.cart.domain.Cart;
import com.saeal.MrDaebackService.cart.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    List<Cart> findByUserId(UUID userId);
    List<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);
}
