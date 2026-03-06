import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        canvas: '#f5f7f9',
        panel: '#ffffff',
        ink: '#112235',
        brand: '#0f766e',
        accent: '#1d4ed8',
        danger: '#dc2626',
        warning: '#d97706'
      }
    }
  },
  plugins: []
} satisfies Config;
