import { useEffect } from "react";
import { useRoute, navigate } from "./router/Router";
import { useAuth } from "./lib/auth/useAuth";
import { LoginPage } from "./pages/LoginPage";
import { AgeVerificationPage } from "./pages/AgeVerificationPage";
import { TopPage } from "./pages/TopPage";
import { AdminProductsPage } from "./pages/AdminProductsPage";
import { AdminOrdersPage } from "./pages/AdminOrdersPage";
import { AdminLayout } from "./components/AdminLayout";
import { isAgeVerified } from "./lib/ageVerification";

/**
 * ルート種別:
 *   - 公開 (年齢確認のみ必要): "/", "/products/:id", "/orders/*"
 *   - 認証フリー: "/age-verification", "/login"
 *   - 管理 (JWT 必要): "/admin/*"
 */
const ADMIN_ROUTE_PREFIX = "/admin";

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
    // 認証フリーなルートは素通し
    if (route === "/age-verification" || route === "/login") {
      // 認証済みなのにログイン画面に来たら管理画面へ
      if (auth.isAuthenticated && route === "/login") {
        navigate("/admin/products");
      }
      return;
    }

    // 管理ルート: 未認証ならログインへ
    if (route.startsWith(ADMIN_ROUTE_PREFIX)) {
      if (!auth.isAuthenticated) {
        navigate("/login");
      }
      return;
    }

    // 公開ルート (顧客側): 年齢未確認なら誘導
    if (!isAgeVerified()) {
      navigate("/age-verification");
      return;
    }
  }, [route, auth.isAuthenticated]);

  // ===== レンダー: 副作用は呼ばない =====

  if (route === "/age-verification") {
    return <AgeVerificationPage />;
  }

  if (route === "/login") {
    // 認証済みのケースは useEffect が遷移させる。一瞬ここを通る可能性はあるが
    // すぐに /admin/products に遷移するので LoginPage を出して問題ない。
    return <LoginPage auth={auth} />;
  }

  // 管理ルート
  if (route.startsWith(ADMIN_ROUTE_PREFIX)) {
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

  // 公開 (顧客) ルート: 年齢未確認なら useEffect が誘導するので空白を返す
  if (!isAgeVerified()) {
    return null;
  }

  if (route === "/" || route === "") {
    return <TopPage />;
  }

  return (
    <div role="alert" style={{ padding: 24 }}>
      <h2>ページが見つかりません</h2>
      <p>パス: {route}</p>
      <a href="#/">トップへ戻る</a>
    </div>
  );
}
