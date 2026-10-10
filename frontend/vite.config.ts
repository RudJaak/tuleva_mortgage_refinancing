/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig, type ProxyOptions } from 'vite'

// The browser only talks to Vite, so the proxied hop is same-origin. Dropping Origin keeps the
// backend's CORS check from rejecting POSTs when the app is opened by LAN address (host: true).
const backend: ProxyOptions = {
  target: 'http://localhost:9000',
  configure: (proxy) => proxy.on('proxyReq', (request) => request.removeHeader('origin')),
}

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/v1': backend,
      '/actuator': backend,
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        quietDeps: true,
        silenceDeprecations: ['import', 'global-builtin', 'color-functions', 'if-function'],
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
