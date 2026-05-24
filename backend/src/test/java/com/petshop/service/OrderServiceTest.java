// backend/src/test/java/com/petshop/service/OrderServiceTest.java
package com.petshop.service;

import com.petshop.dto.order.*;
import com.petshop.entity.*;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private PetOrderRepository petOrderRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private OrderService orderService;

    private User user;
    private Product product;
    private PetOrder order;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).fullName("Juan").email("juan@test.com")
                .password("pass").role(Role.CUSTOMER).build();

        product = Product.builder().id(1L).name("Collar").price(new BigDecimal("15.00"))
                .stock(10).active(true).build();

        OrderItem item = OrderItem.builder()
                .id(1L).product(product).quantity(2)
                .unitPrice(product.getPrice()).build();

        order = PetOrder.builder()
                .id(1L).user(user).status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("30.00"))
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);
    }

    @Test
    @DisplayName("getAllOrders - retorna lista de todos los pedidos")
    void getAllOrders_returnsList() {
        when(petOrderRepository.findAll()).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getAllOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("getOrdersByUserId - filtra por usuario")
    void getOrdersByUserId_filtersCorrectly() {
        when(petOrderRepository.findByUserId(1L)).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getOrdersByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getOrderById - lanza excepción cuando no existe")
    void getOrderById_throws_whenNotFound() {
        when(petOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("createOrder - calcula total correctamente y guarda con PENDING por defecto")
    void createOrder_calculatesTotal_defaultsPending() {
        OrderRequest request = new OrderRequest(1L, null,
                List.of(new OrderItemRequest(1L, 3)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(petOrderRepository.save(any(PetOrder.class))).thenAnswer(inv -> {
            PetOrder o = inv.getArgument(0);
            o.setId(10L);
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        OrderResponse result = orderService.createOrder(request);

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.totalAmount()).isEqualByComparingTo("45.00"); // 3 * 15
    }

    @Test
    @DisplayName("createOrder - respeta status enviado en el request")
    void createOrder_respectsProvidedStatus() {
        OrderRequest request = new OrderRequest(1L, OrderStatus.PAID,
                List.of(new OrderItemRequest(1L, 1)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(petOrderRepository.save(any(PetOrder.class))).thenAnswer(inv -> {
            PetOrder o = inv.getArgument(0);
            o.setId(11L);
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        OrderResponse result = orderService.createOrder(request);

        assertThat(result.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("createOrder - lanza excepción si el usuario no existe")
    void createOrder_throws_whenUserNotFound() {
        OrderRequest request = new OrderRequest(99L, null,
                List.of(new OrderItemRequest(1L, 1)));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createOrder - lanza excepción si el producto no existe")
    void createOrder_throws_whenProductNotFound() {
        OrderRequest request = new OrderRequest(1L, null,
                List.of(new OrderItemRequest(99L, 1)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateOrderStatus - cambia estado correctamente")
    void updateOrderStatus_changesStatus() {
        when(petOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(petOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderStatusUpdateRequest req = new OrderStatusUpdateRequest(OrderStatus.SHIPPED);
        OrderResponse result = orderService.updateOrderStatus(1L, req);

        assertThat(result.status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("deleteOrder - invoca delete cuando el pedido existe")
    void deleteOrder_deletesSuccessfully() {
        when(petOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(1L);

        verify(petOrderRepository).delete(order);
    }

    @Test
    @DisplayName("updateOrder - reemplaza items y recalcula total")
    void updateOrder_replacesItemsAndRecalculatesTotal() {
        OrderRequest request = new OrderRequest(1L, OrderStatus.PAID,
                List.of(new OrderItemRequest(1L, 5)));
        when(petOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(petOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = orderService.updateOrder(1L, request);

        assertThat(result.totalAmount()).isEqualByComparingTo("75.00"); // 5 * 15
    }
}