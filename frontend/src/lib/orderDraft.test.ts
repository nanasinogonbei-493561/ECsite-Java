import { describe, expect, it } from "vitest";
import { clearOrderDraft, loadOrderDraft, saveOrderDraft, type OrderDraft } from "./orderDraft";

const sampleDraft: OrderDraft = {
  product: {
    id: 1,
    name: "純米吟醸A",
    price: 1800,
    volume: 720,
    stockQuantity: 12,
  },
  quantity: 2,
  customer: {
    name: "山田太郎",
    email: "taro@example.com",
    phone: "090-1111-2222",
    deliveryAddress: "東京都千代田区1-2-3",
  },
};

describe("orderDraft", () => {
  it("save → load で同じ内容が取れる", () => {
    saveOrderDraft(sampleDraft);
    expect(loadOrderDraft()).toEqual(sampleDraft);
  });

  it("clear で空になる", () => {
    saveOrderDraft(sampleDraft);
    clearOrderDraft();
    expect(loadOrderDraft()).toBeNull();
  });

  it("初期状態は null", () => {
    expect(loadOrderDraft()).toBeNull();
  });

  it("壊れた JSON は null を返す (例外を投げない)", () => {
    sessionStorage.setItem("sakeec.orderDraft", "{not json");
    expect(loadOrderDraft()).toBeNull();
  });
});
