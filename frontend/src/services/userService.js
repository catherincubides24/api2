import api from "./api";

// Solo accesible para administradores (ver UserController en el backend).
export const userService = {
  async getAllUsers() {
    const { data } = await api.get("/users");
    return data;
  },
};
