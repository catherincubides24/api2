import api from "./api";

export const paymentService = {

  // LLAMADA 1: Le dice al backend "crea una orden en PayPal para el pedido X"
  // El backend habla con PayPal y nos devuelve una URL de aprobación
  async createPayPalOrder(orderId) {
    const returnUrl = `${window.location.origin}/payment/success`;  // a donde regresa si paga
    const cancelUrl = `${window.location.origin}/payment/cancel`;   // a donde regresa si cancela

    const { data } = await api.post("/payment/paypal/create-order", null, {
      params: { orderId, returnUrl, cancelUrl },
    });
    return data; // devuelve { paypalOrderId, approvalUrl, status }
  },

  // LLAMADA 2: Le dice al backend "el usuario aprobó, cobra el dinero"
  async capturePayPalOrder(paypalOrderId, orderId) {
    const { data } = await api.post("/payment/paypal/capture", {
      paypalOrderId,  // ID que dio PayPal
      orderId,        // ID de tu pedido interno
    });
    return data;
  },
};