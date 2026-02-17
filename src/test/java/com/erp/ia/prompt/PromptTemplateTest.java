package com.erp.ia.prompt;

import com.erp.ia.prompt.model.PromptTemplate;
import com.erp.ia.prompt.model.PromptTemplate.PromptStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplateTest {

    @Test
    void shouldRenderVariables() {
        PromptTemplate template = new PromptTemplate(
                "inventory-analysis", 1,
                "Analyze stock for product {{productName}} in warehouse {{warehouse}}.");

        String rendered = template.render(Map.of(
                "productName", "Widget A",
                "warehouse", "WH-01"));

        assertEquals("Analyze stock for product Widget A in warehouse WH-01.", rendered);
    }

    @Test
    void shouldHandleMissingVariablesGracefully() {
        PromptTemplate template = new PromptTemplate(
                "test", 1,
                "Hello {{name}}, your role is {{role}}.");

        String rendered = template.render(Map.of("name", "Admin"));

        assertEquals("Hello Admin, your role is {{role}}.", rendered);
    }

    @Test
    void shouldStartAsActive() {
        PromptTemplate template = new PromptTemplate("test", 1, "content");
        assertEquals(PromptStatus.ACTIVE, template.getStatus());
    }
}
