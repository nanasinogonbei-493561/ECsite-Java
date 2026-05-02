// 利用側はこのバレル経由で import する。例:
//   import { adminLogin, getProducts, ApiError } from "./lib/api";

export { ApiError } from "./apiClient";
export { api, getAdminToken, setAdminToken } from "./client";
export * from "./types";
export {
  // 認証
  adminLogin,
  adminLogout,
  // 商品 (公開)
  getProducts,
  getProduct,
  // 注文 (公開)
  createOrder,
  // 管理: 商品
  adminListProducts,
  adminCreateProduct,
  adminUpdateProduct,
  adminDeleteProduct,
  // 管理: 注文
  adminListOrders,
  adminUpdateOrderStatus,
} from "./endpoints";
