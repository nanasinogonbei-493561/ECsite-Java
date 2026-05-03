/**
 * バックエンド (sakeec) の DTO に対応するフロント側の型。
 * 値オブジェクト (BigDecimal, Instant) は JSON 上は string で来る前提。
 */

export interface ProductResponse {
  id: number;
  name: string;
  brewery?: string;
  price: string | number;
  volume?: number;
  alcoholContent?: string | number;
  description?: string;
  imageUrl?: string;
  stockQuantity: number;
}

export interface OrderItemRequest {
  productId: number;
  /** 1 〜 3 (バックエンドのバリデーションと同じ) */
  quantity: number;
}

export interface OrderCustomerRequest {
  name: string;
  email: string;
  phone?: string;
  deliveryAddress: string;
}

export interface OrderRequest {
  items: OrderItemRequest[];
  customer: OrderCustomerRequest;
}

export interface OrderResponse {
  id: number;
  orderNumber: string;
  totalAmount: string | number;
  status: "PENDING" | "SHIPPED" | "DELIVERED" | "CANCELLED";
}

export interface AdminLoginRequest {
  username: string;
  password: string;
}

export interface AdminLoginResponse {
  token: string;
}

export interface AdminProductRequest {
  name: string;
  price: number;
  imageUrl?: string;
  /** 在庫数 (0 以上)。未指定時、サーバ側は 0 として扱う。 */
  stockQuantity?: number;
}

export interface AdminOrderStatusUpdateRequest {
  status: "PENDING" | "SHIPPED" | "DELIVERED" | "CANCELLED";
}

export interface AdminOrderStatusUpdateResponse {
  id: number;
  status: string;
  /** ISO8601 */
  updatedAt: string;
}
