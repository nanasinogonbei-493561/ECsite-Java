/**
 * 管理画面の認証状態を管理する hook。
 * sessionStorage 上の JWT を信頼ソースとし、login/logout で同期する。
 */

import { useCallback, useEffect, useState } from "react";
import {
  adminLogin,
  adminLogout,
  getAdminToken,
} from "../api";

export interface AuthState {
  isAuthenticated: boolean;
  username: string | undefined;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  /** 401 を画面側で検知したときに呼び、ローカルのトークンを破棄する */
  forceLogout: () => void;
}

const USERNAME_KEY = "sakeec.adminUsername";

export function useAuth(): AuthState {
  const [token, setToken] = useState<string | undefined>(getAdminToken());
  const [username, setUsername] = useState<string | undefined>(
    typeof sessionStorage !== "undefined"
      ? sessionStorage.getItem(USERNAME_KEY) ?? undefined
      : undefined,
  );

  // 別タブでログアウトした等の sessionStorage 変化に追従
  useEffect(() => {
    const onStorage = () => {
      setToken(getAdminToken());
      setUsername(sessionStorage.getItem(USERNAME_KEY) ?? undefined);
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const login = useCallback(async (u: string, p: string) => {
    await adminLogin(u, p);
    sessionStorage.setItem(USERNAME_KEY, u);
    setToken(getAdminToken());
    setUsername(u);
  }, []);

  const logout = useCallback(async () => {
    await adminLogout();
    sessionStorage.removeItem(USERNAME_KEY);
    setToken(undefined);
    setUsername(undefined);
  }, []);

  const forceLogout = useCallback(() => {
    sessionStorage.removeItem(USERNAME_KEY);
    setToken(undefined);
    setUsername(undefined);
  }, []);

  return {
    isAuthenticated: !!token,
    username,
    login,
    logout,
    forceLogout,
  };
}
