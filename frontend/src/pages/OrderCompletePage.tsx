import { useEffect } from "react";
import { logger } from "../lib/logging";
import { navigate } from "../router/Router";

interface Props {
  orderNumber: string | null;
}

/**
 * 基本設計書 3.1 / 5.2:
 *   注文(完了) - 完了表示。orderNumber をクエリから受け取り、控えとして表示する。
 *   API は叩かない (確定は /orders/confirm 側で完了済み)。
 */
export function OrderCompletePage({ orderNumber }: Props) {
  // orderNumber が無い (URL 直叩き等) ならトップへ戻す
  useEffect(() => {
    if (!orderNumber) {
      navigate("/");
      return;
    }
    logger.info({
      event: "ORDER_COMPLETE_VIEWED",
      message: "order complete page viewed",
      context: { orderNumber },
    });
  }, [orderNumber]);

  if (!orderNumber) return null;

  return (
    <div className="top-shell">
      <header className="top-header">
        <a href="#/" className="top-brand" style={{ textDecoration: "none" }}>
          SakeEC
        </a>
      </header>

      <main className="top-main">
        <div className="card order-complete">
          <div className="order-complete-icon" aria-hidden="true">✓</div>
          <h1 className="order-complete-title">ご注文ありがとうございました</h1>
          <p className="muted" style={{ margin: 0 }}>
            ご注文を承りました。確認メールをお送りしますので、内容をご確認ください。
          </p>

          <div className="order-complete-number">
            <span className="muted">注文番号</span>
            <strong>{orderNumber}</strong>
          </div>

          <p className="muted" style={{ margin: 0, fontSize: 13 }}>
            お支払いは商品到着時の代引きとなります。<br />
            お問い合わせの際は、上記の注文番号をお伝えください。
          </p>

          <div className="form-actions" style={{ justifyContent: "center" }}>
            <a href="#/" className="btn btn-primary">
              トップへ戻る
            </a>
          </div>
        </div>
      </main>
    </div>
  );
}
