import { useCallback, useEffect, useState } from "react";
import {
  adminListOrders,
  adminUpdateOrderStatus,
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

  const updateStatus = async (order: OrderResponse, next: OrderStatus) => {
    setUpdatingId(order.orderNumber);
    setError(null);
    try {
      await adminUpdateOrderStatus(order.id, next);
      await reload();
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
            {orders.map((o) => (
              <tr key={o.orderNumber}>
                <td>{o.orderNumber}</td>
                <td>¥{Number(o.totalAmount).toLocaleString()}</td>
                <td>
                  <span className={`status-badge status-${o.status.toLowerCase()}`}>
                    {STATUS_LABELS[o.status]}
                  </span>
                </td>
                <td className="row-actions">
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
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
