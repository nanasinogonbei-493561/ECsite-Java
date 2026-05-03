/**
 * 注文フロー (/orders/new → /orders/confirm → /orders/complete) で使う
 * 入力中の注文情報をセッションストレージに保持する。
 *
 * MVP は 1 商品単位。複数商品カート化は Phase2。
 */

import type { ProductResponse } from "./api";

const STORAGE_KEY = "sakeec.orderDraft";

export interface OrderDraft {
  product: ProductResponse;
  quantity: number;
  customer: {
    name: string;
    email: string;
    phone: string;
    deliveryAddress: string;
  };
}

export function saveOrderDraft(draft: OrderDraft): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(draft));
  } catch {
    // noop
  }
}

export function loadOrderDraft(): OrderDraft | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as OrderDraft;
  } catch {
    return null;
  }
}

export function clearOrderDraft(): void {
  try {
    sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // noop
  }
}
