package com.saeal.MrDaebackService.product.repository;

import com.saeal.MrDaebackService.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
}
