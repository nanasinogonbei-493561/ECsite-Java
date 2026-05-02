import { Component, type ErrorInfo, type ReactNode } from "react";
import { logger } from "./logger";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
}

/**
 * React のレンダリング中の例外をキャッチして構造化ログに記録する。
 * fallback 未指定時は最小限のテキストを表示する。
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    logger.error({
      event: "REACT_BOUNDARY_CAUGHT",
      message: error.message,
      error,
      context: {
        componentStack: info.componentStack,
      },
    });
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return this.props.fallback ?? (
        <div role="alert" style={{ padding: 16 }}>
          <p>申し訳ありません。画面の表示中にエラーが発生しました。</p>
          <p>ページを再読み込みしてください。</p>
        </div>
      );
    }
    return this.props.children;
  }
}
