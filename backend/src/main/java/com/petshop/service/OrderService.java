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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final PetOrderRepository petOrderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public List<OrderResponse> getAllOrders() {
        return petOrderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return petOrderRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getOrderById(Long id) {
        return toResponse(findOrderById(id));
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = findUserById(request.userId());

        PetOrder order = PetOrder.builder()
                .user(user)
                .status(request.status() == null ? OrderStatus.PENDING : request.status())
                .build();

        List<OrderItem> items = buildOrderItems(request.items(), order);
        order.setItems(new ArrayList<>(items));
        order.setTotalAmount(calculateTotal(items));

        PetOrder savedOrder = petOrderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        PetOrder order = findOrderById(id);
        User user = findUserById(request.userId());

        order.setUser(user);
        order.setStatus(request.status() == null ? order.getStatus() : request.status());

        order.getItems().clear();
        List<OrderItem> newItems = buildOrderItems(request.items(), order);
        order.getItems().addAll(newItems);
        order.setTotalAmount(calculateTotal(newItems));

        PetOrder savedOrder = petOrderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        PetOrder order = findOrderById(id);
        order.setStatus(request.status());
        PetOrder savedOrder = petOrderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional
    public void deleteOrder(Long id) {
        PetOrder order = findOrderById(id);
        petOrderRepository.delete(Objects.requireNonNull(order));
    }

    private List<OrderItem> buildOrderItems(List<OrderItemRequest> itemRequests, PetOrder order) {
        return itemRequests.stream()
                .map(itemRequest -> {
                    Product product = findProductById(itemRequest.productId());
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
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id " + id));
    }

    private User findUserById(Long id) {
        return userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    private Product findProductById(Long id) {
        return productRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + id));
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
