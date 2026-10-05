import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  timeout: 10000,
});

let sessionInvalidHandler = null;
export const onSessionInvalid = (handler) => {
  sessionInvalidHandler = handler;
};

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("petshop_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const code = error.response?.data?.details?.code;
    if (status === 401 && code === "SESSION_REPLACED") {
      sessionInvalidHandler?.();
    }
    return Promise.reject(error);
  }
);

export default api;