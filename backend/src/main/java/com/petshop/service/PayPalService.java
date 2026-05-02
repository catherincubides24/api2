package com.petshop.service;

import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.paypal.core.PayPalHttpClient;
import com.petshop.dto.payment.PaymentCaptureResponse;
import com.petshop.dto.payment.PaymentOrderResponse;
import com.petshop.entity.OrderStatus;
import com.petshop.entity.PetOrder;
import com.petshop.exception.BadRequestException;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.PetOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalService {

    private final PayPalHttpClient payPalHttpClient;
    private final PetOrderRepository petOrderRepository;

    /**
     * Crea una orden en PayPal y devuelve el URL de aprobación.
     * El frontend redirige al usuario a ese URL para que pague.
     */
    public PaymentOrderResponse createPayPalOrder(Long orderId, String returnUrl, String cancelUrl) {
        // Buscar el pedido interno
        PetOrder petOrder = petOrderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id " + orderId));

        if (petOrder.getStatus() == OrderStatus.PAID || petOrder.getStatus() == OrderStatus.SHIPPED) {
            throw new BadRequestException("Este pedido ya fue pagado o está en camino.");
        }

        // Construir la orden de PayPal
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        // Convertimos el total a String con dos decimales (asumiendo USD)
        String amountValue = petOrder.getTotalAmount()
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .toPlainString();

        // Crear AmountWithBreakdown correctamente
        AmountWithBreakdown amount = new AmountWithBreakdown()
                .currencyCode("USD")
                .value(amountValue);

        // Construir PurchaseUnitRequest con el amount
        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .referenceId(String.valueOf(orderId))
                .description("Pedido Huellitas Shop #" + orderId)
                .amount(amount);   // <--- CORRECCIÓN CLAVE

        orderRequest.purchaseUnits(List.of(purchaseUnit));

        // URLs de retorno
        ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .brandName("Huellitas Shop")
                .landingPage("BILLING")
                .userAction("PAY_NOW");

        orderRequest.applicationContext(applicationContext);

        OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);

        try {
            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            // Extraer el URL de aprobación de los links de la respuesta
            String approvalUrl = order.links().stream()
                    .filter(link -> "approve".equals(link.rel()))
                    .findFirst()
                    .map(LinkDescription::href)
                    .orElseThrow(() -> new BadRequestException("No se pudo obtener el URL de aprobación de PayPal"));

            log.info("Orden PayPal creada: {} para pedido interno: {}", order.id(), orderId);
            return new PaymentOrderResponse(order.id(), approvalUrl, order.status());

        } catch (IOException e) {
            log.error("Error al crear orden PayPal para pedido {}: {}", orderId, e.getMessage());
            throw new BadRequestException("Error al conectar con PayPal: " + e.getMessage());
        }
    }

    /**
     * Captura el pago después de que el usuario aprueba en PayPal.
     * Actualiza el estado del pedido interno a PAID.
     */
    @Transactional
    public PaymentCaptureResponse capturePayPalOrder(String paypalOrderId, Long orderId) {
        OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
        request.requestBody(new OrderRequest());

        try {
            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order capturedOrder = response.result();

            String status = capturedOrder.status();
            log.info("Captura PayPal - Orden: {}, Estado: {}", paypalOrderId, status);

            if ("COMPLETED".equals(status)) {
                // Actualizar estado del pedido interno
                PetOrder petOrder = petOrderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + orderId));

                petOrder.setStatus(OrderStatus.PAID);
                petOrderRepository.save(petOrder);

                return new PaymentCaptureResponse(
                        paypalOrderId,
                        "COMPLETED",
                        orderId,
                        "Pago completado exitosamente. Pedido #" + orderId + " confirmado."
                );
            } else {
                return new PaymentCaptureResponse(
                        paypalOrderId,
                        status,
                        orderId,
                        "El pago no fue completado. Estado: " + status
                );
            }

        } catch (IOException e) {
            log.error("Error al capturar pago PayPal {}: {}", paypalOrderId, e.getMessage());
            throw new BadRequestException("Error al capturar el pago de PayPal: " + e.getMessage());
        }
    }
}