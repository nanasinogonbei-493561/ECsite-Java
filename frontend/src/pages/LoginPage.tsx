import { useState, type FormEvent } from "react";
import type { AuthState } from "../lib/auth/useAuth";
import { navigate } from "../router/Router";
import { toUserMessage } from "../components/errorMessage";

interface Props {
  auth: AuthState;
}

export function LoginPage({ auth }: Props) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await auth.login(username, password);
      navigate("/admin/products");
    } catch (err) {
      setError(toUserMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-shell">
      <form className="login-card" onSubmit={onSubmit} aria-label="管理者ログイン">
        <h1 className="login-title">SakeEC 管理者ログイン</h1>

        <label className="field">
          <span>ユーザー名</span>
          <input
            type="text"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            disabled={submitting}
          />
        </label>

        <label className="field">
          <span>パスワード</span>
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            disabled={submitting}
          />
        </label>

        {error && <p role="alert" className="error-text">{error}</p>}

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? "ログイン中…" : "ログイン"}
        </button>
      </form>
    </div>
  );
}
