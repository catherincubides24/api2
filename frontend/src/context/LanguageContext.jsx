import { createContext, useCallback, useContext, useEffect, useState } from "react";
import en from "../i18n/en.json";
import es from "../i18n/es.json";

const LanguageContext = createContext(null);
const LANG_KEY = "petshop_language";
const translations = { es, en };

function getInitialLanguage() {
  const stored = localStorage.getItem(LANG_KEY);
  if (stored === "es" || stored === "en") return stored;
  if (typeof navigator !== "undefined" && navigator.language?.toLowerCase().startsWith("en")) {
    return "en";
  }
  return "es";
}

export function LanguageProvider({ children }) {
  const [language, setLanguage] = useState(getInitialLanguage);

  useEffect(() => {
    localStorage.setItem(LANG_KEY, language);
    document.documentElement.lang = language;
  }, [language]);

  const toggleLanguage = () => {
    setLanguage((prev) => (prev === "es" ? "en" : "es"));
  };

  /**
   * Helper de traducción con soporte para rutas anidadas "seccion.clave"
   * y reemplazos {token}.
   */
  const t = useCallback(
    (key, replacements) => {
      const keys = key.split(".");
      let val = translations[language];
      for (const k of keys) {
        val = val?.[k];
      }
      if (typeof val !== "string") {
        // Fallback al español si falta en inglés
        let fallbackVal = translations.es;
        for (const k of keys) {
          fallbackVal = fallbackVal?.[k];
        }
        if (typeof fallbackVal === "string") {
          val = fallbackVal;
        } else {
          return key;
        }
      }

      if (replacements && typeof replacements === "object") {
        return Object.entries(replacements).reduce(
          (str, [token, replacement]) => str.replace(new RegExp(`\\{${token}\\}`, "g"), replacement),
          val
        );
      }
      return val;
    },
    [language]
  );

  /**
   * Traduce categorías si existe traducción disponible
   */
  const tCategory = useCallback(
    (category) => {
      if (!category) return "";
      const translated = translations[language]?.categories?.[category];
      return translated || category;
    },
    [language]
  );

  /**
   * Traduce nombre de producto si existe traducción disponible
   */
  const tProductName = useCallback(
    (name) => {
      if (!name) return "";
      const translated = translations[language]?.products?.[name]?.name;
      return translated || name;
    },
    [language]
  );

  /**
   * Traduce descripción de producto si existe traducción disponible
   */
  const tProductDesc = useCallback(
    (name, desc) => {
      const translated = translations[language]?.products?.[name]?.description;
      return translated || desc || "";
    },
    [language]
  );

  /**
   * Formateador de fechas sensible al idioma actual
   */
  const formatDate = useCallback(
    (date, options = { dateStyle: "medium" }) => {
      if (!date) return "";
      const locale = language === "en" ? "en-US" : "es-CO";
      try {
        return new Date(date).toLocaleString(locale, {
          timeZone: "America/Bogota",
          ...options,
        });
      } catch {
        return String(date);
      }
    },
    [language]
  );

  return (
    <LanguageContext.Provider
      value={{
        language,
        setLanguage,
        toggleLanguage,
        t,
        tCategory,
        tProductName,
        tProductDesc,
        formatDate,
        isEnglish: language === "en",
      }}
    >
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error("useLanguage debe usarse dentro de LanguageProvider");
  }
  return context;
}
