import { useEffect, useMemo, useState, type FormEvent } from "react";
import { ApiError, getProduct, type ProductResponse } from "../lib/api";
import { logger } from "../lib/logging";
import { navigate } from "../router/Router";
import { toUserMessage } from "../components/errorMessage";
import { loadOrderDraft, saveOrderDraft } from "../lib/orderDraft";

interface Props {
  productId: number;
  initialQuantity: number;
}

const MIN_QTY = 1;
const MAX_QTY = 3;
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * 基本設計書 3.1 / 5.2:
 *   注文(入力) - 顧客情報・数量入力。確定は /orders/confirm 側。
 *   ここでは API は叩かず、入力内容を sessionStorage に保存して遷移する。
 */
export function OrderNewPage({ productId, initialQuantity }: Props) {
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  // フォーム状態。初期値は draft 復元 → クエリ → 既定値の順。
  const draft = useMemo(() => loadOrderDraft(), []);
  const [quantity, setQuantity] = useState<number>(() => {
    if (draft?.product.id === productId) return draft.quantity;
    return clampQuantity(initialQuantity);
  });
  const [name, setName] = useState(draft?.customer.name ?? "");
  const [email, setEmail] = useState(draft?.customer.email ?? "");
  const [phone, setPhone] = useState(draft?.customer.phone ?? "");
  const [deliveryAddress, setDeliveryAddress] = useState(
    draft?.customer.deliveryAddress ?? "",
  );
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setLoadError(null);
    setNotFound(false);
    getProduct(productId)
      .then((p) => {
        if (cancelled) return;
        setProduct(p);
        // 在庫上限が 3 未満なら quantity を寄せる
        const cap = Math.min(MAX_QTY, Math.max(MIN_QTY, p.stockQuantity));
        setQuantity((q) => Math.min(q, cap || MIN_QTY));
      })
      .catch((e) => {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 404) {
          setNotFound(true);
        } else {
          setLoadError(toUserMessage(e));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [productId]);

  const stock = product?.stockQuantity ?? 0;
  const outOfStock = product != null && stock <= 0;
  const maxSelectable = Math.min(MAX_QTY, Math.max(MIN_QTY, stock));
  const total = product
    ? toNumber(product.price) * quantity
    : 0;

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!product) return;

    const validation = validateInputs({
      quantity,
      stock,
      name,
      email,
      deliveryAddress,
    });
    if (validation) {
      setSubmitError(validation);
      logger.warn({
        event: "ORDER_INPUT_INVALID",
        message: validation,
        context: { productId: product.id, quantity },
      });
      return;
    }

    saveOrderDraft({
      product,
      quantity,
      customer: {
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim(),
        deliveryAddress: deliveryAddress.trim(),
      },
    });
    logger.info({
      event: "ORDER_INPUT_SUBMITTED",
      message: "order draft saved",
      context: { productId: product.id, quantity },
    });
    navigate("/orders/confirm");
  };

  return (
    <div className="top-shell">
      <header className="top-header">
        <a href="#/" className="top-brand" style={{ textDecoration: "none" }}>
          SakeEC
        </a>
      </header>

      <main className="top-main">
        <p className="muted" style={{ marginTop: 0 }}>
          <a href={product ? `#/products/${product.id}` : "#/"}>← 商品ページへ戻る</a>
        </p>

        <h1 className="top-title">注文情報の入力</h1>

        {loading && <p className="muted">読み込み中…</p>}
        {loadError && <p role="alert" className="error-text">{loadError}</p>}
        {notFound && (
          <div role="alert">
            <h2>商品が見つかりません</h2>
            <p className="muted">URL が間違っているか、商品が削除された可能性があります。</p>
          </div>
        )}

        {product && (
          <form className="order-form" onSubmit={onSubmit} aria-label="注文入力">
            <section className="card order-product">
              <h3>ご注文商品</h3>
              <div className="order-product-row">
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt="" className="order-product-thumb" />
                ) : (
                  <div className="order-product-thumb order-product-thumb--placeholder" aria-hidden="true">
                    画像なし
                  </div>
                )}
                <div>
                  <p className="order-product-name">{product.name}</p>
                  <p className="muted" style={{ margin: 0 }}>
                    単価 {formatYen(product.price)}
                    {product.volume != null && ` / ${product.volume}ml`}
                  </p>
                  <p className="muted" style={{ margin: 0 }}>
                    在庫 {stock} 本
                  </p>
                </div>
              </div>
            </section>

            <section className="card">
              <h3>数量</h3>
              <label className="field" style={{ maxWidth: 180 }}>
                <span>数量 ({MIN_QTY}〜{MAX_QTY})</span>
                <select
                  value={quantity}
                  onChange={(e) => setQuantity(Number(e.target.value))}
                  disabled={outOfStock}
                >
                  {Array.from({ length: maxSelectable }, (_, i) => i + 1).map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </label>
              {outOfStock && (
                <p role="alert" className="error-text">在庫がありません。</p>
              )}
            </section>

            <section className="card">
              <h3>お客様情報</h3>
              <label className="field">
                <span>お名前 <span className="required-mark">*</span></span>
                <input
                  type="text"
                  autoComplete="name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>メールアドレス <span className="required-mark">*</span></span>
                <input
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>電話番号</span>
                <input
                  type="tel"
                  autoComplete="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="090-1234-5678"
                />
              </label>
              <label className="field">
                <span>お届け先住所 <span className="required-mark">*</span></span>
                <textarea
                  rows={3}
                  autoComplete="street-address"
                  value={deliveryAddress}
                  onChange={(e) => setDeliveryAddress(e.target.value)}
                  required
                />
              </label>
            </section>

            <section className="card order-total">
              <span>合計金額</span>
              <strong>{formatYen(total)}</strong>
            </section>

            {submitError && <p role="alert" className="error-text">{submitError}</p>}

            <div className="form-actions">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={outOfStock}
              >
                確認画面へ進む
              </button>
            </div>
          </form>
        )}
      </main>
    </div>
  );
}

function clampQuantity(n: number): number {
  if (!Number.isFinite(n)) return MIN_QTY;
  return Math.min(MAX_QTY, Math.max(MIN_QTY, Math.floor(n)));
}

function toNumber(v: string | number): number {
  const n = typeof v === "string" ? Number(v) : v;
  return Number.isFinite(n) ? n : 0;
}

function formatYen(value: string | number): string {
  const n = toNumber(value);
  return `¥${n.toLocaleString("ja-JP")}`;
}

interface ValidationInput {
  quantity: number;
  stock: number;
  name: string;
  email: string;
  deliveryAddress: string;
}

function validateInputs(v: ValidationInput): string | null {
  if (!Number.isInteger(v.quantity) || v.quantity < MIN_QTY || v.quantity > MAX_QTY) {
    return `数量は ${MIN_QTY}〜${MAX_QTY} の範囲で指定してください。`;
  }
  if (v.quantity > v.stock) {
    return "在庫が不足しています。";
  }
  if (!v.name.trim()) return "お名前を入力してください。";
  if (!v.email.trim()) return "メールアドレスを入力してください。";
  if (!EMAIL_RE.test(v.email.trim())) return "メールアドレスの形式が正しくありません。";
  if (!v.deliveryAddress.trim()) return "お届け先住所を入力してください。";
  return null;
}
