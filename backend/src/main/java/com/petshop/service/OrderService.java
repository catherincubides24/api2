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
import com.petshop.event.OrderPaidEvent;
import com.petshop.exception.BadRequestException;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de pedidos y ventas.
 * Defensa en profundidad: el controller ya valida permisos con @PreAuthorize y
 * este servicio vuelve a verificar la propiedad (ver {@link #verifyOwnershipOrStaff}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PetOrderRepository petOrderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher events;

    // ---------------------------------------------------------------- lectura

    public PageResponse<OrderResponse> getAllOrdersPaged(int page, int size) {
        return toPageResponse(petOrderRepository.findAll(buildPageable(page, size)));
    }

    public PageResponse<OrderResponse> getOrdersByUserIdPaged(Long userId, int page, int size) {
        verifyOwnershipOrStaff(userId);
        return toPageResponse(petOrderRepository.findByUserId(userId, buildPageable(page, size)));
    }

    public OrderResponse getOrderById(Long id) {
        PetOrder order = findOrderById(id);
        verifyOwnershipOrStaff(order.getUser().getId());
        return toResponse(order);
    }

    public TicketResponse getTicket(Long id) {
        PetOrder order = findOrderById(id);
        verifyOwnershipOrStaff(order.getUser().getId());
        return toTicket(order);
    }

    // -------------------------------------------------------------- escritura

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // Un CUSTOMER solo puede crear pedidos a su propio nombre.
        verifyOwnershipOrStaff(request.userId());
        User user = findUserById(request.userId());

        // El estado inicial lo fija siempre el servidor.
        PetOrder order = PetOrder.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.paymentMethod())
                .build();

        List<OrderItem> items = buildOrderItems(request.items(), order);
        order.setItems(new ArrayList<>(items));
        order.setTotalAmount(calculateTotal(items));

        PetOrder saved = petOrderRepository.save(order);
        saved.setTicketNumber(generateTicketNumber(saved.getId()));
        return toResponse(petOrderRepository.save(saved));
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        PetOrder order = findOrderById(id);
        requirePending(order, "Solo se pueden editar pedidos pendientes");

        order.setUser(findUserById(request.userId()));
        // El estado NO se modifica aquí: solo con updateOrderStatus / updatePayment.
        if (request.paymentMethod() != null) {
            order.setPaymentMethod(request.paymentMethod());
        }

        order.getItems().clear();
        List<OrderItem> newItems = buildOrderItems(request.items(), order);
        order.getItems().addAll(newItems);
        order.setTotalAmount(calculateTotal(newItems));

        return toResponse(petOrderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        PetOrder order = findOrderById(id);
        boolean becamePaid = order.getStatus() != OrderStatus.PAID && request.status() == OrderStatus.PAID;

        order.setStatus(request.status());
        PetOrder saved = petOrderRepository.save(order);

        if (becamePaid) {
            events.publishEvent(new OrderPaidEvent(saved.getId()));
        }
        return toResponse(saved);
    }

    /** Confirmación de pagos en efectivo/transferencia (ADMIN y EMPLOYEE). */
    @Transactional
    public OrderResponse updatePayment(Long id, PaymentUpdateRequest request) {
        PetOrder order = findOrderById(id);
        order.setPaymentMethod(request.paymentMethod());

        if (request.markAsPaid()) {
            requirePending(order, "Solo se pueden marcar como pagados los pedidos pendientes");
            order.setStatus(OrderStatus.PAID);
        }

        PetOrder saved = petOrderRepository.save(order);
        if (request.markAsPaid()) {
            events.publishEvent(new OrderPaidEvent(saved.getId())); // Observer
        }
        return toResponse(saved);
    }

    @Transactional
    public void deleteOrder(Long id) {
        petOrderRepository.delete(Objects.requireNonNull(findOrderById(id)));
    }

    // ---------------------------------------------------------------- helpers

    private PageRequest buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // El orden lo fija el servidor: el cliente no puede ordenar por campos internos.
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<OrderResponse> toPageResponse(Page<PetOrder> pageResult) {
        List<OrderResponse> content = pageResult.getContent().stream().map(this::toResponse).toList();
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

    private void requirePending(PetOrder order, String message) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(message);
        }
    }

    private List<OrderItem> buildOrderItems(List<OrderItemRequest> itemRequests, PetOrder order) {
        return itemRequests.stream()
                .map(itemRequest -> {
                    Product product = findProductById(itemRequest.productId());
                    if (!Boolean.TRUE.equals(product.getActive())) {
                        throw new BadRequestException("El producto '" + product.getName() + "' no está disponible");
                    }
                    if (product.getStock() < itemRequest.quantity()) {
                        throw new BadRequestException("Stock insuficiente para '" + product.getName() + "'");
                    }
                    return OrderItem.builder()
                            .order(order)
                            .product(product)
                            .quantity(itemRequest.quantity())
                            .unitPrice(product.getPrice()) // el precio lo pone el servidor, nunca el cliente
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

    /** ADMIN y EMPLOYEE acceden a todo; un CUSTOMER solo a lo suyo. */
    private void verifyOwnershipOrStaff(Long targetUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ForbiddenException("No autenticado");
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_EMPLOYEE"));
        if (isStaff) {
            return;
        }

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("No autenticado"));

        if (!currentUser.getId().equals(targetUserId)) {
            throw new ForbiddenException("No tienes permiso para acceder a esta información");
        }
    }

    private List<OrderItemResponse> toItemResponses(PetOrder order) {
        return order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();
    }

    private OrderResponse toResponse(PetOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getFullName(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTicketNumber(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                toItemResponses(order)
        );
    }

    private TicketResponse toTicket(PetOrder order) {
        return new TicketResponse(
                order.getTicketNumber(),
                order.getId(),
                LocalDateTime.now(),
                order.getCreatedAt(),
                order.getUser().getFullName(),
                order.getUser().getEmail(),
                order.getStatus(),
                order.getPaymentMethod(),
                toItemResponses(order),
                order.getTotalAmount()
        );
    }
}