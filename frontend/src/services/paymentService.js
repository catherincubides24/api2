import api from "./api";

export const paymentService = {
  async createPayPalOrder(orderId) {
    const returnUrl = `${window.location.origin}/payment/success`;
    const cancelUrl = `${window.location.origin}/payment/cancel`;

    const { data } = await api.post("/payment/paypal/create-order", null, {
      params: { orderId, returnUrl, cancelUrl },
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