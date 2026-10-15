import api from "./api";

// Solo accesible para administradores (ver AuditLogController en el backend).
export const logService = {
  async getLogs(page = 0, size = 15) {
    const { data } = await api.get("/logs", { params: { page, size } });
    return data;
  },
};