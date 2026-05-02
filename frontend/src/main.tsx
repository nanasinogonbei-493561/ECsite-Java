import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import {
  ErrorBoundary,
  installGlobalLogHandlers,
  logger,
} from "./lib/logging";

// 起動の最初に呼ぶ: window.onerror / unhandledrejection を構造化ログ化
installGlobalLogHandlers();

const container = document.getElementById("root")!;

createRoot(container, {
  // React 19 で追加された 3 つのフック。すべて構造化ログに集約する。
  onCaughtError: (error, errorInfo) => {
    logger.error({
      event: "REACT_BOUNDARY_CAUGHT",
      message: error instanceof Error ? error.message : String(error),
      error,
      context: { componentStack: errorInfo.componentStack },
    });
  },
  onUncaughtError: (error, errorInfo) => {
    logger.fatal({
      event: "UNCAUGHT_ERROR",
      message: error instanceof Error ? error.message : String(error),
      error,
      context: { componentStack: errorInfo.componentStack },
    });
  },
  onRecoverableError: (error, errorInfo) => {
    logger.warn({
      event: "REACT_RECOVERABLE_ERROR",
      message: error instanceof Error ? error.message : String(error),
      error,
      context: { componentStack: errorInfo.componentStack },
    });
  },
}).render(
  <StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </StrictMode>,
);
