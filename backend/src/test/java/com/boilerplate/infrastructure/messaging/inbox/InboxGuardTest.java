package com.boilerplate.infrastructure.messaging.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboxGuardTest {

    @Mock
    ProcessedEventRepository repository;

    private InboxGuard guard;

    @BeforeEach
    void setUp() {
        guard = new InboxGuard(repository);
    }

    @Test
    void claim_is_true_when_the_repository_inserted_a_row() {
        UUID eventId = UUID.randomUUID();
        when(repository.insertIfAbsent(any(), eq("consumer-a"), eq(eventId))).thenReturn(1);

        assertThat(guard.claim("consumer-a", eventId)).isTrue();
    }

    @Test
    void claim_is_false_when_the_row_already_existed() {
        UUID eventId = UUID.randomUUID();
        when(repository.insertIfAbsent(any(), eq("consumer-a"), eq(eventId))).thenReturn(0);

        assertThat(guard.claim("consumer-a", eventId)).isFalse();
    }
}
