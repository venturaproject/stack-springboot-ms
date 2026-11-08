package com.boilerplate.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.support.AbstractRepositoryIT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OutboxEventRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    OutboxEventRepository repository;

    private static OutboxEvent event(String type) {
        return OutboxEvent.capture(type, "auth", UUID.randomUUID(), "{}");
    }

    @Test
    void claimDue_returns_only_pending_rows_oldest_first_within_the_limit() throws Exception {
        OutboxEvent a = repository.save(event("auth.x.v1"));
        Thread.sleep(5);
        OutboxEvent b = repository.save(event("auth.y.v1"));
        Thread.sleep(5);
        OutboxEvent published = event("auth.z.v1");
        published.markPublished();
        repository.save(published);
        OutboxEvent dead = event("auth.w.v1");
        dead.deadLetter();
        repository.save(dead);

        List<OutboxEvent> claimed = repository.claimDue(10);
        assertThat(claimed).extracting(OutboxEvent::getId).containsExactly(a.getId(), b.getId());

        assertThat(repository.claimDue(1)).hasSize(1);
        assertThat(repository.countByPublishedAtIsNullAndFailedAtIsNull()).isEqualTo(2);
        assertThat(repository.countByFailedAtIsNotNull()).isEqualTo(1);
    }

    @Test
    void a_backed_off_row_is_not_claimed_until_its_next_attempt_is_due() {
        OutboxEvent parked = event("auth.parked.v1");
        parked.recordFailure(java.time.Duration.ofHours(1));
        repository.save(parked);

        assertThat(repository.claimDue(10)).extracting(OutboxEvent::getId).doesNotContain(parked.getId());
    }

    @Test
    void deletePublishedBefore_only_removes_old_published_rows() {
        OutboxEvent old = event("auth.old.v1");
        old.markPublished();
        repository.save(old);
        OutboxEvent pending = repository.save(event("auth.pending.v1"));

        int deleted = repository.deletePublishedBefore(Instant.now().plus(1, ChronoUnit.DAYS));

        assertThat(deleted).isGreaterThanOrEqualTo(1);
        assertThat(repository.findById(old.getId())).isEmpty();
        assertThat(repository.findById(pending.getId())).isPresent();
    }
}
