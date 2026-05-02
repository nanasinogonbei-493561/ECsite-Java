import type { ReactNode } from "react";
import { navigate } from "../router/Router";
import type { AuthState } from "../lib/auth/useAuth";

interface Props {
  auth: AuthState;
  currentPath: string;
  children: ReactNode;
}

export function AdminLayout({ auth, currentPath, children }: Props) {
  const isActive = (path: string) =>
    currentPath === path ? "nav-link active" : "nav-link";

  const handleLogout = async () => {
    try {
      await auth.logout();
    } finally {
      navigate("/login");
    }
  };

  return (
    <div className="admin-shell">
      <header className="admin-header">
        <div className="admin-brand">SakeEC 管理</div>
        <nav className="admin-nav">
          <a className={isActive("/admin/products")} href="#/admin/products">商品管理</a>
          <a className={isActive("/admin/orders")} href="#/admin/orders">注文管理</a>
        </nav>
        <div className="admin-user">
          <span className="admin-username">{auth.username ?? ""}</span>
          <button type="button" className="btn btn-secondary" onClick={handleLogout}>
            ログアウト
          </button>
        </div>
      </header>
      <main className="admin-main">{children}</main>
    </div>
  );
}
