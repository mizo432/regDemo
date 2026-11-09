package com.github.mizo432.reg.commission.repository;

import com.github.mizo432.reg.commission.domain.CommissionProduct;
import com.github.mizo432.reg.commission.domain.CommissionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionRepository extends JpaRepository<CommissionProduct, Long> {

    Optional<CommissionProduct> findByProductIdAndStatus(Long productId, CommissionProduct.Status status);

    List<CommissionProduct> findByStatus(CommissionProduct.Status status);
}
