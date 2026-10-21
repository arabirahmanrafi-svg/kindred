import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server on :5173 forwards API calls to Spring Boot on :8080.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
  },
})
