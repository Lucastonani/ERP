package com.erp.ia.prompt;

import com.erp.ia.prompt.model.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PromptRepository extends JpaRepository<PromptTemplate, Long> {

    Optional<PromptTemplate> findByNameAndVersionAndTenantId(String name, int version, String tenantId);

    List<PromptTemplate> findByNameAndTenantIdOrderByVersionDesc(String name, String tenantId);

    Optional<PromptTemplate> findFirstByNameAndStatusAndTenantIdOrderByVersionDesc(
            String name, PromptTemplate.PromptStatus status, String tenantId);
}
