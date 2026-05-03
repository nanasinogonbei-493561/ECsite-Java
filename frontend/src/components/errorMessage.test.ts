import { describe, expect, it } from "vitest";
import { ApiError } from "../lib/api";
import { isUnauthorized, toUserMessage } from "./errorMessage";

function err(status: number, code?: string): ApiError {
  return new ApiError(status, code, undefined, null, "test");
}

describe("toUserMessage", () => {
  it("既知の errorCode を日本語化する", () => {
    expect(toUserMessage(err(400, "VALIDATION_ERROR"))).toMatch(/入力/);
    expect(toUserMessage(err(401, "AUTH_FAILED"))).toMatch(/ユーザー名/);
    expect(toUserMessage(err(404, "NOT_FOUND"))).toMatch(/見つかりません/);
    expect(toUserMessage(err(409, "OUT_OF_STOCK"))).toMatch(/在庫/);
  });

  it("5xx は汎用メッセージ", () => {
    expect(toUserMessage(err(500))).toMatch(/サーバー/);
    expect(toUserMessage(err(503))).toMatch(/サーバー/);
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
