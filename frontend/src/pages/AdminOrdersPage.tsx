import { Fragment, useCallback, useEffect, useState } from "react";
import {
  adminGetOrderDetail,
  adminListOrders,
  adminUpdateOrderStatus,
  type AdminOrderDetailResponse,
  type OrderResponse,
} from "../lib/api";
import type { AuthState } from "../lib/auth/useAuth";
import { isUnauthorized, toUserMessage } from "../components/errorMessage";
import { navigate } from "../router/Router";

interface Props {
  auth: AuthState;
}

type OrderStatus = OrderResponse["status"];

const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING:   "受付済",
  SHIPPED:   "発送済",
  DELIVERED: "配達完了",
  CANCELLED: "キャンセル",
};

// バックエンドのステータス遷移マシンと同じ
const ALLOWED_NEXT: Record<OrderStatus, OrderStatus[]> = {
  PENDING:   ["SHIPPED", "CANCELLED"],
  SHIPPED:   ["DELIVERED"],
  DELIVERED: [],
  CANCELLED: [],
};

const STATUS_FILTER_OPTIONS: Array<{ value: ""; label: string } | { value: OrderStatus; label: string }> = [
  { value: "",          label: "全て" },
  { value: "PENDING",   label: "受付済" },
  { value: "SHIPPED",   label: "発送済" },
  { value: "DELIVERED", label: "配達完了" },
  { value: "CANCELLED", label: "キャンセル" },
];

export function AdminOrdersPage({ auth }: Props) {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<"" | OrderStatus>("");
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  // 展開中の注文 ID と、その詳細データ (取得中は loading フラグ)
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<AdminOrderDetailResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

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
      const list = await adminListOrders(statusFilter || undefined);
      setOrders(list);
    } catch (e) {
      if (!handleAuthError(e)) setError(toUserMessage(e));
    } finally {
      setLoading(false);
    }
  }, [statusFilter, handleAuthError]);

  useEffect(() => {
    reload();
  }, [reload]);

  const toggleDetail = async (order: OrderResponse) => {
    if (expandedId === order.id) {
      setExpandedId(null);
      setDetail(null);
      setDetailError(null);
      return;
    }
    setExpandedId(order.id);
    setDetail(null);
    setDetailError(null);
    setDetailLoading(true);
    try {
      const d = await adminGetOrderDetail(order.id);
      setDetail(d);
    } catch (e) {
      if (!handleAuthError(e)) setDetailError(toUserMessage(e));
    } finally {
      setDetailLoading(false);
    }
  };

  const updateStatus = async (order: OrderResponse, next: OrderStatus) => {
    setUpdatingId(order.orderNumber);
    setError(null);
    try {
      await adminUpdateOrderStatus(order.id, next);
      await reload();
      // 展開中の注文を更新したらステータス表示も新しくしたいので詳細も取り直す
      if (expandedId === order.id) {
        const d = await adminGetOrderDetail(order.id);
        setDetail(d);
      }
    } catch (e) {
      if (!handleAuthError(e)) setError(toUserMessage(e));
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <section>
      <div className="page-header">
        <h2>注文管理</h2>
      </div>

      <div className="toolbar">
        <label>
          ステータス：
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as "" | OrderStatus)}
          >
            {STATUS_FILTER_OPTIONS.map((opt) => (
              <option key={opt.value || "ALL"} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className="btn btn-secondary" onClick={reload}>
          再読込
        </button>
      </div>

      {error && <p role="alert" className="error-text">{error}</p>}

      {loading ? (
        <p>読み込み中…</p>
      ) : orders.length === 0 ? (
        <p>該当する注文がありません。</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>注文番号</th>
              <th>合計</th>
              <th>ステータス</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => {
              const expanded = expandedId === o.id;
              return (
                <Fragment key={o.orderNumber}>
                  <tr>
                    <td>{o.orderNumber}</td>
                    <td>¥{Number(o.totalAmount).toLocaleString()}</td>
                    <td>
                      <span className={`status-badge status-${o.status.toLowerCase()}`}>
                        {STATUS_LABELS[o.status]}
                      </span>
                    </td>
                    <td className="row-actions">
                      <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => toggleDetail(o)}
                        aria-expanded={expanded}
                      >
                        {expanded ? "詳細を閉じる" : "詳細"}
                      </button>
                      {ALLOWED_NEXT[o.status].length === 0 && <span className="muted">変更不可</span>}
                      {ALLOWED_NEXT[o.status].map((next) => (
                        <button
                          key={next}
                          type="button"
                          className="btn btn-secondary"
                          disabled={updatingId === o.orderNumber}
                          onClick={() => updateStatus(o, next)}
                        >
                          {updatingId === o.orderNumber ? "更新中…" : `→ ${STATUS_LABELS[next]}`}
                        </button>
                      ))}
                    </td>
                  </tr>
                  {expanded && (
                    <tr className="detail-row">
                      <td colSpan={4}>
                        {detailLoading && <p className="muted">詳細を読み込み中…</p>}
                        {detailError && <p role="alert" className="error-text">{detailError}</p>}
                        {!detailLoading && !detailError && detail && detail.id === o.id && (
                          <OrderDetailPanel detail={detail} />
                        )}
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      )}
    </section>
  );
}

function OrderDetailPanel({ detail }: { detail: AdminOrderDetailResponse }) {
  return (
    <div className="order-detail-panel">
      <div className="order-detail-grid">
        <section>
          <h4>お客様情報</h4>
          <dl className="confirm-list">
            <dt>お名前</dt>
            <dd>{detail.customerName}</dd>
            <dt>メールアドレス</dt>
            <dd>{detail.customerEmail}</dd>
            <dt>電話番号</dt>
            <dd>{detail.customerPhone || "—"}</dd>
            <dt>お届け先</dt>
            <dd style={{ whiteSpace: "pre-wrap" }}>{detail.deliveryAddress || "—"}</dd>
          </dl>
        </section>

        <section>
          <h4>注文情報</h4>
          <dl className="confirm-list">
            <dt>注文番号</dt>
            <dd>{detail.orderNumber}</dd>
            <dt>受注日時</dt>
            <dd>{formatDateTime(detail.createdAt)}</dd>
            <dt>ステータス</dt>
            <dd>
              <span className={`status-badge status-${detail.status.toLowerCase()}`}>
                {STATUS_LABELS[detail.status]}
              </span>
            </dd>
            <dt>合計金額</dt>
            <dd><strong>¥{Number(detail.totalAmount).toLocaleString()}</strong></dd>
          </dl>
        </section>
      </div>

      <h4 style={{ marginTop: 16 }}>明細</h4>
      <table className="data-table" style={{ marginTop: 4 }}>
        <thead>
          <tr>
            <th>商品ID</th>
            <th>商品名</th>
            <th>単価</th>
            <th>数量</th>
            <th>小計</th>
          </tr>
        </thead>
        <tbody>
          {detail.items.length === 0 ? (
            <tr><td colSpan={5} className="muted">明細がありません。</td></tr>
          ) : detail.items.map((it) => (
            <tr key={it.productId}>
              <td>{it.productId}</td>
              <td>{it.productName}</td>
              <td>¥{Number(it.unitPrice).toLocaleString()}</td>
              <td>{it.quantity}</td>
              <td>¥{Number(it.subtotal).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("ja-JP", {
    year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit",
  });
}
