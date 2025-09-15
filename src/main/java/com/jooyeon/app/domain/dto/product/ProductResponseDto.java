package com.jooyeon.app.domain.dto.product;

import com.jooyeon.app.domain.entity.product.Product;
import com.jooyeon.app.domain.entity.product.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponseDto convertToResponseDto(Product product) {
        return ProductResponseDto.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}