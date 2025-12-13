import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Optional but recommended for tunnels: bind to all interfaces
    host: true,

    // This fixes the "Blocked request" error for your custom domain
    allowedHosts: [
      'winvestco.in',
      'www.winvestco.in',
      // You can add more subdomains here if needed
    ],

    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})