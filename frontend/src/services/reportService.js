import api from "./api";

const clean = (filters) =>
  Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== "" && value != null)
  );

export const reportService = {
  async getSummary(filters) {
    const { data } = await api.get("/reports/summary", { params: clean(filters) });
    return data;
  },

  async download(format, filters) {
    const response = await api.get("/reports/export", {
      params: { ...clean(filters), format },
      responseType: "blob",
      timeout: 60000,
    });
    return response.data;
  },
};