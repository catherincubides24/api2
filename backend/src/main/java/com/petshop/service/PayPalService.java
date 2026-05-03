package com.petshop.service;

import com.petshop.dto.payment.PaymentCaptureResponse;
import com.petshop.dto.payment.PaymentOrderResponse;
import com.petshop.entity.OrderStatus;
import com.petshop.entity.PetOrder;
import com.petshop.exception.BadRequestException;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.PetOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalService {

    private final RestClient payPalRestClient;
    private final PetOrderRepository petOrderRepository;

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 1: Crear orden en PayPal
    // ─────────────────────────────────────────────────────────────────────────

    public PaymentOrderResponse createPayPalOrder(Long orderId, String returnUrl, String cancelUrl) {

        PetOrder petOrder = petOrderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id " + orderId));

        if (petOrder.getStatus() == OrderStatus.PAID || petOrder.getStatus() == OrderStatus.SHIPPED) {
            throw new BadRequestException("Este pedido ya fue pagado o está en camino.");
        }

        String accessToken = getAccessToken();

        // Monto con dos decimales
        String amount = petOrder.getTotalAmount()
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .toPlainString();

        // Cuerpo de la orden PayPal (estructura REST v2)
        Map<String, Object> orderBody = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(
                        Map.of(
                                "reference_id", String.valueOf(orderId),
                                "description", "Pedido Huellitas Shop #" + orderId,
                                "amount", Map.of(
                                        "currency_code", "USD",
                                        "value", amount
                                )
                        )
                ),
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

            // Extraer el approvalUrl de los links
            @SuppressWarnings("unchecked")
            List<Map<String, String>> links = (List<Map<String, String>>) response.get("links");

            String approvalUrl = links.stream()
                    .filter(link -> "approve".equals(link.get("rel")))
                    .map(link -> link.get("href"))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("PayPal no devolvió URL de aprobación."));

            log.info("Orden PayPal creada: {} para pedido interno: {}", paypalOrderId, orderId);
            return new PaymentOrderResponse(paypalOrderId, approvalUrl, status);

        } catch (RestClientException e) {
            log.error("Error al crear orden PayPal para pedido {}: {}", orderId, e.getMessage());
            throw new BadRequestException("Error al conectar con PayPal: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 2: Capturar el pago luego de que el usuario aprueba en PayPal
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentCaptureResponse capturePayPalOrder(String paypalOrderId, Long orderId) {

        String accessToken = getAccessToken();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = payPalRestClient.post()
                    .uri("/v2/checkout/orders/{id}/capture", paypalOrderId)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of()) // body vacío requerido por PayPal
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new BadRequestException("PayPal no devolvió respuesta al capturar el pago.");
            }

            String status = (String) response.get("status");
            log.info("Captura PayPal - Orden: {}, Estado: {}", paypalOrderId, status);

            if ("COMPLETED".equals(status)) {
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

        } catch (RestClientException e) {
            log.error("Error al capturar pago PayPal {}: {}", paypalOrderId, e.getMessage());
            throw new BadRequestException("Error al capturar el pago de PayPal: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Método privado: obtener access token de PayPal (OAuth2 Client Credentials)
    // ─────────────────────────────────────────────────────────────────────────

    private String getAccessToken() {
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

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
                throw new BadRequestException("No se pudo obtener el token de acceso de PayPal.");
            }

            return (String) response.get("access_token");

        } catch (RestClientException e) {
            log.error("Error al obtener token PayPal: {}", e.getMessage());
            throw new BadRequestException("Error de autenticación con PayPal: " + e.getMessage());
        }
    }
}