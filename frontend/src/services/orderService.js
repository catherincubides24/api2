import api from "./api";

export const orderService = {
  async createOrder(payload) {
    const { data } = await api.post("/orders", payload);
    return data;
  },

  async updateOrderStatus(id, status) {
    const { data } = await api.patch(`/orders/${id}/status`, { status });
    return data;
  },

  async updatePayment(id, paymentMethod, markAsPaid = true) {
    const { data } = await api.patch(`/orders/${id}/payment`, {
      paymentMethod,
      markAsPaid,
    });
    return data;
  },

  async getAllOrders(page = 0, size = 10) {
    const { data } = await api.get("/orders", { params: { page, size } });
    return data;
  },

  async getOrdersByUser(userId, page = 0, size = 10) {
    const { data } = await api.get(`/orders/user/${userId}`, {
      params: { page, size },
    });
    return data;
  },

  async getOrderById(id) {
    const { data } = await api.get(`/orders/${id}`);
    return data;
  },

  async getTicket(id) {
    const { data } = await api.get(`/orders/${id}/ticket`);
    return data;
  },
};
