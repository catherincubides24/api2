package com.petshop.service;

import com.petshop.dto.payment.PaymentCaptureResponse;
import com.petshop.dto.payment.PaymentOrderResponse;
import com.petshop.entity.OrderStatus;
import com.petshop.entity.PaymentMethod;
import com.petshop.entity.PetOrder;
import com.petshop.event.OrderPaidEvent;
import com.petshop.exception.BadRequestException;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.PetOrderRepository;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalService {

    private final RestClient payPalRestClient;
    private final PetOrderRepository petOrderRepository;
    private final ApplicationEventPublisher events;

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ─────────────────────────────────────────────────────────────────────
    // PASO 1: Crear orden en PayPal
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentOrderResponse createPayPalOrder(Long orderId) {
        PetOrder petOrder = findOrder(orderId);

        if (petOrder.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Solo se pueden pagar pedidos pendientes.");
        }

        // Las URLs las define el servidor, nunca el cliente (evita open redirect).
        String returnUrl = frontendUrl + "/payment/success";
        String cancelUrl = frontendUrl + "/payment/cancel";

        String accessToken = getAccessToken();
        String amount = petOrder.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();

        Map<String, Object> orderBody = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "reference_id", String.valueOf(orderId),
                        "description", "Pedido Huellitas Shop #" + orderId,
                        "amount", Map.of("currency_code", "USD", "value", amount)
                )),
                "application_context", Map.of(
                        "brand_name", "Huellitas Shop",
                        "landing_page", "BILLING",
                        "user_action", "PAY_NOW",
                        "return_url", returnUrl,
                        "cancel_url", cancelUrl
                )
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = payPalRestClient.post()
                    .uri("/v2/checkout/orders")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(orderBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new BadRequestException("PayPal no devolvió respuesta al crear la orden.");
            }

            String paypalOrderId = (String) response.get("id");
            String status = (String) response.get("status");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> links = (List<Map<String, String>>) response.get("links");

            String approvalUrl = links.stream()
                    .filter(link -> "approve".equals(link.get("rel")))
                    .map(link -> link.get("href"))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("PayPal no devolvió URL de aprobación."));

            // Vínculo pedido <-> orden PayPal (se verifica al capturar)
            petOrder.setPaypalOrderId(paypalOrderId);
            petOrderRepository.save(petOrder);

            log.info("Orden PayPal {} creada para pedido {}", paypalOrderId, orderId);
            return new PaymentOrderResponse(paypalOrderId, approvalUrl, status);

        } catch (RestClientException e) {
            log.error("Error al crear orden PayPal para pedido {}: {}", orderId, e.getMessage());
            throw new BadRequestException("No se pudo iniciar el pago con PayPal.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PASO 2: Capturar el pago luego de que el usuario aprueba en PayPal
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentCaptureResponse capturePayPalOrder(String paypalOrderId, Long orderId) {
        PetOrder petOrder = findOrder(orderId);

        // El pago de PayPal debe pertenecer a ESTE pedido.
        if (!paypalOrderId.equals(petOrder.getPaypalOrderId())) {
            throw new BadRequestException("La orden de PayPal no corresponde a este pedido.");
        }

        // Idempotencia: si ya está pagado no se captura de nuevo.
        if (petOrder.getStatus() == OrderStatus.PAID) {
            return new PaymentCaptureResponse(paypalOrderId, "COMPLETED", orderId, "El pedido ya estaba pagado.");
        }
        if (petOrder.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("El pedido no está en un estado pagable.");
        }

        String accessToken = getAccessToken();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = payPalRestClient.post()
                    .uri("/v2/checkout/orders/{id}/capture", paypalOrderId)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new BadRequestException("PayPal no devolvió respuesta al capturar el pago.");
            }

            String status = (String) response.get("status");
            log.info("Captura PayPal {} -> {}", paypalOrderId, status);

            if ("COMPLETED".equals(status)) {
                petOrder.setStatus(OrderStatus.PAID);
                petOrder.setPaymentMethod(PaymentMethod.PAYPAL);
                petOrderRepository.save(petOrder);
                events.publishEvent(new OrderPaidEvent(petOrder.getId())); // Observer

                return new PaymentCaptureResponse(
                        paypalOrderId, "COMPLETED", orderId,
                        "Pago completado exitosamente. Pedido #" + orderId + " confirmado.");
            }

            return new PaymentCaptureResponse(
                    paypalOrderId, status, orderId, "El pago no fue completado. Estado: " + status);

        } catch (RestClientException e) {
            log.error("Error al capturar pago PayPal {}: {}", paypalOrderId, e.getMessage());
            throw new BadRequestException("No se pudo confirmar el pago con PayPal.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private PetOrder findOrder(Long orderId) {
        return petOrderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id " + orderId));
    }

    private String getAccessToken() {
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = payPalRestClient.post()
                    .uri("/v1/oauth2/token")
                    .header("Authorization", "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new BadRequestException("No se pudo autenticar con PayPal.");
            }
            return (String) response.get("access_token");

        } catch (RestClientException e) {
            log.error("Error al obtener token PayPal: {}", e.getMessage());
            throw new BadRequestException("No se pudo autenticar con PayPal.");
        }
    }
}