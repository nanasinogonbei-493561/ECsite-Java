import { useState, type FormEvent } from "react";
import { navigate } from "../router/Router";
import { logger } from "../lib/logging";
import { markAgeVerified, verifyBirthDate } from "../lib/ageVerification";

/**
 * 基本設計書 3.1 / 7. ビジネスルール:
 *   初回アクセスで生年月日入力。20 歳以上のみ継続利用可能（セッション保持）。
 */
export function AgeVerificationPage() {
  const [year, setYear] = useState("");
  const [month, setMonth] = useState("");
  const [day, setDay] = useState("");
  const [error, setError] = useState<string | null>(null);

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    const result = verifyBirthDate(year, month, day);
    if (!result.ok) {
      setError(result.message ?? "入力内容を確認してください。");
      logger.warn({
        event: "AGE_VERIFICATION_FAILED",
        message: result.message ?? "age verification failed",
        context: { age: result.age },
      });
      return;
    }
    markAgeVerified();
    logger.info({
      event: "AGE_VERIFICATION_SUCCEEDED",
      message: "age verified",
      context: { age: result.age },
    });
    navigate("/");
  };

  return (
    <div className="login-shell">
      <form className="login-card" onSubmit={onSubmit} aria-label="年齢確認">
        <h1 className="login-title">年齢確認</h1>
        <p className="muted" style={{ marginTop: 0, marginBottom: 16 }}>
          このサイトはお酒を販売しています。20 歳未満の方はご利用いただけません。
          <br />
          生年月日を入力してください。
        </p>

        <div className="dob-grid">
          <label className="field">
            <span>西暦（年）</span>
            <input
              type="number"
              inputMode="numeric"
              min={1900}
              max={2100}
              value={year}
              onChange={(e) => setYear(e.target.value)}
              placeholder="1990"
              required
              autoFocus
            />
          </label>
          <label className="field">
            <span>月</span>
            <input
              type="number"
              inputMode="numeric"
              min={1}
              max={12}
              value={month}
              onChange={(e) => setMonth(e.target.value)}
              placeholder="1"
              required
            />
          </label>
          <label className="field">
            <span>日</span>
            <input
              type="number"
              inputMode="numeric"
              min={1}
              max={31}
              value={day}
              onChange={(e) => setDay(e.target.value)}
              placeholder="1"
              required
            />
          </label>
        </div>

        {error && (
          <p role="alert" className="error-text">
            {error}
          </p>
        )}

        <button type="submit" className="btn btn-primary" style={{ width: "100%" }}>
          確認して入店する
        </button>
      </form>
    </div>
  );
}
