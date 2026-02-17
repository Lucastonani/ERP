package com.erp.ia.execution;

import com.erp.ia.execution.model.ExecutedAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExecutedActionRepository extends JpaRepository<ExecutedAction, Long> {
    Optional<ExecutedAction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
