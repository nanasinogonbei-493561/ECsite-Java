import { describe, expect, it } from "vitest";
import { ApiError } from "../lib/api";
import { isUnauthorized, toUserMessage } from "./errorMessage";

function err(status: number, code?: string, body?: unknown): ApiError {
  return new ApiError(status, code, undefined, body ?? null, "test");
}

describe("toUserMessage", () => {
  it("既知の errorCode を日本語化する", () => {
    expect(toUserMessage(err(400, "VALIDATION_ERROR"))).toMatch(/入力/);
    expect(toUserMessage(err(401, "AUTH_FAILED"))).toMatch(/ユーザー名/);
    expect(toUserMessage(err(404, "NOT_FOUND"))).toMatch(/見つかりません/);
    expect(toUserMessage(err(409, "OUT_OF_STOCK"))).toMatch(/在庫/);
  });

  it("500 は汎用 5xx メッセージ", () => {
    expect(toUserMessage(err(500))).toMatch(/サーバーで問題/);
  });

  it("502/503/504 はバックエンド未起動を示すメッセージ", () => {
    expect(toUserMessage(err(502))).toMatch(/接続できません/);
    expect(toUserMessage(err(503))).toMatch(/接続できません/);
    expect(toUserMessage(err(504))).toMatch(/接続できません/);
  });

  it("backend が message を返していれば未知の 4xx で表示する", () => {
    const e = err(422, undefined, { code: "WHATEVER", message: "個別な業務エラー文言" });
    expect(toUserMessage(e)).toBe("個別な業務エラー文言");
  });

  it("401 (errorCode なし) はセッション切れメッセージ", () => {
    expect(toUserMessage(err(401))).toMatch(/セッション/);
  });

  it("ApiError 以外は通信失敗メッセージ", () => {
    expect(toUserMessage(new Error("boom"))).toMatch(/通信に失敗/);
    expect(toUserMessage("boom")).toMatch(/通信に失敗/);
  });
});

describe("isUnauthorized", () => {
  it("401 のときだけ true", () => {
    expect(isUnauthorized(err(401))).toBe(true);
    expect(isUnauthorized(err(403))).toBe(false);
    expect(isUnauthorized(new Error("x"))).toBe(false);
  });
});
