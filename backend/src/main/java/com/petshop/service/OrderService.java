package com.petshop.service;

import com.petshop.dto.common.PageResponse;
import com.petshop.dto.order.OrderItemRequest;
import com.petshop.dto.order.OrderItemResponse;
import com.petshop.dto.order.OrderRequest;
import com.petshop.dto.order.OrderResponse;
import com.petshop.dto.order.OrderStatusUpdateRequest;
import com.petshop.dto.order.PaymentUpdateRequest;
import com.petshop.dto.order.TicketResponse;
import com.petshop.entity.OrderItem;
import com.petshop.entity.OrderStatus;
import com.petshop.entity.PetOrder;
import com.petshop.entity.Product;
import com.petshop.entity.User;
import com.petshop.exception.ForbiddenException;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.PetOrderRepository;
import com.petshop.repository.ProductRepository;
import com.petshop.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquesta la logica de negocio de pedidos y ventas.
 * Los metodos de lectura verifican que un CUSTOMER solo pueda ver su propia
 * informacion; un ADMIN tiene acceso completo (ver {@link #verifyOwnershipOrAdmin}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PetOrderRepository petOrderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public PageResponse<OrderResponse> getAllOrdersPaged(int page, int size) {
        Page<PetOrder> result = petOrderRepository.findAll(buildPageable(page, size));
        return toPageResponse(result);
    }

    public PageResponse<OrderResponse> getOrdersByUserIdPaged(Long userId, int page, int size) {
        verifyOwnershipOrAdmin(userId);
        Page<PetOrder> result = petOrderRepository.findByUserId(userId, buildPageable(page, size));
        return toPageResponse(result);
    }

    public OrderResponse getOrderById(Long id) {
        PetOrder order = findOrderById(id);
        verifyOwnershipOrAdmin(order.getUser().getId());
        return toResponse(order);
    }

    public TicketResponse getTicket(Long id) {
        PetOrder order = findOrderById(id);
        verifyOwnershipOrAdmin(order.getUser().getId());
        return toTicket(order);
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = findUserById(request.userId());

        PetOrder order = PetOrder.builder()
                .user(user)
                .status(request.status() == null ? OrderStatus.PENDING : request.status())
                .paymentMethod(request.paymentMethod())
                .build();

        List<OrderItem> items = buildOrderItems(request.items(), order);
        order.setItems(new ArrayList<>(items));
        order.setTotalAmount(calculateTotal(items));

        PetOrder savedOrder = petOrderRepository.save(order);
        savedOrder.setTicketNumber(generateTicketNumber(savedOrder.getId()));
        savedOrder = petOrderRepository.save(savedOrder);

        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        PetOrder order = findOrderById(id);
        User user = findUserById(request.userId());

        order.setUser(user);
        order.setStatus(request.status() == null ? order.getStatus() : request.status());
        order.setPaymentMethod(request.paymentMethod() == null ? order.getPaymentMethod() : request.paymentMethod());

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

    /** Usado por el modulo de ventas / gestion administrativa de pedidos para confirmar pagos en efectivo o transferencia. */
    @Transactional
    public OrderResponse updatePayment(Long id, PaymentUpdateRequest request) {
        PetOrder order = findOrderById(id);
        order.setPaymentMethod(request.paymentMethod());
        if (request.markAsPaid()) {
            order.setStatus(OrderStatus.PAID);
        }
        PetOrder savedOrder = petOrderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional
    public void deleteOrder(Long id) {
        PetOrder order = findOrderById(id);
        petOrderRepository.delete(Objects.requireNonNull(order));
    }

    // ---------------------------------------------------------------- helpers

    private PageRequest buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<OrderResponse> toPageResponse(Page<PetOrder> pageResult) {
        List<OrderResponse> content = pageResult.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    private String generateTicketNumber(Long orderId) {
        return "HS-" + String.format("%06d", orderId);
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

    /**
     * Garantiza que un CUSTOMER solo pueda ver su propia informacion (pedidos, tickets).
     * Los administradores tienen acceso completo. Evita que un cliente consulte
     * pedidos o datos de otro usuario cambiando el id en la URL.
     */
    private void verifyOwnershipOrAdmin(Long targetUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ForbiddenException("No autenticado");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("No autenticado"));

        if (!currentUser.getId().equals(targetUserId)) {
            throw new ForbiddenException("No tienes permiso para acceder a esta informacion");
        }
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
                order.getPaymentMethod(),
                order.getTicketNumber(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses
        );
    }

    private TicketResponse toTicket(PetOrder order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();

        return new TicketResponse(
                order.getTicketNumber(),
                order.getId(),
                LocalDateTime.now(),
                order.getCreatedAt(),
                order.getUser().getFullName(),
                order.getUser().getEmail(),
                order.getStatus(),
                order.getPaymentMethod(),
                itemResponses,
                order.getTotalAmount()
        );
    }
}
