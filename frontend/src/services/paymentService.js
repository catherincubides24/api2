import api from "./api";

export const paymentService = {
  // El backend define las URLs de retorno; el cliente solo envía el orderId.
  async createPayPalOrder(orderId) {
    const { data } = await api.post("/payment/paypal/create-order", null, {
      params: { orderId },
    });
    return data; // { paypalOrderId, approvalUrl, status }
  },

  async capturePayPalOrder(paypalOrderId, orderId) {
    const { data } = await api.post("/payment/paypal/capture", {
      paypalOrderId,
      orderId,
    });
    return data;
  },
};