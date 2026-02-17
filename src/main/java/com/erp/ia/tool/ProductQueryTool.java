package com.erp.ia.tool;

import com.erp.ia.core.model.Product;
import com.erp.ia.core.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductQueryTool implements AgentTool<ProductQueryTool.Input, ProductQueryTool.Output> {

    private final ProductRepository productRepository;

    public ProductQueryTool(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public String getName() {
        return "ProductQueryTool";
    }

    @Override
    public String getDescription() {
        return "Searches products by name, category, or SKU";
    }

    @Override
    public Class<Input> getInputType() {
        return Input.class;
    }

    @Override
    public Class<Output> getOutputType() {
        return Output.class;
    }

    @Override
    public Output execute(Input input) {
        List<Product> products;
        if (input.sku != null) {
            products = productRepository.findBySku(input.sku).map(List::of).orElse(List.of());
        } else if (input.category != null) {
            products = productRepository.findByCategory(input.category);
        } else if (input.query != null) {
            products = productRepository.findByNameContainingIgnoreCase(input.query);
        } else {
            products = productRepository.findByTenantId(input.tenantId != null ? input.tenantId : "default");
        }

        List<ProductItem> items = products.stream()
                .filter(Product::isActive)
                .map(p -> new ProductItem(p.getId(), p.getSku(), p.getName(), p.getCategory(), p.getUnit()))
                .toList();

        return new Output(items, items.size());
    }

    // --- Typed DTOs ---

    public static class Input {
        public String sku;
        public String query;
        public String category;
        public String tenantId;
        public Boolean belowMinimum; // unused here, but accepted from agent plan
    }

    public record Output(List<ProductItem> products, int totalCount) {
    }

    public record ProductItem(Long id, String sku, String name, String category, String unit) {
    }
}
