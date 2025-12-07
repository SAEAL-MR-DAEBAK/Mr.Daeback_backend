package com.saeal.MrDaebackService.product.service;

import com.saeal.MrDaebackService.dinner.domain.Dinner;
import com.saeal.MrDaebackService.dinner.domain.DinnerMenuItem;
import com.saeal.MrDaebackService.dinner.repository.DinnerMenuItemRepository;
import com.saeal.MrDaebackService.dinner.repository.DinnerRepository;
import com.saeal.MrDaebackService.product.domain.Product;
import com.saeal.MrDaebackService.product.domain.ProductMenuItem;
import com.saeal.MrDaebackService.product.dto.request.CreateProductRequest;
import com.saeal.MrDaebackService.product.dto.request.CreateAdditionalMenuProductRequest;
import com.saeal.MrDaebackService.product.dto.request.UpdateProductMenuItemRequest;
import com.saeal.MrDaebackService.product.enums.ProductType;
import com.saeal.MrDaebackService.product.dto.response.ProductResponseDto;
import com.saeal.MrDaebackService.product.dto.response.ProductMenuItemResponseDto;
import com.saeal.MrDaebackService.product.repository.ProductRepository;
import com.saeal.MrDaebackService.menuItems.domain.MenuItems;
import com.saeal.MrDaebackService.menuItems.repository.MenuItemsRepository;
import com.saeal.MrDaebackService.servingStyle.domain.ServingStyle;
import com.saeal.MrDaebackService.servingStyle.repository.ServingStyleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DinnerRepository dinnerRepository;
    private final ServingStyleRepository servingStyleRepository;
    private final DinnerMenuItemRepository dinnerMenuItemRepository;
    private final MenuItemsRepository menuItemsRepository;

    /**
     * Product 생성 (Dinner + Style 기반)
     * - GUI: StyleStep에서 호출
     * - LLM: VoiceOrderService에서 호출
     *
     * totalPrice = dinner.basePrice + style.extraPrice (메뉴 아이템 미포함)
     * 프론트엔드에서 커스터마이징 차이를 계산하여 최종 가격 결정
     */
    @Transactional
    public ProductResponseDto createProduct(CreateProductRequest request) {
        UUID dinnerId = UUID.fromString(request.getDinnerId());
        UUID servingStyleId = UUID.fromString(request.getServingStyleId());

        Dinner dinner = dinnerRepository.findById(dinnerId)
                .orElseThrow(() -> new IllegalArgumentException("Dinner not found: " + dinnerId));
        ServingStyle servingStyle = servingStyleRepository.findById(servingStyleId)
                .orElseThrow(() -> new IllegalArgumentException("Serving style not found: " + servingStyleId));

        // totalPrice = dinner.basePrice + style.extraPrice (메뉴 아이템 미포함)
        BigDecimal totalPrice = dinner.getBasePrice().add(servingStyle.getExtraPrice());

        String productName = request.getProductName();
        if (productName == null || productName.isBlank()) {
            productName = dinner.getDinnerName() + " - " + servingStyle.getStyleName();
        }

        Product product = Product.builder()
                .dinner(dinner)
                .servingStyle(servingStyle)
                .productName(productName)
                .totalPrice(totalPrice)
                .quantity(request.getQuantity())
                .memo(request.getMemo())
                .address(request.getAddress())
                .build();

        // 메뉴 아이템 정보 저장 (가격 계산용, totalPrice에 미포함)
        List<DinnerMenuItem> dinnerMenuItems = dinnerMenuItemRepository.findByDinnerId(dinnerId);
        for (DinnerMenuItem dinnerMenuItem : dinnerMenuItems) {
            BigDecimal menuItemUnitPrice = dinnerMenuItem.getMenuItem().getUnitPrice();
            int defaultQty = dinnerMenuItem.getDefaultQuantity();
            BigDecimal lineTotal = menuItemUnitPrice.multiply(BigDecimal.valueOf(defaultQty));

            ProductMenuItem productMenuItem = ProductMenuItem.builder()
                    .product(product)
                    .menuItem(dinnerMenuItem.getMenuItem())
                    .quantity(defaultQty)
                    .unitPrice(menuItemUnitPrice)
                    .lineTotal(lineTotal)
                    .build();
            product.getProductMenuItems().add(productMenuItem);
        }

        Product savedProduct = productRepository.save(product);
        return ProductResponseDto.from(savedProduct);
    }

    /**
     * 추가 메뉴 Product 생성
     * - GUI: CheckoutStep에서 공통 추가 메뉴용
     */
    @Transactional
    public ProductResponseDto createAdditionalMenuProduct(CreateAdditionalMenuProductRequest request) {
        UUID menuItemId = UUID.fromString(request.getMenuItemId());
        MenuItems menuItem = menuItemsRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + menuItemId));

        int quantity = request.getQuantity();
        BigDecimal unitPrice = menuItem.getUnitPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        Product product = Product.builder()
                .productType(ProductType.ADDITIONAL_MENU_PRODUCT)
                .dinner(null)
                .servingStyle(null)
                .productName("추가 메뉴: " + menuItem.getName())
                .totalPrice(totalPrice)
                .quantity(quantity)
                .memo(request.getMemo())
                .address(request.getAddress())
                .build();

        ProductMenuItem productMenuItem = ProductMenuItem.builder()
                .product(product)
                .menuItem(menuItem)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .lineTotal(totalPrice)
                .build();
        product.getProductMenuItems().add(productMenuItem);

        Product savedProduct = productRepository.save(product);
        return ProductResponseDto.from(savedProduct);
    }

    /**
     * MenuItem 수량 수정
     * - GUI: CheckoutStep에서 커스터마이징 반영 시 호출
     * - LLM: VoiceOrderService CUSTOMIZE_MENU에서 호출
     *
     * Product.totalPrice는 업데이트하지 않음 (프론트엔드 계산 신뢰)
     */
    @Transactional
    public ProductMenuItemResponseDto updateProductMenuItem(UUID productId, UUID menuItemId, UpdateProductMenuItemRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        ProductMenuItem target = product.getProductMenuItems().stream()
                .filter(pmi -> pmi.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found in product: " + menuItemId));

        int newQuantity = request.getQuantity();
        BigDecimal unitPrice = target.getUnitPrice();
        if (unitPrice == null) {
            unitPrice = target.getMenuItem().getUnitPrice();
        }
        if (unitPrice == null) {
            throw new IllegalStateException("Unit price is missing for menu item " + menuItemId);
        }

        target.setQuantity(newQuantity);
        target.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(newQuantity)));

        // Product.totalPrice는 업데이트하지 않음 (프론트엔드 계산 신뢰)

        Product saved = productRepository.save(product);

        return saved.getProductMenuItems().stream()
                .filter(pmi -> pmi.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .map(ProductMenuItemResponseDto::from)
                .orElseThrow(() -> new IllegalStateException("Updated menu item not found after save"));
    }

    /**
     * Product 메모 수정
     * - GUI: CheckoutStep에서 특별 요청사항 저장 시 호출
     */
    @Transactional
    public void updateProductMemo(UUID productId, String memo) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.setMemo(memo);
        productRepository.save(product);
    }
}
