package com.petshop.controller;

import com.petshop.dto.payment.PaymentCaptureRequest;
import com.petshop.dto.payment.PaymentCaptureResponse;
import com.petshop.dto.payment.PaymentOrderResponse;
import com.petshop.service.PayPalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment/paypal")
@RequiredArgsConstructor
public class PayPalController {

    private final PayPalService payPalService;

    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @RequestParam @NotNull Long orderId,
            @RequestParam(defaultValue = "http://localhost:5173/payment/success") String returnUrl,
            @RequestParam(defaultValue = "http://localhost:5173/payment/cancel") String cancelUrl
    ) {
        return ResponseEntity.ok(
                payPalService.createPayPalOrder(orderId, returnUrl, cancelUrl));
    }

    @PostMapping("/capture")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<PaymentCaptureResponse> captureOrder(
            @Valid @RequestBody PaymentCaptureRequest request
    ) {
        return ResponseEntity.ok(
                payPalService.capturePayPalOrder(request.paypalOrderId(), request.orderId()));
    }
}