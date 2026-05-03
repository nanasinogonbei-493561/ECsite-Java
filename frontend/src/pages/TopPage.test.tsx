import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

const getProductsMock = vi.fn();
vi.mock("../lib/api", async (orig) => {
  const actual = await orig<typeof import("../lib/api")>();
  return {
    ...actual,
    getProducts: (...args: unknown[]) => getProductsMock(...args),
  };
});

const navigateMock = vi.fn();
vi.mock("../router/Router", () => ({
  navigate: (...args: unknown[]) => navigateMock(...args),
  useRoute: () => "/",
}));

import { TopPage } from "./TopPage";
import type { ProductResponse } from "../lib/api";

const products: ProductResponse[] = [
  { id: 1, name: "純米吟醸A", price: 1800, volume: 720, stockQuantity: 12 },
  { id: 2, name: "本醸造B", price: 1200, volume: 720, stockQuantity: 0 },
];

beforeEach(() => {
  getProductsMock.mockReset();
  navigateMock.mockReset();
});

describe("<TopPage />", () => {
  it("商品一覧を取得して描画する", async () => {
    getProductsMock.mockResolvedValueOnce(products);
    render(<TopPage />);

    expect(await screen.findByRole("link", { name: /純米吟醸A の詳細/ })).toBeInTheDocument();
    expect(screen.getByText("¥1,800")).toBeInTheDocument();
    expect(screen.getByText("¥1,200")).toBeInTheDocument();
    // 在庫 0 商品はバッジ表示で「詳細へ」ボタンが出ない
    expect(screen.getAllByText("在庫なし").length).toBeGreaterThan(0);
  });

  it("検索フォームで submittedQuery が API に渡る", async () => {
    getProductsMock
      .mockResolvedValueOnce(products) // 初回: query なし
      .mockResolvedValueOnce([products[0]]); // 検索後

    const user = userEvent.setup();
    render(<TopPage />);
    await screen.findByText("¥1,800");

    await user.type(screen.getByRole("searchbox"), "純米");
    await user.click(screen.getByRole("button", { name: "検索" }));

    await waitFor(() => {
      expect(getProductsMock).toHaveBeenLastCalledWith("純米", 30);
    });
  });

  it("0 件のときは空メッセージを出す", async () => {
    getProductsMock.mockResolvedValueOnce([]);
    render(<TopPage />);
    expect(await screen.findByText(/該当する商品がありません/)).toBeInTheDocument();
  });

  it("API 失敗時はエラーメッセージを出す", async () => {
    getProductsMock.mockRejectedValueOnce(new Error("network down"));
    render(<TopPage />);
    expect(await screen.findByRole("alert")).toBeInTheDocument();
  });
});
