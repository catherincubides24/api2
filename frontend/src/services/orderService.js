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

  async getAllOrders() {
    const { data } = await api.get("/orders");
    return data;
  },

  async getOrdersByUser(userId) {
    const { data } = await api.get(`/orders/user/${userId}`);
    return data;
  },
};
