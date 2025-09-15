package com.jooyeon.app.service.product;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.ProductException;
import com.jooyeon.app.domain.dto.product.ProductResponseDto;
import com.jooyeon.app.domain.entity.product.Product;
import com.jooyeon.app.domain.entity.product.ProductStatus;
import com.jooyeon.app.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;


    public ProductResponseDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        return ProductResponseDto.convertToResponseDto(product);
    }

    public List<Product> getProductsByIds(List<Long> productIds) {
        return productRepository.findAllById(productIds);
    }

    public void checkStockAvailability(Long productId, int quantity) {
        log.debug("[PRODUCT] 재고 확인: productId={}, quantity={}", productId, quantity);
        // TODO: 실제 재고 확인 로직 구현
    }

    public void reserveStock(Long productId, int quantity) {
        log.debug("[PRODUCT] 재고 예약: productId={}, quantity={}", productId, quantity);
        // TODO: 실제 재고 예약 로직 구현
    }

    public void releaseStock(Long productId, int quantity) {
        log.debug("[PRODUCT] 재고 해제: productId={}, quantity={}", productId, quantity);
        // TODO: 실제 재고 해제 로직 구현
    }
}