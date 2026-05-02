package com.petshop.dto.payment;

public record PaymentOrderResponse(
        String paypalOrderId,
        String approvalUrl,
        String status
) {}