const defaultTheme = require('tailwindcss/defaultTheme')

/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      screens: {
        'electron': '1200px',  // Electron 应用的推荐断点
        'electron-lg': '1400px',  // Electron 大屏幕断点
      },
      colors: {
        // 莫兰迪配色
        morandi: {
          50: '#F8F9FA',
          100: '#F1F3F4',
          200: '#E8EAED',
          300: '#DADCE0',
          400: '#BDC1C6',
          500: '#9AA0A6',
          600: '#80868B',
          700: '#5F6368',
          800: '#3C4043',
          900: '#202124',
        },
        sage: {
          50: '#F6F8F6',
          100: '#E8F5E8',
          200: '#D3E6E2',
          300: '#B8D4CE',
          400: '#9BC2BA',
          500: '#7DB0A6',
          600: '#5F9E92',
          700: '#4A8A7E',
          800: '#3A766A',
          900: '#2A6256',
        },
        teal: {
          500: '#0FA29C',
          600: '#12C0CA',
        },
        gradient: {
          start: '#00D0D8',
          end: '#12C0CA',
        },
        // 新的蓝绿色调配色
        liteisle: {
          bg: '#DDEEEE',        // 主背景色
          sidebar: '#B9E3E3',   // 侧边栏背景
          card: '#F9FAFA',      // 卡片背景
          border: '#000000',    // 边框颜色
        }
      },
      fontFamily: {
        sans: ['"LXGW WenKai Lite"', 'sans-serif', ...defaultTheme.fontFamily.sans],
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-in-out',
        'slide-up': 'slideUp 0.3s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        }
      },
      typography: {
        DEFAULT: {
          css: {
            maxWidth: 'none',
            color: '#374151',
            p: {
              marginTop: '1.5em',
              marginBottom: '1.5em',
            },
            'h1, h2, h3': {
              color: '#111827',
              fontWeight: '600',
            },
            pre: {
              backgroundColor: '#f3f4f6',
              color: '#374151',
              padding: '1rem',
              borderRadius: '0.5rem',
              marginTop: '1.5em',
              marginBottom: '1.5em',
            },
            code: {
              color: '#374151',
              backgroundColor: '#f3f4f6',
              paddingLeft: '0.25rem',
              paddingRight: '0.25rem',
              paddingTop: '0.125rem',
              paddingBottom: '0.125rem',
              borderRadius: '0.25rem',
              '&::before': {
                content: 'none',
              },
              '&::after': {
                content: 'none',
              },
            },
            'pre code': {
              backgroundColor: 'transparent',
              borderWidth: '0',
              borderRadius: '0',
              padding: '0',
              color: 'inherit',
            },
          },
        },
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/typography'),
  ],
} 