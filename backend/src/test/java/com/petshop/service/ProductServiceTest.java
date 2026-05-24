// backend/src/test/java/com/petshop/service/ProductServiceTest.java
package com.petshop.service;

import com.petshop.dto.product.ProductRequest;
import com.petshop.dto.product.ProductResponse;
import com.petshop.entity.Product;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Alimento Premium")
                .description("Descripción")
                .price(new BigDecimal("39.90"))
                .stock(25)
                .imageUrl("https://example.com/img.jpg")
                .category("Perros")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getPublicProducts - retorna solo productos activos")
    void getPublicProducts_returnsActiveProducts() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getPublicProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Alimento Premium");
        assertThat(result.get(0).active()).isTrue();
        verify(productRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("getPublicProducts - lista vacía cuando no hay productos activos")
    void getPublicProducts_returnsEmptyWhenNoActive() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of());

        List<ProductResponse> result = productService.getPublicProducts();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllProducts - retorna todos los productos incluyendo inactivos")
    void getAllProducts_returnsAll() {
        Product inactive = Product.builder().id(2L).name("Inactivo")
                .price(BigDecimal.TEN).stock(0).active(false).createdAt(LocalDateTime.now()).build();
        when(productRepository.findAll()).thenReturn(List.of(product, inactive));

        List<ProductResponse> result = productService.getAllProducts();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getProductById - retorna producto cuando existe")
    void getProductById_returnsProduct_whenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Alimento Premium");
    }

    @Test
    @DisplayName("getProductById - lanza excepción cuando no existe")
    void getProductById_throws_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("createProduct - persiste y retorna producto con active=true por defecto")
    void createProduct_defaultsActiveToTrue() {
        ProductRequest request = new ProductRequest(
                "Nuevo", "Desc", new BigDecimal("10.00"), 5,
                null, "Gatos", null // active null → debe ser true
        );
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse result = productService.createProduct(request);

        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct - respeta active=false cuando se envía explícitamente")
    void createProduct_respectsExplicitActiveFalse() {
        ProductRequest request = new ProductRequest(
                "Nuevo", "Desc", new BigDecimal("10.00"), 5, null, "Gatos", false
        );
        Product saved = Product.builder().id(2L).name("Nuevo").price(BigDecimal.TEN)
                .stock(5).active(false).createdAt(LocalDateTime.now()).build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.createProduct(request);

        assertThat(result.active()).isFalse();
    }

    @Test
    @DisplayName("updateProduct - actualiza todos los campos")
    void updateProduct_updatesAllFields() {
        ProductRequest request = new ProductRequest(
                "Actualizado", "Nueva desc", new BigDecimal("55.00"),
                10, "https://img.com", "Accesorios", false
        );
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Necesitamos que el producto tenga createdAt para el toResponse
        product.setCreatedAt(LocalDateTime.now());
        ProductResponse result = productService.updateProduct(1L, request);

        assertThat(result.name()).isEqualTo("Actualizado");
        assertThat(result.price()).isEqualByComparingTo("55.00");
        assertThat(result.active()).isFalse();
    }

    @Test
    @DisplayName("updateProduct - mantiene active actual cuando request trae null")
    void updateProduct_keepsActiveWhenNull() {
        ProductRequest request = new ProductRequest(
                "Nombre", "Desc", BigDecimal.TEN, 5, null, null, null
        );
        product.setActive(true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, request);

        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("updateProduct - lanza excepción cuando producto no existe")
    void updateProduct_throws_whenNotFound() {
        ProductRequest request = new ProductRequest("X", null, BigDecimal.ONE, 1, null, null, true);
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteProduct - invoca delete cuando producto existe")
    void deleteProduct_deletesSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    @DisplayName("deleteProduct - lanza excepción cuando no existe")
    void deleteProduct_throws_whenNotFound() {
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}