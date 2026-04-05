import api from "./api";

export const productService = {
  async getPublicProducts() {
    const { data } = await api.get("/products");
    return data;
  },

  async getAdminProducts() {
    const { data } = await api.get("/products/admin/all");
    return data;
  },

  async createProduct(payload) {
    const { data } = await api.post("/products", payload);
    return data;
  },

  async updateProduct(id, payload) {
    const { data } = await api.put(`/products/${id}`, payload);
    return data;
  },

  async deleteProduct(id) {
    await api.delete(`/products/${id}`);
  },
};
