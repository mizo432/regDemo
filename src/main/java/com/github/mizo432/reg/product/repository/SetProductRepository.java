package com.github.mizo432.reg.product.repository;

import com.github.mizo432.reg.product.domain.SetProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SetProductRepository extends JpaRepository<SetProduct, Long> {

    Optional<SetProduct> findBySetBarcode(String setBarcode);

    List<SetProduct> findByStatus(SetProduct.Status status);
}
