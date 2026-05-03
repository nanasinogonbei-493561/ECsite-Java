import { describe, expect, it } from "vitest";
import {
  calcAge,
  clearAgeVerified,
  isAgeVerified,
  markAgeVerified,
  verifyBirthDate,
} from "./ageVerification";

describe("calcAge", () => {
  it("誕生日が来ていれば満年齢を返す", () => {
    const today = new Date(2026, 4, 3); // 2026-05-03
    expect(calcAge(new Date(2000, 4, 3), today)).toBe(26);
    expect(calcAge(new Date(2000, 4, 2), today)).toBe(26);
  });
  it("誕生日が来ていなければ 1 引く", () => {
    const today = new Date(2026, 4, 3); // 2026-05-03
    expect(calcAge(new Date(2000, 4, 4), today)).toBe(25);
    expect(calcAge(new Date(2000, 5, 1), today)).toBe(25);
  });
});

describe("verifyBirthDate", () => {
  // 「今日」は固定して境界条件を厳密に試す
  const today = new Date(2026, 4, 3); // 2026-05-03

  it("ちょうど 20 歳の誕生日当日は OK", () => {
    const r = verifyBirthDate("2006", "5", "3", today);
    expect(r.ok).toBe(true);
    expect(r.age).toBe(20);
  });

  it("20 歳の誕生日前日は NG (まだ 19 歳)", () => {
    const r = verifyBirthDate("2006", "5", "4", today);
    expect(r.ok).toBe(false);
    expect(r.age).toBe(19);
    expect(r.message).toMatch(/20/);
  });

  it("空欄は入力エラー", () => {
    expect(verifyBirthDate("", "5", "3", today).ok).toBe(false);
    expect(verifyBirthDate("2000", "", "3", today).ok).toBe(false);
    expect(verifyBirthDate("2000", "5", "", today).ok).toBe(false);
  });

  it("存在しない日付 (2/30) は弾く", () => {
    const r = verifyBirthDate("2000", "2", "30", today);
    expect(r.ok).toBe(false);
    expect(r.message).toMatch(/存在しない/);
  });

  it("月の範囲外を弾く", () => {
    expect(verifyBirthDate("2000", "0", "1", today).ok).toBe(false);
    expect(verifyBirthDate("2000", "13", "1", today).ok).toBe(false);
  });

  it("未来の日付を弾く (同年内)", () => {
    // today=2026-05-03 に対し 2026-12-31 は未来
    const r = verifyBirthDate("2026", "12", "31", today);
    expect(r.ok).toBe(false);
    expect(r.message).toMatch(/未来/);
  });

  it("西暦の上限超えを弾く", () => {
    const r = verifyBirthDate("2030", "1", "1", today);
    expect(r.ok).toBe(false);
    expect(r.message).toMatch(/西暦/);
  });

  it("数値以外を弾く", () => {
    const r = verifyBirthDate("abcd", "5", "3", today);
    expect(r.ok).toBe(false);
  });
});

describe("sessionStorage 連携", () => {
  it("mark → isVerified → clear のサイクル", () => {
    expect(isAgeVerified()).toBe(false);
    markAgeVerified();
    expect(isAgeVerified()).toBe(true);
    clearAgeVerified();
    expect(isAgeVerified()).toBe(false);
  });
});
