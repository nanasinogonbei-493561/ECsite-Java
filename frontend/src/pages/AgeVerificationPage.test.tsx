import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

const navigateMock = vi.fn();
vi.mock("../router/Router", () => ({
  navigate: (...args: unknown[]) => navigateMock(...args),
  useRoute: () => "/age-verification",
}));

import { AgeVerificationPage } from "./AgeVerificationPage";
import { isAgeVerified } from "../lib/ageVerification";

beforeEach(() => {
  navigateMock.mockReset();
});

describe("<AgeVerificationPage />", () => {
  it("20 歳以上を入力すると markAgeVerified し / にナビする", async () => {
    const user = userEvent.setup();
    render(<AgeVerificationPage />);

    await user.type(screen.getByLabelText("西暦（年）"), "1990");
    await user.type(screen.getByLabelText("月"), "1");
    await user.type(screen.getByLabelText("日"), "1");
    await user.click(screen.getByRole("button", { name: /確認して入店する/ }));

    expect(isAgeVerified()).toBe(true);
    expect(navigateMock).toHaveBeenCalledWith("/");
  });

  it("20 歳未満なら入力エラーを表示し、ナビしない", async () => {
    const user = userEvent.setup();
    render(<AgeVerificationPage />);

    // 今日は 2026-05-03。2010 年生まれは確実に 20 歳未満
    await user.type(screen.getByLabelText("西暦（年）"), "2010");
    await user.type(screen.getByLabelText("月"), "1");
    await user.type(screen.getByLabelText("日"), "1");
    await user.click(screen.getByRole("button", { name: /確認して入店する/ }));

    expect(screen.getByRole("alert")).toHaveTextContent(/20/);
    expect(navigateMock).not.toHaveBeenCalled();
    expect(isAgeVerified()).toBe(false);
  });

  it("存在しない日付は入力エラー", async () => {
    const user = userEvent.setup();
    render(<AgeVerificationPage />);

    await user.type(screen.getByLabelText("西暦（年）"), "1990");
    await user.type(screen.getByLabelText("月"), "2");
    await user.type(screen.getByLabelText("日"), "30");
    await user.click(screen.getByRole("button", { name: /確認して入店する/ }));

    expect(screen.getByRole("alert")).toHaveTextContent(/存在しない/);
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
