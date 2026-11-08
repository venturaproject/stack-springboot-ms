import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'path'

const backendTarget = process.env.VITE_BACKEND_URL ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [tailwindcss(), react()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    // Reached through nginx; the Host header is localhost:<published port>.
    allowedHosts: true,
    proxy: {
      '/api': { target: backendTarget, changeOrigin: true },
    },
    // Bind-mounted source on Docker Desktop needs polling for HMR. pnpm keeps its
    // content-addressed store at frontend/.pnpm-store (tens of thousands of files) — polling
    // that blocks Vite's event loop for seconds at a time, so it must be ignored explicitly
    // (Vite only ignores node_modules/.git by default).
    watch: {
      usePolling: true,
      ignored: ['**/.pnpm-store/**', '**/.git/**', '**/dist/**', '**/coverage/**'],
    },
  },
  // Don't let the dep scanner wander outside the real entry (e.g. into *.test files).
  optimizeDeps: {
    entries: ['index.html'],
  },
  build: {
    outDir: path.resolve(import.meta.dirname, './dist'),
    emptyOutDir: true,
    // Screens are code-split by route (see app-routes.tsx). Here we only pull the
    // always-loaded vendor code into stable, long-cached chunks so an app-code change
    // doesn't invalidate React/Radix/etc. Heavy single-use libs (recharts, …) are left to
    // land in the route chunk that imports them.
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (/[\\/](react|react-dom|react-router|react-router-dom|scheduler)[\\/]/.test(id)) return 'react-vendor'
          if (id.includes('@radix-ui')) return 'radix-vendor'
          if (id.includes('@tanstack')) return 'tanstack-vendor'
          return undefined
        },
      },
    },
  },
})
