import api from "./api";

export const authService = {
  async login(credentials) {
    const { data } = await api.post("/auth/login", credentials);
    return data;
  },

  async register(payload) {
    const { data } = await api.post("/auth/register", payload);
    return data;
  },

  async logout() {
    await api.post("/auth/logout");
  },

  async heartbeat() {
    await api.post("/auth/heartbeat");
  },
};