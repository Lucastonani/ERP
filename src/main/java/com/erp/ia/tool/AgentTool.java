package com.erp.ia.tool;

/**
 * Typed tool interface. Each tool has strongly-typed input and output DTOs.
 */
public interface AgentTool<I, O> {
    String getName();

    String getDescription();

    Class<I> getInputType();

    Class<O> getOutputType();

    O execute(I input);
}
