# 日本酒ECサイト 基本設計書（React + Spring Boot / MVP）

1. システム全体構成
1.1 アーキテクチャ

フロント：React 19 + TypeScript（Vite）、UIは最小（Bootstrap or DaisyUI）

API：Spring Boot 3（REST、JSON）

DB：Dev=H2 / Prod=PostgreSQL or MySQL

画像：Dev=ローカル、Prod=Cloudinary or Nginx配信（VPS）

認証：管理画面のみ（セッション or JWT）。顧客は非ログイン

年齢確認：フロント側でDoB入力→sessionStorage管理＋API側でもガード可

CORS：フロントhttp://localhost:5173（Dev）からAPIhttp://localhost:8080へ

React(Vite) → REST(JSON) → Spring Boot → JPA → DB
           ↘ 画像GET (/images/*) via Nginx/Cloudinary

2. リポジトリ構成（モノレポ推奨）
sake-ec/
├─ frontend/   # React + TS
│  ├─ src/
│  │  ├─ app/ (router, providers)
│  │  ├─ pages/ (public, admin)
│  │  ├─ components/
│  │  ├─ features/ (products, cartless-order, auth)
│  │  ├─ services/ (apiClient.ts, products.ts, orders.ts, admin.ts)
│  │  └─ types/ (dto.ts, models.ts)
│  └─ .env.local (VITE_API_BASE_URL=http://localhost:8080)
└─ backend/    # Spring Boot
   ├─ src/main/java/com/example/sakeec/
   │  ├─ config/ (Cors, Security, WebMvc)
   │  ├─ controller/ (public, admin)
   │  ├─ dto/ (request/response)
   │  ├─ service/ (query/command)
   │  ├─ domain/ (model, repository)
   │  ├─ infra/ (jpa, storage, mail)
   │  └─ util/
   └─ src/main/resources/ (application.yml, schema/data など)

3. API設計（OpenAPI雛形）
3.1 リソースとエンドポイント（MVP）

Public（認証不要）

GET /api/products（検索q、ページングpage,size）

GET /api/products/{id}

POST /api/orders/preview（合計試算; 任意）

POST /api/orders（確定：代引き前提）

Admin（要認証）

POST /api/admin/login（フォーム or JSON）

GET /api/admin/products

POST /api/admin/products

PUT /api/admin/products/{id}

DELETE /api/admin/products/{id}

GET /api/admin/orders

GET /api/admin/orders/{id}

PUT /api/admin/orders/{id}/status

3.2 リクエスト/レスポンス（抜粋）
// frontend/src/types/dto.ts
export type ProductSummary = {
  id: number; name: string; price: number; volume: number;
  imageUrl: string | null; inStock: boolean;
};
export type ProductDetail = ProductSummary & {
  brewery?: string; alcoholContent?: number; description?: string; stockQuantity: number;
};

export type PlaceOrderRequest = {
  productId: number;
  quantity: 1|2|3;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  deliveryAddress: string;
};
export type PlaceOrderResponse = { orderNumber: string; totalAmount: number; createdAt: string; };

export type AdminLoginRequest = { username: string; password: string; };
export type AdminLoginResponse = { token: string }; // セッションの場合はSet-Cookie

3.3 OpenAPI（最小の骨格）
openapi: 3.0.3
info: { title: Sake EC API, version: "0.1.0" }
servers: [{ url: http://localhost:8080 }]
paths:
  /api/products:
    get:
      parameters:
        - { name: q, in: query, schema: { type: string } }
        - { name: page, in: query, schema: { type: integer, default: 0 } }
        - { name: size, in: query, schema: { type: integer, default: 20 } }
      responses:
        "200": { description: OK }
  /api/products/{id}:
    get:
      responses: { "200": { description: OK }, "404": { description: Not Found } }
  /api/orders:
    post:
      requestBody: { required: true }
      responses:
        "201": { description: Created }

4. バックエンド詳細設計
4.1 Maven 依存（抜粋）
<dependencies>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
  <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
  <!-- prod用: postgresql or mysql ドライバ -->
</dependencies>

4.2 CORS / Security（最小）
// CORS: devフロントを許可
@Bean CorsConfigurationSource cors() {
  var cfg = new CorsConfiguration();
  cfg.setAllowedOrigins(List.of("http://localhost:5173"));
  cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE"));
  cfg.setAllowedHeaders(List.of("*"));
  cfg.setAllowCredentials(true);
  var src = new UrlBasedCorsConfigurationSource();
  src.registerCorsConfiguration("/**", cfg);
  return src;
}

@Bean SecurityFilterChain http(HttpSecurity h) throws Exception {
  h.csrf(csrf -> csrf.disable())       // 管理画面がCookieセッションならenableを検討
   .cors(Customizer.withDefaults())
   .authorizeHttpRequests(a -> a
      .requestMatchers("/api/admin/**").authenticated()
      .anyRequest().permitAll()
   )
   .httpBasic(Customizer.withDefaults()); // MVP簡易。後でformLogin/JWTに
  return h.build();
}

4.3 エンティティ＆リポジトリ（在庫は楽観ロック）
@Entity class Product {
  @Id @GeneratedValue Long id;
  String name; String brewery;
  BigDecimal price; Integer volume;
  BigDecimal alcoholContent;
  String description; String imagePath;
  Integer stockQuantity;
  @Version Integer version;
  LocalDateTime createdAt; LocalDateTime updatedAt;
  void reduceStock(int qty){ if(qty<1||qty>3) throw new IllegalArgumentException(); if(stockQuantity<qty) throw new IllegalStateException(); stockQuantity-=qty; }
}

public interface ProductJpa extends JpaRepository<Product,Long> {
  @Query("select p from Product p where (:q is null or lower(p.name) like lower(concat('%',:q,'%')))")
  Page<Product> search(@Param("q") String q, Pageable pageable);
}

4.4 注文ユースケース
@Service @Transactional
public class PlaceOrderService {
  private final ProductJpa products; private final OrderJpa orders; private final OrderNumberGenerator gen;
  public PlaceOrderResponse place(PlaceOrderRequest r){
    var p = products.findById(r.productId()).orElseThrow();
    p.reduceStock(r.quantity());
    var o = OrderFactory.create(r, p); // 合計計算、明細作成、status=RECEIVED
    orders.save(o);
    return new PlaceOrderResponse(o.getOrderNumber(), o.getTotalAmount(), o.getCreatedAt());
  }
}

4.5 application.yml（プロファイル）
spring:
  jpa:
    hibernate.ddl-auto: update
    open-in-view: false
---
spring:
  config.activate.on-profile: dev
  datasource:
    url: jdbc:h2:mem:sake;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    username: sa
  jpa.show-sql: true
---
spring:
  config.activate.on-profile: prod
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}

5. フロントエンド詳細設計
5.1 ルーティング（最小）
/age            年齢確認
/               商品一覧 + 検索
/products/:id   商品詳細（ここから注文へ） 
/orders/new     注文フォーム
/orders/confirm 注文確認
/orders/complete 注文完了(番号表示)

/admin/login    管理ログイン
/admin/products, /admin/products/new, /admin/products/:id/edit
/admin/orders, /admin/orders/:id

5.2 年齢確認（実装方針）

DoB入力→20歳未満はエラーメッセージ

sessionStorage.setItem('AGE_VERIFIED','1')

ルートガード（/, /products/*, /orders/*）で未検証なら/ageへ

5.3 APIクライアント（axios 例）
// services/apiClient.ts
import axios from "axios";
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true, // セッション方式の場合
});

// services/products.ts
import { api } from "./apiClient";
export async function fetchProducts(q?: string, page=0, size=20){
  const res = await api.get("/api/products", { params: { q, page, size }});
  return res.data as { content: ProductSummary[]; totalPages: number; };
}
export async function fetchProduct(id: number){
  const res = await api.get(`/api/products/${id}`);
  return res.data as ProductDetail;
}

// services/orders.ts
export async function placeOrder(body: PlaceOrderRequest){
  const res = await api.post("/api/orders", body);
  return res.data as PlaceOrderResponse;
}

5.4 画面骨格（例）
// app/router.tsx (React Router v7)
import { createBrowserRouter } from "react-router-dom";
export const router = createBrowserRouter([
  { path:"/age", element:<AgeGatePage/> },
  { path:"/", element:<RequireAge><ProductListPage/></RequireAge> },
  { path:"/products/:id", element:<RequireAge><ProductDetailPage/></RequireAge> },
  { path:"/orders/new", element:<RequireAge><OrderFormPage/></RequireAge> },
  { path:"/orders/confirm", element:<RequireAge><OrderConfirmPage/></RequireAge> },
  { path:"/orders/complete", element:<RequireAge><OrderCompletePage/></RequireAge> },
  { path:"/admin/login", element:<AdminLoginPage/> },
  { path:"/admin/products", element:<RequireAdmin><AdminProductsPage/></RequireAdmin> },
  // …
]);

// components/RequireAge.tsx
export function RequireAge({children}:{children:React.ReactNode}){
  const ok = sessionStorage.getItem("AGE_VERIFIED")==="1";
  if(!ok){ location.href="/age"; return null; }
  return <>{children}</>;
}

5.5 型・バリデーション

UI側もZod/Yupなどで軽い入力検証（メール形式、数量1–3）

API側はBean Validationで厳密検証（二重化で安全）

6. 画像アップロード設計（MVP）

管理画面から1枚のみアップロード

VPS：/var/app/images/へ保存→Nginxで/images/*を静的公開

Render：CloudinaryへPOST→返却URLをimagePathに保存

フロントは<img src={product.imageUrl ?? '/noimage.png'} />

7. ステータスとユースケース
7.1 注文の状態遷移

RECEIVED → SHIPPED → COMPLETED（それ以外はエラー）

7.2 シーケンス（注文）
Client → GET /api/products/{id}
Client → POST /api/orders { productId, quantity, … }
API   → Product.findById → reduceStock(@Version) → Order.save
API   → 201 Created { orderNumber, totalAmount, createdAt }
Client → /orders/complete へ遷移

8. 非機能（ログ/例外/性能）

ログ：MVPはINFO中心（将来JSON構造化に拡張）

例外：@ControllerAdviceでValidation/IllegalStateを400, 在庫競合を409

ページング：一覧は20件固定（APIはpage,sizeで調整可）

9. 環境変数（再掲）
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
CLOUDINARY_URL=...
MAIL_PASSWORD=...


フロント .env：

VITE_API_BASE_URL=http://localhost:8080

10. デプロイ要点
10.1 Render

Backend: Build=mvn -DskipTests package, Start=java -jar target/app.jar

PostgreSQLアドオンと環境変数設定

CORSのAllowedOriginに本番フロントURLを追加

10.2 ConoHa VPS + Docker

docker-compose.yml（app, db, nginx）

Nginxで/api→Spring、/images→ローカル静的、Let’s Encrypt

11. 作業手順（20日計画に直結）

Initializr（Web/JPA/Security/Validation/H2、Maven、Java17）

APIスケルトン（Products GET, Product GET, Orders POST）

エンティティ/リポジトリ/楽観ロック

React雛形（一覧/詳細/注文、AgeGate、APIクライアント）

管理ログイン簡易 & Product CRUD

画像アップロード（VPS=ローカル or Render=Cloudinary）

例外/バリデーション/メッセージ

デプロイ設定（Render or VPS）＋動作確認