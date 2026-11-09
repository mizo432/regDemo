package com.github.mizo432.reg.commission.domain;

import com.github.mizo432.reg.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CommissionProductDomainTest {

    @Test
    void calculateCommission_RATEタイプで正しく計算されること() {
        // Given
        CommissionProduct commission = new CommissionProduct();
        commission.setCommissionType(CommissionProduct.CommissionType.RATE);
        commission.setCommissionRate(new BigDecimal("0.1"));

        // When
        BigDecimal result = commission.calculateCommission(new BigDecimal("1000"));

        // Then
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void calculateCommission_FIXEDタイプで固定額が返ること() {
        // Given
        CommissionProduct commission = new CommissionProduct();
        commission.setCommissionType(CommissionProduct.CommissionType.FIXED);
        commission.setFixedCommission(new BigDecimal("50"));

        // When
        BigDecimal result = commission.calculateCommission(new BigDecimal("1000"));

        // Then
        assertEquals(new BigDecimal("50"), result);
    }

    // === 敵対的検証: 負の値・null・極端な値（修正済み - 検証が追加された） ===

    @Test
    void setCommissionRate_負の手数料率でValidationExceptionがスローされる() {
        // Given
        CommissionProduct commission = new CommissionProduct();

        // When & Then - setterで検証
        assertThrows(ValidationException.class, () -> {
            commission.setCommissionRate(new BigDecimal("-0.1"));
        });
    }

    @Test
    void setCommissionRate_1を超える手数料率でValidationExceptionがスローされる() {
        // Given
        CommissionProduct commission = new CommissionProduct();

        // When & Then - setterで検証
        assertThrows(ValidationException.class, () -> {
            commission.setCommissionRate(new BigDecimal("2.0"));
        });
    }

    @Test
    void setFixedCommission_負の固定額でValidationExceptionがスローされる() {
        // Given
        CommissionProduct commission = new CommissionProduct();

        // When & Then - setterで検証
        assertThrows(ValidationException.class, () -> {
            commission.setFixedCommission(new BigDecimal("-50"));
        });
    }

    @Test
    void calculateCommission_負の販売額でValidationExceptionがスローされる() {
        // Given
        CommissionProduct commission = new CommissionProduct();
        commission.setCommissionType(CommissionProduct.CommissionType.RATE);
        commission.setCommissionRate(new BigDecimal("0.1"));

        // When & Then
        assertThrows(ValidationException.class, () -> {
            commission.calculateCommission(new BigDecimal("-1000"));
        });
    }

    @Test
    void calculateCommission_0の販売額で0の手数料が計算されること() {
        // Given
        CommissionProduct commission = new CommissionProduct();
        commission.setCommissionType(CommissionProduct.CommissionType.RATE);
        commission.setCommissionRate(new BigDecimal("0.1"));

        // When
        BigDecimal result = commission.calculateCommission(BigDecimal.ZERO);

        // Then
        assertEquals(new BigDecimal("0.00"), result);
    }

    @Test
    void calculateCommission_null手数料率でValidationExceptionがスローされる() {
        // Given
        CommissionProduct commission = new CommissionProduct();
        commission.setCommissionType(CommissionProduct.CommissionType.RATE);
        commission.setCommissionRate(null);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            commission.calculateCommission(new BigDecimal("1000"));
        });
    }

    @Test
    void calculateCommission_null固定額でValidationExceptionがスローされる() {
        // Given
        CommissionProduct commission = new CommissionProduct();
        commission.setCommissionType(CommissionProduct.CommissionType.FIXED);
        commission.setFixedCommission(null);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            commission.calculateCommission(new BigDecimal("1000"));
        });
    }

    @Test
    void setCommissionRate_極端な手数料率でValidationExceptionがスローされる() {
        // 1.0を超える手数料率はsetterで拒否される
        // Given
        CommissionProduct commission = new CommissionProduct();

        // When & Then
        assertThrows(ValidationException.class, () -> {
            commission.setCommissionRate(new BigDecimal("999999.9999"));
        });
    }

    @Test
    void calculateCommission_null販売額でValidationExceptionがスローされる() {
        // Given
        CommissionProduct commission = new CommissionProduct();
        commission.setCommissionType(CommissionProduct.CommissionType.RATE);
        commission.setCommissionRate(new BigDecimal("0.1"));

        // When & Then - ValidationExceptionがスローされる
        assertThrows(ValidationException.class, () -> {
            commission.calculateCommission(null);
        });
    }
}
