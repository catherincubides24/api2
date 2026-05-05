package com.petshop.service;

import com.petshop.dto.product.ProductRequest;
import com.petshop.dto.product.ProductResponse;
import com.petshop.entity.Product;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.ProductRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getPublicProducts() {
        log.info("Consultando productos activos públicos");
        List<ProductResponse> products = productRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
        log.debug("Productos activos encontrados: {}", products.size());
        return products;
    }

    public List<ProductResponse> getAllProducts() {
        log.info("Consultando todos los productos (admin)");
        List<ProductResponse> products = productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
        log.debug("Total productos encontrados: {}", products.size());
        return products;
    }

    public ProductResponse getProductById(Long id) {
        log.info("Consultando producto con id: {}", id);
        return toResponse(findProductById(id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creando producto: nombre='{}', precio={}, stock={}",
                request.name(), request.price(), request.stock());

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .imageUrl(request.imageUrl())
                .category(request.category())
                .active(request.active() == null ? Boolean.TRUE : request.active())
                .build();

        ProductResponse response = toResponse(productRepository.save(Objects.requireNonNull(product)));
        log.info("Producto creado exitosamente: id={}, nombre='{}'", response.id(), response.name());
        return response;
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Actualizando producto id: {}", id);
        Product product = findProductById(id);

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(request.category());
        product.setActive(request.active() == null ? product.getActive() : request.active());

        ProductResponse response = toResponse(productRepository.save(product));
        log.info("Producto actualizado exitosamente: id={}, nombre='{}'", response.id(), response.name());
        return response;
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Eliminando producto id: {}", id);
        Product product = findProductById(id);
        productRepository.delete(Objects.requireNonNull(product));
        log.info("Producto eliminado exitosamente: id={}", id);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado con id: {}", id);
                    return new ResourceNotFoundException("Producto no encontrado con id " + id);
                });
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getCategory(),
                product.getActive(),
                product.getCreatedAt()
        );
    }
}
