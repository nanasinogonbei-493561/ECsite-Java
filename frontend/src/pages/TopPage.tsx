import { useEffect, useMemo, useState } from "react";
import { getProducts, type ProductResponse } from "../lib/api";
import { logger } from "../lib/logging";
import { clearAgeVerified } from "../lib/ageVerification";
import { navigate } from "../router/Router";
import { toUserMessage } from "../components/errorMessage";

/**
 * 基本設計書 3.1 / 5.2:
 *   トップ (商品一覧) - 商品カード最大 30 件、簡易検索 (商品名)。
 *   GET /api/products?q=...
 */
export function TopPage() {
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [query, setQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getProducts(submittedQuery || undefined, 30)
      .then((res) => {
        if (cancelled) return;
        setProducts(res);
        logger.info({
          event: "PRODUCTS_LOADED",
          message: `loaded ${res.length} products`,
          context: { query: submittedQuery, count: res.length },
        });
      })
      .catch((e) => {
        if (cancelled) return;
        setError(toUserMessage(e));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [submittedQuery]);

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmittedQuery(query.trim());
  };

  const onResetAge = () => {
    clearAgeVerified();
    navigate("/age-verification");
  };

  return (
    <div className="top-shell">
      <header className="top-header">
        <div className="top-brand">SakeEC</div>
        <div style={{ display: "flex", gap: 8 }}>
          <a href="#/login" className="btn btn-secondary top-age-reset">
            管理者ログイン
          </a>
          <button
            type="button"
            className="btn btn-secondary top-age-reset"
            onClick={onResetAge}
            aria-label="年齢確認をやり直す"
          >
            年齢確認をやり直す
          </button>
        </div>
      </header>

      <main className="top-main">
        <h1 className="top-title">商品一覧</h1>

        <form className="top-search" onSubmit={onSearch} role="search">
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="商品名で検索"
            aria-label="商品名で検索"
          />
          <button type="submit" className="btn btn-primary">検索</button>
        </form>

        {error && <p role="alert" className="error-text">{error}</p>}

        {loading ? (
          <p className="muted">読み込み中…</p>
        ) : products.length === 0 ? (
          <p className="muted">該当する商品がありません。</p>
        ) : (
          <ProductGrid products={products} />
        )}
      </main>
    </div>
  );
}

function ProductGrid({ products }: { products: ProductResponse[] }) {
  return (
    <ul className="product-grid">
      {products.map((p) => (
        <ProductCard key={p.id} product={p} />
      ))}
    </ul>
  );
}

function ProductCard({ product }: { product: ProductResponse }) {
  const outOfStock = product.stockQuantity <= 0;
  const detailHref = `#/products/${product.id}`;
  const priceLabel = useMemo(() => formatYen(product.price), [product.price]);

  return (
    <li className="product-card">
      <a href={detailHref} className="product-card-image-link" aria-label={`${product.name} の詳細`}>
        {product.imageUrl ? (
          <img src={product.imageUrl} alt="" className="product-card-image" loading="lazy" />
        ) : (
          <div className="product-card-image product-card-image--placeholder" aria-hidden="true">
            画像なし
          </div>
        )}
      </a>
      <div className="product-card-body">
        <h2 className="product-card-name">
          <a href={detailHref}>{product.name}</a>
        </h2>
        <p className="product-card-meta">
          <span className="product-card-price">{priceLabel}</span>
          {product.volume != null && (
            <span className="product-card-volume">{product.volume}ml</span>
          )}
        </p>
        {outOfStock ? (
          <span className="status-badge status-cancelled">在庫なし</span>
        ) : (
          <a href={detailHref} className="btn btn-primary product-card-detail">
            詳細へ
          </a>
        )}
      </div>
    </li>
  );
}

function formatYen(value: string | number): string {
  const n = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(n)) return String(value);
  return `¥${n.toLocaleString("ja-JP")}`;
}
