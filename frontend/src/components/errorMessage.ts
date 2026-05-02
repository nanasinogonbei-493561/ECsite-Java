import { ApiError } from "../lib/api";

/**
 * 例外をユーザ向けメッセージに変換する。
 * バックエンドの errorCode を優先的に解釈し、未知のコードはステータスから定型文を返す。
 */
export function toUserMessage(e: unknown): string {
  if (e instanceof ApiError) {
    switch (e.errorCode) {
      case "VALIDATION_ERROR":         return "入力内容に誤りがあります。";
      case "AUTH_FAILED":               return "ユーザー名またはパスワードが正しくありません。";
      case "UNAUTHORIZED":              return "ログインが必要です。";
      case "NOT_FOUND":                 return "対象が見つかりませんでした。";
      case "OUT_OF_STOCK":              return "在庫が不足しています。";
      case "INVALID_STATUS_TRANSITION": return "このステータスには変更できません。";
      case "MALFORMED_REQUEST":         return "リクエストの形式が不正です。";
      case "UNSUPPORTED_MEDIA_TYPE":    return "リクエスト形式が許可されていません。";
      case "METHOD_NOT_ALLOWED":        return "操作が許可されていません。";
    }
    if (e.status >= 500) return "サーバーで問題が発生しました。時間をおいて再度お試しください。";
    if (e.status === 401) return "セッションの有効期限が切れました。再度ログインしてください。";
    return "操作に失敗しました。";
  }
  return "通信に失敗しました。ネットワーク接続をご確認ください。";
}

export function isUnauthorized(e: unknown): boolean {
  return e instanceof ApiError && e.status === 401;
}
