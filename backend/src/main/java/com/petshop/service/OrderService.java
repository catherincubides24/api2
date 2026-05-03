package com.petshop.service;

import com.petshop.dto.order.OrderItemRequest;
import com.petshop.dto.order.OrderItemResponse;
import com.petshop.dto.order.OrderRequest;
import com.petshop.dto.order.OrderResponse;
import com.petshop.dto.order.OrderStatusUpdateRequest;
import com.petshop.entity.OrderItem;
import com.petshop.entity.OrderStatus;
import com.petshop.entity.PetOrder;
import com.petshop.entity.Product;
import com.petshop.entity.User;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.PetOrderRepository;
import com.petshop.repository.ProductRepository;
import com.petshop.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
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
public class OrderService {

    private final PetOrderRepository petOrderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public List<OrderResponse> getAllOrders() {
        log.info("Consultando todos los pedidos (admin)");
        List<OrderResponse> orders = petOrderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
        log.debug("Total de pedidos encontrados: {}", orders.size());
        return orders;
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.info("Consultando pedidos del usuario id: {}", userId);
        List<OrderResponse> orders = petOrderRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        log.debug("Pedidos encontrados para usuario {}: {}", userId, orders.size());
        return orders;
    }

    public OrderResponse getOrderById(Long id) {
        log.info("Consultando pedido id: {}", id);
        return toResponse(findOrderById(id));
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creando pedido para usuario id: {}, ítems: {}",
                request.userId(), request.items().size());

        User user = findUserById(request.userId());

        PetOrder order = PetOrder.builder()
                .user(user)
                .status(request.status() == null ? OrderStatus.PENDING : request.status())
                .build();

        List<OrderItem> items = buildOrderItems(request.items(), order);
        order.setItems(new ArrayList<>(items));
        order.setTotalAmount(calculateTotal(items));

        PetOrder savedOrder = petOrderRepository.save(order);
        log.info("Pedido creado exitosamente: id={}, usuario={}, total={}, estado={}",
                savedOrder.getId(), user.getEmail(), savedOrder.getTotalAmount(), savedOrder.getStatus());
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        log.info("Actualizando pedido id: {}", id);
        PetOrder order = findOrderById(id);
        User user = findUserById(request.userId());

        order.setUser(user);
        order.setStatus(request.status() == null ? order.getStatus() : request.status());

        order.getItems().clear();
        List<OrderItem> newItems = buildOrderItems(request.items(), order);
        order.getItems().addAll(newItems);
        order.setTotalAmount(calculateTotal(newItems));

        PetOrder savedOrder = petOrderRepository.save(order);
        log.info("Pedido actualizado exitosamente: id={}, nuevo estado={}, nuevo total={}",
                savedOrder.getId(), savedOrder.getStatus(), savedOrder.getTotalAmount());
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        log.info("Actualizando estado del pedido id: {} → {}", id, request.status());
        PetOrder order = findOrderById(id);
        OrderStatus estadoAnterior = order.getStatus();
        order.setStatus(request.status());
        PetOrder savedOrder = petOrderRepository.save(order);
        log.info("Estado del pedido {} cambiado: {} → {}",
                savedOrder.getId(), estadoAnterior, savedOrder.getStatus());
        return toResponse(savedOrder);
    }

    @Transactional
    public void deleteOrder(Long id) {
        log.info("Eliminando pedido id: {}", id);
        PetOrder order = findOrderById(id);
        petOrderRepository.delete(Objects.requireNonNull(order));
        log.info("Pedido eliminado exitosamente: id={}", id);
    }

    private List<OrderItem> buildOrderItems(List<OrderItemRequest> itemRequests, PetOrder order) {
        return itemRequests.stream()
                .map(itemRequest -> {
                    Product product = findProductById(itemRequest.productId());
                    log.debug("Ítem agregado al pedido: producto='{}', cantidad={}, precio={}",
                            product.getName(), itemRequest.quantity(), product.getPrice());
                    return OrderItem.builder()
                            .order(order)
                            .product(product)
                            .quantity(itemRequest.quantity())
                            .unitPrice(product.getPrice())
                            .build();
                })
                .toList();
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PetOrder findOrderById(Long id) {
        return petOrderRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> {
                    log.warn("Pedido no encontrado con id: {}", id);
                    return new ResourceNotFoundException("Pedido no encontrado con id " + id);
                });
    }

    private User findUserById(Long id) {
        return userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con id: {}", id);
                    return new ResourceNotFoundException("Usuario no encontrado con id " + id);
                });
    }

    private Product findProductById(Long id) {
        return productRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado con id: {}", id);
                    return new ResourceNotFoundException("Producto no encontrado con id " + id);
                });
    }

    private OrderResponse toResponse(PetOrder order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getFullName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses
        );
    }
}