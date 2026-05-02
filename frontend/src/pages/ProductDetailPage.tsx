import { useEffect, useMemo, useState } from "react";
import { ApiError, getProduct, type ProductResponse } from "../lib/api";
import { logger } from "../lib/logging";
import { toUserMessage } from "../components/errorMessage";

interface Props {
  productId: number;
}

/**
 * 基本設計書 3.1 / 5.2:
 *   商品詳細 - 基本情報＋在庫。GET /api/products/{id}。
 *   在庫 0 は「在庫なし」表示で注文不可 (14. 受け入れ条件)。
 */
export function ProductDetailPage({ productId }: Props) {
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setNotFound(false);
    getProduct(productId)
      .then((res) => {
        if (cancelled) return;
        setProduct(res);
        logger.info({
          event: "PRODUCT_VIEWED",
          message: "product detail viewed",
          context: { productId: res.id },
        });
      })
      .catch((e) => {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 404) {
          setNotFound(true);
        } else {
          setError(toUserMessage(e));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [productId]);

  return (
    <div className="top-shell">
      <header className="top-header">
        <a href="#/" className="top-brand" style={{ textDecoration: "none" }}>
          SakeEC
        </a>
      </header>

      <main className="top-main">
        <p className="muted" style={{ marginTop: 0 }}>
          <a href="#/">← 商品一覧へ戻る</a>
        </p>

        {loading && <p className="muted">読み込み中…</p>}
        {error && <p role="alert" className="error-text">{error}</p>}
        {notFound && (
          <div role="alert">
            <h2>商品が見つかりません</h2>
            <p className="muted">URL が間違っているか、商品が削除された可能性があります。</p>
          </div>
        )}

        {product && <ProductDetail product={product} />}
      </main>
    </div>
  );
}

function ProductDetail({ product }: { product: ProductResponse }) {
  const outOfStock = product.stockQuantity <= 0;
  const priceLabel = useMemo(() => formatYen(product.price), [product.price]);
  const orderHref = `#/orders/new?productId=${product.id}&qty=1`;

  return (
    <article className="product-detail">
      <div className="product-detail-image">
        {product.imageUrl ? (
          <img src={product.imageUrl} alt="" />
        ) : (
          <div className="product-card-image--placeholder" aria-hidden="true">
            画像なし
          </div>
        )}
      </div>

      <div className="product-detail-body">
        <h1 className="product-detail-name">{product.name}</h1>
        {product.brewery && (
          <p className="product-detail-brewery muted">{product.brewery}</p>
        )}

        <p className="product-detail-price">{priceLabel}</p>

        <dl className="product-detail-spec">
          {product.volume != null && (
            <>
              <dt>容量</dt>
              <dd>{product.volume}ml</dd>
            </>
          )}
          {product.alcoholContent != null && (
            <>
              <dt>アルコール度数</dt>
              <dd>{product.alcoholContent}%</dd>
            </>
          )}
          <dt>在庫</dt>
          <dd>
            {outOfStock ? (
              <span className="status-badge status-cancelled">在庫なし</span>
            ) : (
              `${product.stockQuantity} 本`
            )}
          </dd>
        </dl>

        {product.description && (
          <section className="product-detail-description">
            <h2>商品説明</h2>
            <p>{product.description}</p>
          </section>
        )}

        <div className="product-detail-actions">
          {outOfStock ? (
            <button type="button" className="btn btn-primary" disabled>
              在庫なし
            </button>
          ) : (
            <a href={orderHref} className="btn btn-primary">
              注文画面へ進む
            </a>
          )}
        </div>
      </div>
    </article>
  );
}

function formatYen(value: string | number): string {
  const n = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(n)) return String(value);
  return `¥${n.toLocaleString("ja-JP")}`;
}
