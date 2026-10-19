package com.petshop.dto.controller;

import com.petshop.dto.common.PageResponse;
import com.petshop.dto.order.OrderRequest;
import com.petshop.dto.order.OrderResponse;
import com.petshop.dto.order.OrderStatusUpdateRequest;
import com.petshop.dto.order.PaymentUpdateRequest;
import com.petshop.dto.order.TicketResponse;
import com.petshop.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String STAFF = "hasAnyRole('ADMIN', 'EMPLOYEE')";

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize(STAFF)
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(orderService.getAllOrdersPaged(page, size));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @orderSecurity.isSelf(#userId, authentication)")
    public ResponseEntity<PageResponse<OrderResponse>> getOrdersByUserId(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(orderService.getOrdersByUserIdPaged(userId, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @orderSecurity.ownsOrder(#id, authentication)")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/{id}/ticket")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @orderSecurity.ownsOrder(#id, authentication)")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(orderService.getTicket(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @orderSecurity.isSelf(#request.userId(), authentication)")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF)
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable @Positive Long id,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(STAFF)
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    /** Confirmar pagos en efectivo/transferencia desde el módulo de ventas (ADMIN y EMPLOYEE). */
    @PatchMapping("/{id}/payment")
    @PreAuthorize(STAFF)
    public ResponseEntity<OrderResponse> updatePayment(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PaymentUpdateRequest request
    ) {
        return ResponseEntity.ok(orderService.updatePayment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable @Positive Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}