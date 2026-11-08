package com.boilerplate.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} (the outbox relay, the saga recovery job, the retention sweeps)
 * and ShedLock.
 *
 * <p>With more than one instance every {@code @Scheduled} method fires on every instance.
 * The outbox relay tolerates that — it claims rows with {@code FOR UPDATE SKIP LOCKED}, so
 * running it everywhere just adds throughput. The housekeeping jobs (retention sweeps, saga
 * recovery) must not: a bulk {@code DELETE} or a re-drive should happen once per tick, not
 * once per instance. Those methods carry {@code @SchedulerLock} and a single instance wins
 * the lock in the {@code shedlock} table each run.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulingConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime() // compare against the database clock, not each instance's
                .build());
    }
}
