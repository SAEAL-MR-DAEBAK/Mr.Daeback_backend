package com.saeal.MrDaebackService.product.repository;

import com.saeal.MrDaebackService.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * 디너와 스타일로 Product 찾기 (최근 생성순)
     * productId가 프론트에서 전달되지 않았을 때 fallback으로 사용
     */
    @Query("SELECT p FROM Product p WHERE p.dinner.id = :dinnerId AND p.servingStyle.id = :servingStyleId ORDER BY p.createdAt DESC")
    List<Product> findByDinnerAndServingStyle(@Param("dinnerId") UUID dinnerId, @Param("servingStyleId") UUID servingStyleId);
}
