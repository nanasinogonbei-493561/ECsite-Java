package com.example.sakeec.security;

import com.example.sakeec.config.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil util(long expirationMs) {
        JwtProperties p = new JwtProperties();
        p.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-test");
        p.setExpirationMs(expirationMs);
        return new JwtUtil(p);
    }

    @Test
    @DisplayName("正常: 生成したトークンは validate で同じ subject を返す")
    void roundTrip() {
        JwtUtil u = util(60_000);
        String token = u.generate("admin");

        Optional<String> subject = u.validateAndGetSubject(token);

        assertThat(subject).contains("admin");
    }

    @Test
    @DisplayName("セキュリティ: 期限切れトークンは empty (ログインの再要求が発生)")
    void expiredToken() throws Exception {
        JwtUtil u = util(1); // 1ms で expire
        String token = u.generate("admin");
        Thread.sleep(20);

        assertThat(u.validateAndGetSubject(token)).isEmpty();
    }

    @Test
    @DisplayName("セキュリティ: 別 secret で署名されたトークンは empty (なりすまし防止)")
    void wrongSignature() {
        JwtUtil good   = util(60_000);
        JwtProperties evilProps = new JwtProperties();
        evilProps.setSecret("attacker-secret-attacker-secret-attacker-secret-XXXXXXXXXXXXXXX");
        evilProps.setExpirationMs(60_000);
        JwtUtil evil = new JwtUtil(evilProps);

        String forged = evil.generate("admin");

        assertThat(good.validateAndGetSubject(forged)).isEmpty();
    }

    @Test
    @DisplayName("セキュリティ: ペイロード改ざん → 署名検証失敗で empty")
    void tamperedToken() {
        JwtUtil u = util(60_000);
        String token = u.generate("admin");
        // payload セクション (header.payload.signature の中央) を 1 文字書き換える
        // → 署名と payload が一致しなくなり検証失敗
        int firstDot  = token.indexOf('.');
        int secondDot = token.indexOf('.', firstDot + 1);
        int mid = (firstDot + secondDot) / 2;
        char original = token.charAt(mid);
        char replacement = (original == 'A') ? 'B' : 'A';
        String tampered = token.substring(0, mid) + replacement + token.substring(mid + 1);

        assertThat(u.validateAndGetSubject(tampered)).isEmpty();
    }

    @Test
    @DisplayName("セキュリティ: 完全に不正な文字列 → empty (例外を呼び出し側に投げない)")
    void garbage() {
        JwtUtil u = util(60_000);
        assertThat(u.validateAndGetSubject("garbage")).isEmpty();
        assertThat(u.validateAndGetSubject("")).isEmpty();
        assertThat(u.validateAndGetSubject(null)).isEmpty();
    }
}
