package com.erp.ia.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmOutputValidatorTest {

    private LlmOutputValidator validator;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator beanValidator = factory.getValidator();
        validator = new LlmOutputValidator(objectMapper, beanValidator);
    }

    @Test
    void shouldParseValidJson() {
        String json = """
                {"name": "Widget A", "quantity": 10}
                """;

        TestDto result = validator.validateAndParse(json, TestDto.class);
        assertNotNull(result);
        assertEquals("Widget A", result.name);
        assertEquals(10, result.quantity);
    }

    @Test
    void shouldStripMarkdownCodeBlock() {
        String markdown = """
                ```json
                {"name": "Widget B", "quantity": 5}
                ```
                """;

        TestDto result = validator.validateAndParse(markdown, TestDto.class);
        assertNotNull(result);
        assertEquals("Widget B", result.name);
    }

    @Test
    void shouldReturnNullForInvalidJson() {
        String invalid = "this is not json";
        TestDto result = validator.validateAndParse(invalid, TestDto.class);
        assertNull(result);
    }

    @Test
    void shouldReturnNullForNullInput() {
        TestDto result = validator.validateAndParse(null, TestDto.class);
        assertNull(result);
    }

    // Test DTO
    public static class TestDto {
        public String name;
        public int quantity;
    }
}
