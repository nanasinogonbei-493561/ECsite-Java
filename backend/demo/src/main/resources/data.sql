-- 開発用初期管理者アカウント (username: admin / password: admin1234)
-- パスワードは BCrypt(rounds=10) でハッシュ化済み
-- 本番環境では必ず変更すること
MERGE INTO admins (username, password)
    KEY (username)
    VALUES ('admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoOa9eqoHCKqMVF6v4j7vFqKqM5vv5F5pC');
