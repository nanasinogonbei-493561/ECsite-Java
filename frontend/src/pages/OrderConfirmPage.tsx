import { useEffect, useMemo, useState } from "react";
import { ApiError, createOrder } from "../lib/api";
import { logger } from "../lib/logging";
import { navigate } from "../router/Router";
import { toUserMessage } from "../components/errorMessage";
import { clearOrderDraft, loadOrderDraft, type OrderDraft } from "../lib/orderDraft";

/**
 * 基本設計書 3.1 / 5.2:
 *   注文(確認) - 合計額表示。確定すると POST /api/orders。
 *   成功時 → /orders/complete?orderNumber=...
 *   在庫不足 (OUT_OF_STOCK) は明示メッセージを出して入力画面に戻す。
 */
export function OrderConfirmPage() {
  const [draft, setDraft] = useState<OrderDraft | null>(() => loadOrderDraft());
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // draft が無い (直接 URL 叩き / セッション切れ) ならトップへ戻す
  useEffect(() => {
    if (!draft) {
      navigate("/");
    }
  }, [draft]);

  const total = useMemo(() => {
    if (!draft) return 0;
    return toNumber(draft.product.price) * draft.quantity;
  }, [draft]);

  if (!draft) return null;

  const onConfirm = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const res = await createOrder({
        items: [{ productId: draft.product.id, quantity: draft.quantity }],
        customer: {
          name: draft.customer.name,
          email: draft.customer.email,
          phone: draft.customer.phone || undefined,
          deliveryAddress: draft.customer.deliveryAddress,
        },
      });
      logger.info({
        event: "ORDER_CREATED",
        message: "order created",
        context: {
          orderNumber: res.orderNumber,
          productId: draft.product.id,
          quantity: draft.quantity,
        },
      });
      clearOrderDraft();
      navigate(`/orders/complete?orderNumber=${encodeURIComponent(res.orderNumber)}`);
    } catch (e) {
      // 在庫不足は入力画面に戻すのが親切なのでメッセージを差し替える
      if (e instanceof ApiError && e.errorCode === "OUT_OF_STOCK") {
        setError("申し訳ありません、在庫が不足しています。数量を変更してください。");
      } else {
        setError(toUserMessage(e));
      }
    } finally {
      setSubmitting(false);
    }
  };

  const backHref = `#/orders/new?productId=${draft.product.id}&qty=${draft.quantity}`;

  return (
    <div className="top-shell">
      <header className="top-header">
        <a href="#/" className="top-brand" style={{ textDecoration: "none" }}>
          SakeEC
        </a>
      </header>

      <main className="top-main">
        <p className="muted" style={{ marginTop: 0 }}>
          <a href={backHref}>← 入力画面へ戻る</a>
        </p>

        <h1 className="top-title">注文内容の確認</h1>
        <p className="muted" style={{ marginTop: -8 }}>
          以下の内容で注文を確定します。よろしければ「注文を確定する」を押してください。
        </p>

        <div className="order-form">
          <section className="card">
            <h3>ご注文商品</h3>
            <div className="order-product-row">
              {draft.product.imageUrl ? (
                <img src={draft.product.imageUrl} alt="" className="order-product-thumb" />
              ) : (
                <div
                  className="order-product-thumb order-product-thumb--placeholder"
                  aria-hidden="true"
                >
                  画像なし
                </div>
              )}
              <div>
                <p className="order-product-name">{draft.product.name}</p>
                <p className="muted" style={{ margin: 0 }}>
                  単価 {formatYen(draft.product.price)}
                  {draft.product.volume != null && ` / ${draft.product.volume}ml`}
                </p>
                <p className="muted" style={{ margin: 0 }}>
                  数量 {draft.quantity} 本
                </p>
              </div>
            </div>
          </section>

          <section className="card">
            <h3>お客様情報</h3>
            <dl className="confirm-list">
              <dt>お名前</dt>
              <dd>{draft.customer.name}</dd>
              <dt>メールアドレス</dt>
              <dd>{draft.customer.email}</dd>
              <dt>電話番号</dt>
              <dd>{draft.customer.phone || "—"}</dd>
              <dt>お届け先住所</dt>
              <dd style={{ whiteSpace: "pre-wrap" }}>{draft.customer.deliveryAddress}</dd>
            </dl>
          </section>

          <section className="card">
            <h3>お支払い方法</h3>
            <p style={{ margin: 0 }}>代引き (商品到着時にお支払い)</p>
          </section>

          <section className="card order-total">
            <span>合計金額</span>
            <strong>{formatYen(total)}</strong>
          </section>

          {error && (
            <p role="alert" className="error-text">
              {error}
            </p>
          )}

          <div className="form-actions">
            <a href={backHref} className="btn btn-secondary">
              修正する
            </a>
            <button
              type="button"
              className="btn btn-primary"
              onClick={onConfirm}
              disabled={submitting}
            >
              {submitting ? "送信中…" : "注文を確定する"}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}

function toNumber(v: string | number): number {
  const n = typeof v === "string" ? Number(v) : v;
  return Number.isFinite(n) ? n : 0;
}

function formatYen(value: string | number): string {
  return `¥${toNumber(value).toLocaleString("ja-JP")}`;
}
