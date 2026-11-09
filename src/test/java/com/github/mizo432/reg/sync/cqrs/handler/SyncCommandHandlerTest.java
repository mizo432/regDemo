package com.github.mizo432.reg.sync.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.sync.cqrs.command.MarkAsAppliedCommand;
import com.github.mizo432.reg.sync.cqrs.command.MarkAsFailedCommand;
import com.github.mizo432.reg.sync.cqrs.command.MarkAsSentCommand;
import com.github.mizo432.reg.sync.cqrs.command.QueueInboxCommand;
import com.github.mizo432.reg.sync.repository.SyncInboxRepository;
import com.github.mizo432.reg.sync.repository.SyncOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncCommandHandlerTest {

    @Mock
    private SyncOutboxRepository syncOutboxRepository;

    @Mock
    private SyncInboxRepository syncInboxRepository;

    @InjectMocks
    private SyncCommandHandler syncCommandHandler;

    // === MarkAsSentCommand の入力検証 ===

    @Test
    void markAsSent_null取引IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new MarkAsSentCommand(null));
        });
    }

    @Test
    void markAsSent_空文字列の取引IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new MarkAsSentCommand(""));
        });
    }

    @Test
    void markAsSent_空白文字列の取引IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new MarkAsSentCommand("   "));
        });
    }

    // === MarkAsFailedCommand の入力検証 ===

    @Test
    void markAsFailed_null取引IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new MarkAsFailedCommand(null, "error"));
        });
    }

    @Test
    void markAsFailed_空文字列の取引IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new MarkAsFailedCommand("", "error"));
        });
    }

    // === QueueInboxCommand の入力検証 ===

    @Test
    void queueInbox_nullイベントタイプでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new QueueInboxCommand(null, "event-1", "payload"));
        });
    }

    @Test
    void queueInbox_空文字列のイベントタイプでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new QueueInboxCommand("", "event-1", "payload"));
        });
    }

    @Test
    void queueInbox_nullイベントIDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new QueueInboxCommand("PRODUCT_CREATED", null, "payload"));
        });
    }

    @Test
    void queueInbox_空文字列のイベントIDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new QueueInboxCommand("PRODUCT_CREATED", "", "payload"));
        });
    }

    // === MarkAsAppliedCommand の入力検証 ===

    @Test
    void markAsApplied_nullInboxIDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncCommandHandler.execute(new MarkAsAppliedCommand(null));
        });
    }
}
