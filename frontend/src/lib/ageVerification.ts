/**
 * 年齢確認の状態をセッションストレージに保持する。
 * ブラウザを閉じるまで有効（基本設計書 9. セキュリティ）。
 */

const STORAGE_KEY = "ageVerifiedAt";
const MIN_AGE = 20;

export function isAgeVerified(): boolean {
  try {
    return sessionStorage.getItem(STORAGE_KEY) !== null;
  } catch {
    return false;
  }
}

export function markAgeVerified(): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, new Date().toISOString());
  } catch {
    // ストレージが使えない環境（プライベートモード等）は黙認
  }
}

export function clearAgeVerified(): void {
  try {
    sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // noop
  }
}

/**
 * 生年月日から満年齢を算出する。
 * 誕生日が来ていなければ 1 引く。
 */
export function calcAge(birth: Date, today: Date = new Date()): number {
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
    age -= 1;
  }
  return age;
}

export interface AgeCheckResult {
  ok: boolean;
  /** 入力エラーがある場合のメッセージ。ok=true のときは undefined。 */
  message?: string;
  age?: number;
}

/**
 * 入力された year/month/day を検証して年齢を返す。
 * 文字列のまま受け取り、数値変換と妥当性チェックをここで一括する。
 */
export function verifyBirthDate(
  yearStr: string,
  monthStr: string,
  dayStr: string,
  today: Date = new Date(),
): AgeCheckResult {
  const year = Number(yearStr);
  const month = Number(monthStr);
  const day = Number(dayStr);

  if (!yearStr || !monthStr || !dayStr) {
    return { ok: false, message: "生年月日を入力してください。" };
  }
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return { ok: false, message: "生年月日は数値で入力してください。" };
  }

  const thisYear = today.getFullYear();
  if (year < 1900 || year > thisYear) {
    return { ok: false, message: "西暦が正しくありません。" };
  }
  if (month < 1 || month > 12) {
    return { ok: false, message: "月は 1〜12 で入力してください。" };
  }
  if (day < 1 || day > 31) {
    return { ok: false, message: "日は 1〜31 で入力してください。" };
  }

  // 月末日のチェック (例: 2/30 を弾く)
  const birth = new Date(year, month - 1, day);
  if (
    birth.getFullYear() !== year ||
    birth.getMonth() !== month - 1 ||
    birth.getDate() !== day
  ) {
    return { ok: false, message: "存在しない日付です。" };
  }
  if (birth.getTime() > today.getTime()) {
    return { ok: false, message: "未来の日付は入力できません。" };
  }

  const age = calcAge(birth, today);
  if (age < MIN_AGE) {
    return {
      ok: false,
      age,
      message: `${MIN_AGE} 歳未満の方はご利用いただけません。`,
    };
  }
  return { ok: true, age };
}
