package com.jooyeon.app.repository;

import com.jooyeon.app.domain.entity.product.Product;
import com.jooyeon.app.domain.entity.product.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {


    List<Product> findByStatusOrderByName(ProductStatus status);

    List<Product> findByStatusAndPriceBetween(ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice);

    List<Product> findByNameContainingAndStatus(String keyword, ProductStatus status);

}