/**
 * バックエンド (sakeec) の各エンドポイントに対応する薄いラッパ。
 *
 * apiClient 内部で API_REQUEST / API_RESPONSE / API_ERROR / NETWORK_ERROR は
 * すべて自動でログされるため、ここでは「どのドメイン操作で起きたか」という
 * 文脈情報 (domain) を付与した最低限のログだけを足し、例外は呼び出し側に再 throw する。
 */

import { ApiError } from "./apiClient";
import { api, setAdminToken } from "./client";
import { logger, setUserId } from "../logging";
import type {
  AdminOrderDetailResponse,
  AdminOrderStatusUpdateResponse,
  AdminProductRequest,
  OrderRequest,
  OrderResponse,
  ProductResponse,
} from "./types";

/**
 * API 呼び出し失敗時の共通エラーログ。
 * `apiClient` 側の汎用ログとは別に、業務ドメイン名 (例: "auth.login") を付けて
 * "誰が叩いたコードで失敗したか" を traceId で串刺し検索できるようにする。
 */
function logDomainFailure(
  domain: string,
  message: string,
  e: unknown,
  extra?: Record<string, unknown>,
): void {
  const apiErr = e instanceof ApiError ? e : undefined;
  logger.warn({
    event: "USER_ACTION",
    message,
    traceId: apiErr?.traceId,
    error: e,
    context: {
      domain,
      status: apiErr?.status,
      errorCode: apiErr?.errorCode,
      ...extra,
    },
  });
}

// ---------- 認証 ----------

export async function adminLogin(username: string, password: string): Promise<void> {
  try {
    const res = await api.post<{ token: string }>("/admin/login", { username, password });
    setAdminToken(res.token);
    setUserId(username);
    logger.info({
      event: "USER_ACTION",
      message: "管理者ログイン成功",
      context: { domain: "auth.login", username },
    });
  } catch (e) {
    // パスワードはログに含めない (username のみ)
    logDomainFailure("auth.login", "管理者ログインに失敗しました", e, { username });
    throw e;
  }
}

export async function adminLogout(): Promise<void> {
  try {
    await api.post("/admin/logout");
  } catch (e) {
    // ログアウトはサーバ失敗してもクライアント側状態は必ずクリアする
    logDomainFailure("auth.logout", "ログアウト要求に失敗しました", e);
  } finally {
    setAdminToken(undefined);
    setUserId(undefined);
  }
}

// ---------- 商品 (公開) ----------

export async function getProducts(query?: string, limit = 30): Promise<ProductResponse[]> {
  const qs = new URLSearchParams();
  if (query) qs.set("q", query);
  qs.set("limit", String(limit));
  try {
    return await api.get<ProductResponse[]>(`/products?${qs.toString()}`);
  } catch (e) {
    logDomainFailure("products.list", "商品一覧の取得に失敗しました", e, { query, limit });
    throw e;
  }
}

export async function getProduct(id: number): Promise<ProductResponse> {
  try {
    return await api.get<ProductResponse>(`/products/${id}`);
  } catch (e) {
    logDomainFailure("products.detail", "商品詳細の取得に失敗しました", e, { id });
    throw e;
  }
}

// ---------- 注文 (公開) ----------

export async function createOrder(req: OrderRequest): Promise<OrderResponse> {
  try {
    return await api.post<OrderResponse>("/orders", req);
  } catch (e) {
    logDomainFailure("orders.create", "注文の作成に失敗しました", e, {
      itemCount: req.items.length,
      // メール / 住所などの個人情報はログに残さない
    });
    throw e;
  }
}

// ---------- 管理: 商品 CRUD ----------

export async function adminListProducts(query?: string, limit = 30): Promise<ProductResponse[]> {
  const qs = new URLSearchParams();
  if (query) qs.set("q", query);
  qs.set("limit", String(limit));
  try {
    return await api.get<ProductResponse[]>(`/admin/products?${qs.toString()}`);
  } catch (e) {
    logDomainFailure("admin.products.list", "管理: 商品一覧の取得に失敗しました", e, { query, limit });
    throw e;
  }
}

export async function adminCreateProduct(req: AdminProductRequest): Promise<ProductResponse> {
  try {
    return await api.post<ProductResponse>("/admin/products", req);
  } catch (e) {
    logDomainFailure("admin.products.create", "管理: 商品の作成に失敗しました", e, { name: req.name });
    throw e;
  }
}

export async function adminUpdateProduct(id: number, req: AdminProductRequest): Promise<ProductResponse> {
  try {
    return await api.put<ProductResponse>(`/admin/products/${id}`, req);
  } catch (e) {
    logDomainFailure("admin.products.update", "管理: 商品の更新に失敗しました", e, { id });
    throw e;
  }
}

export async function adminDeleteProduct(id: number): Promise<void> {
  try {
    await api.delete<void>(`/admin/products/${id}`);
  } catch (e) {
    logDomainFailure("admin.products.delete", "管理: 商品の削除に失敗しました", e, { id });
    throw e;
  }
}

// ---------- 管理: 注文 ----------

export async function adminListOrders(status?: string): Promise<OrderResponse[]> {
  const qs = new URLSearchParams();
  if (status) qs.set("status", status);
  const path = qs.toString() ? `/admin/orders?${qs.toString()}` : "/admin/orders";
  try {
    return await api.get<OrderResponse[]>(path);
  } catch (e) {
    logDomainFailure("admin.orders.list", "管理: 注文一覧の取得に失敗しました", e, { status });
    throw e;
  }
}

export async function adminGetOrderDetail(id: number): Promise<AdminOrderDetailResponse> {
  try {
    return await api.get<AdminOrderDetailResponse>(`/admin/orders/${id}`);
  } catch (e) {
    logDomainFailure("admin.orders.detail", "管理: 注文詳細の取得に失敗しました", e, { id });
    throw e;
  }
}

export async function adminUpdateOrderStatus(
  id: number,
  status: "PENDING" | "SHIPPED" | "DELIVERED" | "CANCELLED",
): Promise<AdminOrderStatusUpdateResponse> {
  try {
    return await api.put<AdminOrderStatusUpdateResponse>(`/admin/orders/${id}/status`, { status });
  } catch (e) {
    logDomainFailure("admin.orders.updateStatus", "管理: 注文ステータスの更新に失敗しました", e, {
      id,
      to: status,
    });
    throw e;
  }
}
