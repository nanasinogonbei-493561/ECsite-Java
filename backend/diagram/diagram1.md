# 日本酒ECサイト 基本設計書（React + Spring Boot / MVP）

1. システム全体構成

1.1 アーキテクチャ
・ フロント：React19 + TypeScript(Vite)、UIは最小(Bootstrap)
・ API：Spring Boot 3(REST, JSON)
・ DB：開発＝H2/本番＝MySQL
・ 画像：開発＝ローカル、本番＝Nginx配信(VPS)
・ 認証：管理画面のみ(セッション or JWT)。顧客は非ログイン
・ 年齢確認：フロント側でDoB入力→sessionStorage管理＋API側でもガード可
・ CORS：フロントhttp://localhost:5173(Dev)からAPI http://localhost:8080へ

React(Vite) → REST(JSON) → Spring Boot → JPA → DB
           ↘ 画像GET (/image/*) via Nginx

