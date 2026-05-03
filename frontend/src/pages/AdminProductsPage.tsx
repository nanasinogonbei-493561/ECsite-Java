import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  adminCreateProduct,
  adminDeleteProduct,
  adminListProducts,
  adminUpdateProduct,
  type AdminProductRequest,
  type ProductResponse,
} from "../lib/api";
import type { AuthState } from "../lib/auth/useAuth";
import { isUnauthorized, toUserMessage } from "../components/errorMessage";
import { navigate } from "../router/Router";

interface Props {
  auth: AuthState;
}

type FormMode =
  | { kind: "idle" }
  | { kind: "create" }
  | { kind: "edit"; product: ProductResponse };

const emptyForm: AdminProductRequest = { name: "", price: 0, imageUrl: "", stockQuantity: 0 };

export function AdminProductsPage({ auth }: Props) {
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [mode, setMode] = useState<FormMode>({ kind: "idle" });
  const [form, setForm] = useState<AdminProductRequest>(emptyForm);
  // 価格・在庫は <input type="number"> の特性上、数値で直接バインドすると
  // 「初期 0 が消せない」「桁の途中状態 (空欄, '1.', '12e' 等) を保持できない」
  // ため、編集中は文字列バッファで保持し submit 時に数値化する。
  const [priceInput, setPriceInput] = useState("");
  const [stockInput, setStockInput] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleAuthError = useCallback((e: unknown) => {
    if (isUnauthorized(e)) {
      auth.forceLogout();
      navigate("/login");
      return true;
    }
    return false;
  }, [auth]);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await adminListProducts(query || undefined, 30);
      setProducts(list);
    } catch (e) {
      if (!handleAuthError(e)) setError(toUserMessage(e));
    } finally {
      setLoading(false);
    }
  }, [query, handleAuthError]);

  useEffect(() => {
    reload();
  }, [reload]);

  const startCreate = () => {
    setMode({ kind: "create" });
    setForm(emptyForm);
    setPriceInput("");
    setStockInput("");
    setError(null);
  };

  const startEdit = (p: ProductResponse) => {
    setMode({ kind: "edit", product: p });
    const priceNum = typeof p.price === "string" ? Number(p.price) : p.price;
    setForm({
      name: p.name,
      price: priceNum,
      imageUrl: p.imageUrl ?? "",
      stockQuantity: p.stockQuantity,
    });
    setPriceInput(String(priceNum));
    setStockInput(String(p.stockQuantity));
    setError(null);
  };

  const cancelForm = () => {
    setMode({ kind: "idle" });
    setForm(emptyForm);
    setPriceInput("");
    setStockInput("");
  };

  const submit = async (ev: FormEvent) => {
    ev.preventDefault();
    const price = Number(priceInput);
    if (!Number.isFinite(price) || price < 0) {
      setError("価格は 0 以上の数値で入力してください。");
      return;
    }
    const stock = Number(stockInput);
    if (!Number.isInteger(stock) || stock < 0) {
      setError("在庫数は 0 以上の整数で入力してください。");
      return;
    }
    const payload: AdminProductRequest = { ...form, price, stockQuantity: stock };
    setSubmitting(true);
    setError(null);
    try {
      if (mode.kind === "create") {
        await adminCreateProduct(payload);
      } else if (mode.kind === "edit") {
        await adminUpdateProduct(mode.product.id, payload);
      }
      cancelForm();
      await reload();
    } catch (e) {
      if (!handleAuthError(e)) setError(toUserMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async (p: ProductResponse) => {
    if (!window.confirm(`商品「${p.name}」を削除しますか？`)) return;
    setError(null);
    try {
      await adminDeleteProduct(p.id);
      await reload();
    } catch (e) {
      if (!handleAuthError(e)) setError(toUserMessage(e));
    }
  };

  return (
    <section>
      <div className="page-header">
        <h2>商品管理</h2>
        <button type="button" className="btn btn-primary" onClick={startCreate}>
          ＋ 新規追加
        </button>
      </div>

      <div className="toolbar">
        <input
          type="search"
          placeholder="商品名で検索"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && reload()}
        />
        <button type="button" className="btn btn-secondary" onClick={reload}>
          再読込
        </button>
      </div>

      {error && <p role="alert" className="error-text">{error}</p>}

      {(mode.kind === "create" || mode.kind === "edit") && (
        <form className="card" onSubmit={submit}>
          <h3>{mode.kind === "create" ? "商品を追加" : `商品 #${mode.product.id} を編集`}</h3>
          <label className="field">
            <span>商品名</span>
            <input
              type="text"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
              disabled={submitting}
            />
          </label>
          <label className="field">
            <span>価格 (円)</span>
            <input
              type="number"
              inputMode="numeric"
              min={0}
              step={1}
              value={priceInput}
              onChange={(e) => setPriceInput(e.target.value)}
              placeholder="例: 1800"
              required
              disabled={submitting}
            />
          </label>
          <label className="field">
            <span>在庫数</span>
            <input
              type="number"
              inputMode="numeric"
              min={0}
              step={1}
              value={stockInput}
              onChange={(e) => setStockInput(e.target.value)}
              placeholder="例: 10"
              required
              disabled={submitting}
            />
          </label>
          <label className="field">
            <span>画像 URL</span>
            <input
              type="text"
              value={form.imageUrl ?? ""}
              onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
              disabled={submitting}
            />
          </label>
          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? "保存中…" : "保存"}
            </button>
            <button type="button" className="btn btn-secondary" onClick={cancelForm} disabled={submitting}>
              キャンセル
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p>読み込み中…</p>
      ) : products.length === 0 ? (
        <p>商品がありません。</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>商品名</th>
              <th>価格</th>
              <th>在庫</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id}>
                <td>{p.id}</td>
                <td>{p.name}</td>
                <td>¥{Number(p.price).toLocaleString()}</td>
                <td>{p.stockQuantity}</td>
                <td className="row-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => startEdit(p)}>編集</button>
                  <button type="button" className="btn btn-danger"    onClick={() => remove(p)}>削除</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
