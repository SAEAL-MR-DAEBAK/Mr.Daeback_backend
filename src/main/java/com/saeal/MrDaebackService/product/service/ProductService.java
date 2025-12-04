package com.saeal.MrDaebackService.product.service;

import com.saeal.MrDaebackService.dinner.domain.Dinner;
import com.saeal.MrDaebackService.dinner.domain.DinnerMenuItem;
import com.saeal.MrDaebackService.dinner.repository.DinnerMenuItemRepository;
import com.saeal.MrDaebackService.dinner.repository.DinnerRepository;
import com.saeal.MrDaebackService.product.domain.Product;
import com.saeal.MrDaebackService.product.domain.ProductMenuItem;
import com.saeal.MrDaebackService.product.dto.request.AddProductMenuItemRequest;
import com.saeal.MrDaebackService.product.dto.request.CreateProductRequest;
import com.saeal.MrDaebackService.product.dto.request.UpdateProductMenuItemRequest;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DinnerRepository dinnerRepository;
    private final ServingStyleRepository servingStyleRepository;
    private final DinnerMenuItemRepository dinnerMenuItemRepository;
    private final MenuItemsRepository menuItemsRepository;

    @Transactional
    public ProductResponseDto createProduct(CreateProductRequest request) {
        UUID dinnerId = UUID.fromString(request.getDinnerId());
        UUID servingStyleId = UUID.fromString(request.getServingStyleId());

        Dinner dinner = dinnerRepository.findById(dinnerId)
                .orElseThrow(() -> new IllegalArgumentException("Dinner not found: " + dinnerId));
        ServingStyle servingStyle = servingStyleRepository.findById(servingStyleId)
                .orElseThrow(() -> new IllegalArgumentException("Serving style not found: " + servingStyleId));

        int quantity = request.getQuantity();
        BigDecimal unitPrice = dinner.getBasePrice().add(servingStyle.getExtraPrice());
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        String productName = request.getProductName();
        if (productName == null || productName.isBlank()) {
            productName = dinner.getDinnerName() + " - " + servingStyle.getStyleName();
        }

        Product product = Product.builder()
                .dinner(dinner)
                .servingStyle(servingStyle)
                .productName(productName)
                .totalPrice(totalPrice)
                .quantity(quantity)
                .memo(request.getMemo())
                .address(request.getAddress())
                .build();

        List<DinnerMenuItem> dinnerMenuItems = getDinnerMenuItemsEntities(dinnerId);
        for (DinnerMenuItem dinnerMenuItem : dinnerMenuItems) {
            BigDecimal menuItemUnitPrice = dinnerMenuItem.getMenuItem().getUnitPrice();
            int defaultQty = dinnerMenuItem.getDefaultQuantity();

            ProductMenuItem productMenuItem = ProductMenuItem.builder()
                    .product(product)
                    .menuItem(dinnerMenuItem.getMenuItem())
                    .quantity(defaultQty)
                    .unitPrice(menuItemUnitPrice)
                    .lineTotal(menuItemUnitPrice.multiply(BigDecimal.valueOf(defaultQty)))
                    .build();
            product.getProductMenuItems().add(productMenuItem);
        }

        Product savedProduct = productRepository.save(product);
        return ProductResponseDto.from(savedProduct);
    }

    @Transactional
    public List<ProductMenuItemResponseDto> getProductMenuItems(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return product.getProductMenuItems().stream()
                .map(ProductMenuItemResponseDto::from)
                .collect(Collectors.toList());
    }

    private List<DinnerMenuItem> getDinnerMenuItemsEntities(UUID dinnerId) {
        return dinnerMenuItemRepository.findByDinnerId(dinnerId);
    }

    @Transactional
    public ProductMenuItemResponseDto addMenuItemToProduct(UUID productId, AddProductMenuItemRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        UUID menuItemId = UUID.fromString(request.getMenuItemId());
        MenuItems menuItem = menuItemsRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + menuItemId));

        int quantity = request.getQuantity();
        // Always align product_menu_item unit price with the source menu item price to keep consistency.
        BigDecimal unitPrice = menuItem.getUnitPrice() == null ? BigDecimal.ZERO : menuItem.getUnitPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        ProductMenuItem productMenuItem = ProductMenuItem.builder()
                .product(product)
                .menuItem(menuItem)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .lineTotal(lineTotal)
                .build();

        product.getProductMenuItems().add(productMenuItem);
        BigDecimal currentTotal = product.getTotalPrice() == null ? BigDecimal.ZERO : product.getTotalPrice();
        product.setTotalPrice(currentTotal.add(lineTotal));

        Product saved = productRepository.save(product);
        // find the newly added item by menu item id and quantity for response
        return saved.getProductMenuItems().stream()
                .filter(pmi -> pmi.getMenuItem().getId().equals(menuItem.getId()) && pmi.getQuantity() == quantity)
                .map(ProductMenuItemResponseDto::from)
                .findFirst()
                .orElseGet(() -> ProductMenuItemResponseDto.from(productMenuItem));
    }

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

        BigDecimal oldLineTotal = target.getLineTotal() == null ? BigDecimal.ZERO : target.getLineTotal();
        BigDecimal newLineTotal = unitPrice.multiply(BigDecimal.valueOf(newQuantity));

        target.setQuantity(newQuantity);
        target.setLineTotal(newLineTotal);

        BigDecimal currentTotal = product.getTotalPrice() == null ? BigDecimal.ZERO : product.getTotalPrice();
        product.setTotalPrice(currentTotal.subtract(oldLineTotal).add(newLineTotal));

        Product saved = productRepository.save(product);

        return saved.getProductMenuItems().stream()
                .filter(pmi -> pmi.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .map(ProductMenuItemResponseDto::from)
                .orElseThrow(() -> new IllegalStateException("Updated menu item not found after save"));
    }

    @Transactional
    public void removeProductMenuItem(UUID productId, UUID menuItemId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        ProductMenuItem target = product.getProductMenuItems().stream()
                .filter(pmi -> pmi.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found in product: " + menuItemId));

        BigDecimal lineTotal = target.getLineTotal() == null ? BigDecimal.ZERO : target.getLineTotal();
        BigDecimal currentTotal = product.getTotalPrice() == null ? BigDecimal.ZERO : product.getTotalPrice();
        BigDecimal newTotal = currentTotal.subtract(lineTotal);
        product.setTotalPrice(newTotal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newTotal);

        product.getProductMenuItems().remove(target); // orphanRemoval=true handles delete
        productRepository.save(product);
    }
}
