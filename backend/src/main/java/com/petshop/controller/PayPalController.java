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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/paypal")
@RequiredArgsConstructor
public class PayPalController {

    private final PayPalService payPalService;

    /**
     * POST /api/payment/paypal/create-order?orderId=X
     * Crea la orden en PayPal y devuelve el approvalUrl para redirigir al usuario.
     */
    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @RequestParam @NotNull Long orderId,
            @RequestParam(defaultValue = "http://localhost:5173/payment/success") String returnUrl,
            @RequestParam(defaultValue = "http://localhost:5173/payment/cancel") String cancelUrl
    ) {
        PaymentOrderResponse response = payPalService.createPayPalOrder(orderId, returnUrl, cancelUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payment/paypal/capture
     * Captura el pago luego de la aprobación del usuario en PayPal.
     */
    @PostMapping("/capture")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<PaymentCaptureResponse> captureOrder(
            @Valid @RequestBody PaymentCaptureRequest request
    ) {
        PaymentCaptureResponse response = payPalService.capturePayPalOrder(
                request.paypalOrderId(),
                request.orderId()
        );
        return ResponseEntity.ok(response);
    }
}