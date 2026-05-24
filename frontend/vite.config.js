import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/auth': {
        target: 'http://localhost:1001',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:1001',
        changeOrigin: true,
      },
      '/sar': {
        target: 'http://localhost:1001',
        changeOrigin: true,
      },
    },
  },
})
