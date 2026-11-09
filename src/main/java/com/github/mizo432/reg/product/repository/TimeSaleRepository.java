package com.github.mizo432.reg.product.repository;

import com.github.mizo432.reg.product.domain.TimeSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TimeSaleRepository extends JpaRepository<TimeSale, Long> {

    /**
     * 指定された商品IDとステータスのタイムセールを、開始日時の降順で返却する。
     * ACTIVE状態のみをフェッチするために使用する。
     */
    List<TimeSale> findByProductIdAndStatusOrderByStartDateDesc(Long productId, TimeSale.Status status);

    List<TimeSale> findByStatus(TimeSale.Status status);
}
