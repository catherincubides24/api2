package com.petshop.dto.controller;

import com.petshop.dto.payment.PaymentCaptureRequest;
import com.petshop.dto.payment.PaymentCaptureResponse;
import com.petshop.dto.payment.PaymentOrderResponse;
import com.petshop.service.PayPalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/payment/paypal")
@RequiredArgsConstructor
public class PayPalController {

    private final PayPalService payPalService;

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.ownsOrder(#orderId, authentication)")
    public ResponseEntity<PaymentOrderResponse> createOrder(
    @RequestParam @NotNull @Positive Long orderId
    ) {
        // Las URLs de retorno las define el servidor (app.frontend-url), no el cliente.
        return ResponseEntity.ok(payPalService.createPayPalOrder(orderId));
    }

    @PostMapping("/capture")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.ownsOrder(#request.orderId(), authentication)")
    public ResponseEntity<PaymentCaptureResponse> captureOrder(
            @Valid @RequestBody PaymentCaptureRequest request
    ) {
        return ResponseEntity.ok(
                payPalService.capturePayPalOrder(request.paypalOrderId(), request.orderId()));
    }
}