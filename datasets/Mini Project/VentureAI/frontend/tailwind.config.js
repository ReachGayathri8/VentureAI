/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        navy: {
          50: '#eef1f8',
          100: '#d7ddee',
          200: '#aebadd',
          300: '#8697cb',
          400: '#5d74ba',
          500: '#3b5299',
          600: '#2c3e78',
          700: '#1f2c58',
          800: '#141d3d',
          900: '#0b1226',
          950: '#080d1a',
        },
        brand: {
          red: '#c81e2d',
          redDark: '#a5121f',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        card: '0 1px 3px 0 rgba(15, 23, 42, 0.08), 0 1px 2px -1px rgba(15, 23, 42, 0.06)',
        cardHover: '0 10px 25px -5px rgba(15, 23, 42, 0.12), 0 8px 10px -6px rgba(15, 23, 42, 0.08)',
      },
      borderRadius: {
        xl2: '1.25rem',
      },
    },
  },
  plugins: [],
};
