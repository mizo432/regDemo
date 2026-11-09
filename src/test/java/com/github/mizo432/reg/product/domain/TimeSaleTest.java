package com.github.mizo432.reg.product.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class TimeSaleTest {

    @Test
    void isActiveNow_Clock注入でアクティブなタイムセールを判定すること() {
        // Given - 固定時刻: 2026-11-08 10:00
        LocalDateTime fixedNow = LocalDateTime.of(2026, 11, 8, 10, 0, 0);
        Clock fixedClock = Clock.fixed(fixedNow.atZone(ZoneId.systemDefault()).toInstant(), ZoneOffset.systemDefault());

        TimeSale timeSale = new TimeSale();
        timeSale.setSalePrice(new BigDecimal("800"));
        timeSale.setStartDate(fixedNow.minusHours(1));
        timeSale.setEndDate(fixedNow.plusHours(1));
        timeSale.setStatus(TimeSale.Status.ACTIVE);

        // When
        boolean result = timeSale.isActiveNow(fixedClock);

        // Then
        assertTrue(result, "固定時刻が開始〜終了の範囲内であればアクティブであるべき");
    }

    @Test
    void isActiveNow_Clock注入で開始前なら非アクティブを返すこと() {
        // Given - 固定時刻: 2026-11-08 08:00（開始時刻より前）
        LocalDateTime fixedNow = LocalDateTime.of(2026, 11, 8, 8, 0, 0);
        Clock fixedClock = Clock.fixed(fixedNow.atZone(ZoneId.systemDefault()).toInstant(), ZoneOffset.systemDefault());

        TimeSale timeSale = new TimeSale();
        timeSale.setSalePrice(new BigDecimal("800"));
        timeSale.setStartDate(fixedNow.plusHours(1));
        timeSale.setEndDate(fixedNow.plusHours(2));
        timeSale.setStatus(TimeSale.Status.ACTIVE);

        // When
        boolean result = timeSale.isActiveNow(fixedClock);

        // Then
        assertFalse(result, "開始時刻より前であれば非アクティブであるべき");
    }

    @Test
    void isActiveNow_Clock注入で終了後なら非アクティブを返すこと() {
        // Given - 固定時刻: 2026-11-08 12:00（終了時刻より後）
        LocalDateTime fixedNow = LocalDateTime.of(2026, 11, 8, 12, 0, 0);
        Clock fixedClock = Clock.fixed(fixedNow.atZone(ZoneId.systemDefault()).toInstant(), ZoneOffset.systemDefault());

        TimeSale timeSale = new TimeSale();
        timeSale.setSalePrice(new BigDecimal("800"));
        timeSale.setStartDate(fixedNow.minusHours(2));
        timeSale.setEndDate(fixedNow.minusHours(1));
        timeSale.setStatus(TimeSale.Status.ACTIVE);

        // When
        boolean result = timeSale.isActiveNow(fixedClock);

        // Then
        assertFalse(result, "終了時刻より後であれば非アクティブであるべき");
    }

    @Test
    void isActiveNow_INACTIVEステータスならClockに関係なく非アクティブを返すこと() {
        // Given
        LocalDateTime fixedNow = LocalDateTime.of(2026, 11, 8, 10, 0, 0);
        Clock fixedClock = Clock.fixed(fixedNow.atZone(ZoneId.systemDefault()).toInstant(), ZoneOffset.systemDefault());

        TimeSale timeSale = new TimeSale();
        timeSale.setSalePrice(new BigDecimal("800"));
        timeSale.setStartDate(fixedNow.minusHours(1));
        timeSale.setEndDate(fixedNow.plusHours(1));
        timeSale.setStatus(TimeSale.Status.INACTIVE);

        // When
        boolean result = timeSale.isActiveNow(fixedClock);

        // Then
        assertFalse(result, "INACTIVEステータスであれば、時刻範囲内でも非アクティブであるべき");
    }

    @Test
    void isActiveNow_デフォルトメソッドはシステムクロックを使用すること() {
        // Given - 現在時刻の範囲内に設定
        TimeSale timeSale = new TimeSale();
        timeSale.setSalePrice(new BigDecimal("800"));
        timeSale.setStartDate(LocalDateTime.now().minusHours(1));
        timeSale.setEndDate(LocalDateTime.now().plusHours(1));
        timeSale.setStatus(TimeSale.Status.ACTIVE);

        // When - デフォルトのisActiveNow()はシステムクロックを使用
        boolean result = timeSale.isActiveNow();

        // Then
        assertTrue(result, "システムクロックが範囲内であればアクティブであるべき");
    }

    // === 入力検証テスト ===

    @Test
    void setSalePrice_nullでIllegalArgumentExceptionがスローされる() {
        TimeSale timeSale = new TimeSale();
        assertThrows(IllegalArgumentException.class, () -> {
            timeSale.setSalePrice(null);
        });
    }

    @Test
    void setSalePrice_負の値でIllegalArgumentExceptionがスローされる() {
        TimeSale timeSale = new TimeSale();
        assertThrows(IllegalArgumentException.class, () -> {
            timeSale.setSalePrice(new BigDecimal("-100"));
        });
    }

    @Test
    void setStartDate_nullでIllegalArgumentExceptionがスローされる() {
        TimeSale timeSale = new TimeSale();
        assertThrows(IllegalArgumentException.class, () -> {
            timeSale.setStartDate(null);
        });
    }

    @Test
    void setEndDate_nullでIllegalArgumentExceptionがスローされる() {
        TimeSale timeSale = new TimeSale();
        assertThrows(IllegalArgumentException.class, () -> {
            timeSale.setEndDate(null);
        });
    }

    @Test
    void setSalePrice_0は許容される() {
        TimeSale timeSale = new TimeSale();
        assertDoesNotThrow(() -> {
            timeSale.setSalePrice(BigDecimal.ZERO);
        });
    }
}
