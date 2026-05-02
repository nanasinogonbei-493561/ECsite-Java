import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // バックエンド (Spring Boot, http://localhost:8080) へリバースプロキシ。
    // これにより同一オリジンとなり、CORS 設定なしで /api を叩け、
    // X-Trace-Id ヘッダもそのまま転送される。
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/health": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
