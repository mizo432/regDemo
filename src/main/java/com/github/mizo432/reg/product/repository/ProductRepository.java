package com.github.mizo432.reg.product.repository;

import com.github.mizo432.reg.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByStatus(Product.Status status);

    List<Product> findByNameContaining(String name);
}
