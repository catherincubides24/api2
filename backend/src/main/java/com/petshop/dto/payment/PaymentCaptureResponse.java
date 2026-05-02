package com.petshop.dto.payment;

public record PaymentCaptureResponse(
        String paypalOrderId,
        String status,
        Long orderId,
        String message
) {}