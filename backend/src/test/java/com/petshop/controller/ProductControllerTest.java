package com.petshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.config.TestSecurityConfig;
import com.petshop.dto.product.ProductRequest;
import com.petshop.dto.product.ProductResponse;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.security.JwtAuthenticationFilter;
import com.petshop.security.JwtService;
import com.petshop.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    private ProductResponse productResponse;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        productResponse = new ProductResponse(
                1L,
                "Collar Reflectivo",
                "Collar ajustable para perro",
                new BigDecimal("15.00"),
                10,
                "https://example.com/img.jpg",
                "Accesorios",
                true,
                LocalDateTime.now()
        );

        productRequest = new ProductRequest(
                "Collar Reflectivo",
                "Collar ajustable para perro",
                new BigDecimal("15.00"),
                10,
                "https://example.com/img.jpg",
                "Accesorios",
                true
        );
    }

    @Test
    @DisplayName("GET /api/products - retorna 200 con lista de productos")
    void getPublicProducts_returns200WithList() throws Exception {
        when(productService.getPublicProducts()).thenReturn(List.of(productResponse));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Collar Reflectivo"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(productService).getPublicProducts();
    }

    @Test
    @DisplayName("GET /api/products - retorna lista vacía cuando no hay productos")
    void getPublicProducts_returnsEmptyList() throws Exception {
        when(productService.getPublicProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/products/{id} - retorna 200 con producto")
    void getProductById_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(productResponse);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Collar Reflectivo"))
                .andExpect(jsonPath("$.price").value(15.00));

        verify(productService).getProductById(1L);
    }

    @Test
    @DisplayName("GET /api/products/{id} - retorna 404 cuando no existe")
    void getProductById_returns404_whenNotFound() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado con id 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/products/admin/all - ADMIN retorna 200")
    @WithMockUser(roles = "ADMIN")
    void getAllProducts_withAdmin_returns200() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(productResponse));

        mockMvc.perform(get("/api/products/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Collar Reflectivo"));

        verify(productService).getAllProducts();
    }

    @Test
    @DisplayName("GET /api/products/admin/all - CUSTOMER no tiene acceso")
    @WithMockUser(roles = "CUSTOMER")
    void getAllProducts_withCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/products/admin/all"))
                .andExpect(status().is5xxServerError());

        verify(productService, never()).getAllProducts();
    }

    @Test
    @DisplayName("POST /api/products - ADMIN crea producto, retorna 201")
    @WithMockUser(roles = "ADMIN")
    void createProduct_withAdmin_returns201() throws Exception {
        when(productService.createProduct(any())).thenReturn(productResponse);

        mockMvc.perform(post("/api/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(productService).createProduct(any());
    }

    @Test
    @DisplayName("POST /api/products - nombre en blanco retorna 400")
    @WithMockUser(roles = "ADMIN")
    void createProduct_withBlankName_returns400() throws Exception {
        ProductRequest invalid = new ProductRequest(
                "", "Desc", new BigDecimal("10.00"), 5, null, null, true
        );

        mockMvc.perform(post("/api/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("POST /api/products - precio nulo retorna 400")
    @WithMockUser(roles = "ADMIN")
    void createProduct_withNullPrice_returns400() throws Exception {
        ProductRequest invalid = new ProductRequest(
                "Nombre", "Desc", null, 5, null, null, true
        );

        mockMvc.perform(post("/api/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - stock negativo retorna 400")
    @WithMockUser(roles = "ADMIN")
    void createProduct_withNegativeStock_returns400() throws Exception {
        ProductRequest invalid = new ProductRequest(
                "Nombre", "Desc", new BigDecimal("10.00"), -1, null, null, true
        );

        mockMvc.perform(post("/api/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - CUSTOMER no tiene acceso")
    @WithMockUser(roles = "CUSTOMER")
    void createProduct_withCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().is5xxServerError());

        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("PUT /api/products/{id} - ADMIN actualiza, retorna 200")
    @WithMockUser(roles = "ADMIN")
    void updateProduct_withAdmin_returns200() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(productResponse);

        mockMvc.perform(put("/api/products/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(productService).updateProduct(eq(1L), any());
    }

    @Test
    @DisplayName("PUT /api/products/{id} - producto no existe retorna 404")
    @WithMockUser(roles = "ADMIN")
    void updateProduct_notFound_returns404() throws Exception {
        when(productService.updateProduct(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado con id 99"));

        mockMvc.perform(put("/api/products/99").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/products/{id} - CUSTOMER no tiene acceso")
    @WithMockUser(roles = "CUSTOMER")
    void updateProduct_withCustomer_returns403() throws Exception {
        mockMvc.perform(put("/api/products/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().is5xxServerError());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - ADMIN elimina, retorna 204")
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_withAdmin_returns204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - producto no existe retorna 404")
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Producto no encontrado con id 99"))
                .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - CUSTOMER no tiene acceso")
    @WithMockUser(roles = "CUSTOMER")
    void deleteProduct_withCustomer_returns403() throws Exception {
        mockMvc.perform(delete("/api/products/1").with(csrf()))
                .andExpect(status().is5xxServerError());

        verify(productService, never()).deleteProduct(any());
    }
}