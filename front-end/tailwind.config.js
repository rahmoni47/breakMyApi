/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        primary: "#3524D4",
        "primary-hover": "#2818B5",
        "bg-main": "#F7F8FF",
        surface: "#FFFFFF",
        "soft-blue": "#EEF3FF",
        "text-main": "#102039",
        "text-secondary": "#777A91",
        success: "#087C8C",
        error: "#DC2626",
        warning: "#7C3AED",
      },
      boxShadow: {
        card: "0 8px 24px rgba(16, 32, 57, 0.08)",
      },
      fontFamily: {
        sans: ["Manrope", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      borderRadius: {
        xl: "14px",
      },
      animation: {
        "fade-in": "fadeIn 0.4s ease-out",
        pulseDot: "pulseDot 1.4s ease-in-out infinite",
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: 0, transform: "translateY(6px)" },
          "100%": { opacity: 1, transform: "translateY(0)" },
        },
        pulseDot: {
          "0%, 100%": { transform: "scale(0.85)", opacity: 0.5 },
          "50%": { transform: "scale(1)", opacity: 1 },
        },
      },
    },
  },
  plugins: [],
};
