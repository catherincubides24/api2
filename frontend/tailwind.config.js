/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        cream: "#fffaf3",
        sand: "#f6ecdd",
        ink: "#1d2838",
        dusk: "#2c4b66",
        coral: "#ff6b4a",
        mint: "#18b5a4",
        peach: "#ffd7b5",
      },
      fontFamily: {
        display: ["'DM Serif Display'", "serif"],
        body: ["'Sora'", "sans-serif"],
      },
      boxShadow: {
        card: "0 20px 45px -30px rgba(21, 41, 64, 0.45)",
        soft: "0 12px 28px -18px rgba(10, 30, 48, 0.35)",
      },
      keyframes: {
        float: {
          "0%, 100%": { transform: "translateY(0px)" },
          "50%": { transform: "translateY(-10px)" },
        },
        rise: {
          "0%": { opacity: "0", transform: "translateY(18px)" },
          "100%": { opacity: "1", transform: "translateY(0px)" },
        },
      },
      animation: {
        float: "float 6s ease-in-out infinite",
        rise: "rise 0.7s ease-out both",
      },
    },
  },
  plugins: [],
};
