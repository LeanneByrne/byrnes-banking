package com.leannebyrne.bank.transfer;

import com.leannebyrne.bank.common.BankingException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * A thin wrapper around MeterRegistry rather than calling it directly from
 * TransferService -- keeps the metric names and tag shapes in one place, and
 * means a test can verify "a failed transfer increments a counter" without
 * needing to know Micrometer's API at all (see TransferServiceTest).
 */
@Component
public class TransferMetrics {

    private final MeterRegistry registry;

    public TransferMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordCompleted(Timer.Sample sample) {
        registry.counter("transfers.completed").increment();
        sample.stop(registry.timer("transfers.duration", "outcome", "completed"));
    }

    public void recordFailed(Timer.Sample sample, BankingException reason) {
        registry.counter("transfers.failed", "reason", reason.getClass().getSimpleName()).increment();
        sample.stop(registry.timer("transfers.duration", "outcome", "failed"));
    }
}
