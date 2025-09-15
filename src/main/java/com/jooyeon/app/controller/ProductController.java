package com.jooyeon.app.controller;

import com.jooyeon.app.domain.dto.common.ApiResponse;
import com.jooyeon.app.domain.dto.product.ProductResponseDto;
import com.jooyeon.app.domain.entity.product.ProductStatus;
import com.jooyeon.app.service.product.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "상품 관리 API")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(
            @Parameter(description = "상품 ID") @PathVariable Long productId) {

        ProductResponseDto product = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

}