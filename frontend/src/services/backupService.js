import api from "./api";

export const backupService = {
  /**
   * Obtiene la información y métricas de la base de datos (conteo de tablas, versión).
   */
  async getDatabaseInfo() {
    const { data } = await api.get("/backup/info");
    return data;
  },

  /**
   * Descarga la copia de seguridad completa en formato SQL.
   */
  async downloadBackup() {
    const response = await api.get("/backup/download", {
      responseType: "blob",
      timeout: 120000,
    });
    return response;
  },

  /**
   * Envía un archivo .sql al servidor para ejecutar la restauración de la base de datos.
   */
  async restoreBackup(file) {
    const formData = new FormData();
    formData.append("file", file);

    const { data } = await api.post("/backup/restore", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      timeout: 180000,
    });
    return data;
  },
};
