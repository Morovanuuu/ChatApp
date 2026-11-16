import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Cererile /api ajung la Spring Boot: acelasi origin pentru browser, deci fara CORS
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
