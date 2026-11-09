package com.github.mizo432.reg.sales.repository;

import com.github.mizo432.reg.sales.domain.CancelReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancelReturnRepository extends JpaRepository<CancelReturn, Long> {

    List<CancelReturn> findByOriginalTransactionId(String originalTransactionId);

    List<CancelReturn> findByStatus(CancelReturn.Status status);
}
