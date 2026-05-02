import { useEffect } from "react";
import { useRoute, navigate } from "./router/Router";
import { useAuth } from "./lib/auth/useAuth";
import { LoginPage } from "./pages/LoginPage";
import { AdminProductsPage } from "./pages/AdminProductsPage";
import { AdminOrdersPage } from "./pages/AdminOrdersPage";
import { AdminLayout } from "./components/AdminLayout";

export default function App() {
  const route = useRoute();
  const auth = useAuth();

  /**
   * ルーティング遷移はすべて副作用 (navigate = window.location.hash 書き換え) なので
   * 必ず useEffect 内で行う。レンダー関数中に navigate を呼ぶと、
   *   1. hash 書き換えで hashchange イベントが発火
   *   2. しかし useRoute の addEventListener は commit 後にしか登録されない
   *   3. 結果イベントを取り逃し、route state が更新されず画面が空白のままになる
   * というバグを踏むため。
   */
  useEffect(() => {
    // hash が空 (起動直後) ならデフォルトに振り分け
    if (route === "/" || route === "") {
      navigate(auth.isAuthenticated ? "/admin/products" : "/login");
      return;
    }
    // 認証済みなのにログイン画面に来たら管理画面へ
    if (auth.isAuthenticated && route === "/login") {
      navigate("/admin/products");
      return;
    }
    // 未認証で認証必須ルートに来たらログインへ
    if (!auth.isAuthenticated && route !== "/login") {
      navigate("/login");
      return;
    }
  }, [route, auth.isAuthenticated]);

  // ===== レンダー: 副作用は呼ばない =====

  if (route === "/login") {
    // 認証済みのケースは useEffect が遷移させる。一瞬ここを通る可能性はあるが
    // すぐに /admin/products に遷移するので LoginPage を出して問題ない。
    return <LoginPage auth={auth} />;
  }

  if (!auth.isAuthenticated) {
    // useEffect が /login へ遷移させるまでの 1 フレーム空白
    return null;
  }

  let page;
  if (route === "/admin/products" || route === "/admin") {
    page = <AdminProductsPage auth={auth} />;
  } else if (route === "/admin/orders") {
    page = <AdminOrdersPage auth={auth} />;
  } else {
    page = (
      <div role="alert">
        <h2>ページが見つかりません</h2>
        <p>パス: {route}</p>
        <a href="#/admin/products">商品管理へ戻る</a>
      </div>
    );
  }

  return (
    <AdminLayout auth={auth} currentPath={route}>
      {page}
    </AdminLayout>
  );
}
