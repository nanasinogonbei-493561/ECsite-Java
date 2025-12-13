日本酒ECサイト 基本設計書（MVP / 1人開発）
1. 目的・スコープ

要件定義の「機能要件（高）」を1ヶ月で実装するための最小構成設計。

Spring Boot + Thymeleaf + Maven、DBは開発H2/本番PostgreSQL or MySQL。

2. 全体アーキテクチャ／フォルダ構成
2.1 レイヤ構造
Presentation(Controller, View) 
  → Application(Service, UseCase)
    → Domain(Entity, Domain Service) 
      → Infrastructure(JPA Repository, File/Email Adapter)


com.example.sakeec
├─ config/               # セキュリティ、WebMvc、メール、ストレージ設定
├─ controller/           # MVCコントローラ(@Controller)
│   ├─ public/           # 顧客向け
│   └─ admin/            # 管理者向け
├─ dto/                  # フォーム/レスポンスDTO
├─ service/              # アプリケーションサービス
├─ domain/
│   ├─ model/            # エンティティ集約
│   │   ├─ product/
│   │   ├─ order/
│   │   └─ admin/
│   └─ repository/       # ドメインリポジトリインターフェース
├─ infra/
│   ├─ repository/       # Spring Data JPA実装
│   ├─ storage/          # 画像保存Adapter(ローカル/Cloudinary)
│   └─ mail/             # メール送信Adapter
└─ util/                 # 共通(注文番号生成、日付、マスク等)


src/main/resources/templates/
├─ public/
│   ├─ age-verification.html
│   ├─ index.html
│   ├─ product-detail.html
│   ├─ order-form.html
│   ├─ order-confirm.html
│   └─ order-complete.html
└─ admin/
    ├─ login.html
    ├─ products.html
    ├─ product-form.html
    ├─ orders.html
    └─ order-detail.html

3. ドメインモデル
3.1 主要エンティティ（集約と不変条件）

Product（集約）

属性：id, name, brewery, price, volume, alcoholContent, description, imagePath, stockQuantity, createdAt, updatedAt, version

不変条件：price>=0、stockQuantity>=0、alcoholContent 0–100

振る舞い：reduceStock(qty)（在庫 < qty は例外）

Order（集約）

Header: id, orderNumber(一意), customerName, email, phone, address, totalAmount, status, createdAt

Items: List<OrderItem>（productId, quantity, unitPrice, subtotal）

不変条件：quantity 1–3、totalAmount = Σ(subtotal)

振る舞い：confirm() → markShipped() → complete()

Admin（集約）

username(unique), password(BCrypt)


3.2 リポジトリ（インターフェース）
interface ProductRepository { Optional<Product> findById(Long id); Page<Product> searchByName(String q, Pageable p); Product save(Product p); }

interface OrderRepository { Optional<Order> findByOrderNumber(String no); Order save(Order o); Page<Order> findAll(Pageable p); }

interface AdminRepository { Optional<Admin> findByUsername(String u); }



4. アプリケーションサービス（ユースケース）
4.1 顧客向け

ProductQueryService

一覧取得（ページング、名前検索）

詳細取得

PlaceOrderService

入力検証 → 商品＆在庫確認 → 合計計算 → 注文番号生成 → 在庫引当（悲観/楽観ロック） → 保存

（任意）確認メール送信（Phase: 中）

4.2 管理者向け

AdminAuthService：ログイン認証

ProductAdminService：商品CRUD（画像アップロード1枚）

OrderAdminService：受注一覧・詳細、ステータス遷移（受注→発送済み→完了）

5. プレゼンテーション（ルーティング & 画面）
5.1 パブリック（年齢確認含む）

GET /age-verification：フォーム表示

POST /age-verification：生年月日→20歳未満は拒否、OKならsession.setAttribute("AGE_VERIFIED", true)

GET /：商品一覧（検索：?q=）

GET /products/{id}：詳細

GET /orders/new?productId=：注文フォーム（数量1–3）

POST /orders/confirm：確認画面へ（入力DTOをセッションかHiddenで持ち回り）

POST /orders/complete：確定（在庫更新＆保存）

5.2 管理

GET /admin/login、POST /admin/login

GET /admin/products、GET /admin/products/new、POST /admin/products

GET /admin/products/{id}/edit、POST /admin/products/{id}

POST /admin/products/{id}/delete

GET /admin/orders、GET /admin/orders/{id}、POST /admin/orders/{id}/status

6. 非機能設計（セキュリティ／バリデーション／セッション）
6.1 年齢確認（HandlerInterceptor）

AgeVerificationInterceptor：/public/** へアクセス時に AGE_VERIFIED 無ければ /age-verification へリダイレクト
（管理画面、静的ファイルは除外）

6.2 管理者認証（Spring Security, 最小構成）

ログインフォーム + セッション

パス：/admin/** に認可、/admin/login は許可

パスワード：BCrypt（初期ユーザをFlyway/Liquibaseで投入 or 環境変数）
@Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  http.authorizeHttpRequests(auth -> auth
      .requestMatchers("/admin/login", "/css/**", "/js/**", "/images/**", "/age-verification", "/", "/products/**", "/orders/**").permitAll()

      .requestMatchers("/admin/**").authenticated()
  ).formLogin(login -> login

      .loginPage("/admin/login").defaultSuccessUrl("/admin/products", true)

      .permitAll()

  ).logout(l -> l.logoutUrl("/admin/logout").logoutSuccessUrl("/"));
  return http.build();
}



6.3 入力バリデーション（例）

注文フォーム：名前必須(1–100)、メール形式、電話 数字/-、住所必須(1–1000)、数量 1–3

商品：名前必須、価格>=0、在庫>=0、アルコール度数 0–100

6.4 在庫整合性

楽観ロック推奨：@Version を Product に付与
競合時は「在庫が変動しました」のエラーメッセージで再入力へ戻す



7. データベース／スキーマ
7.1 DDL（JPA前提のイメージ）
-- products
id BIGINT PK AUTO_INCREMENT,
name VARCHAR(255) NOT NULL,
brewery VARCHAR(100),
price DECIMAL(10,2) NOT NULL,
volume INT NOT NULL,
alcohol_content DECIMAL(3,1),
description TEXT,
image_path VARCHAR(255),
stock_quantity INT NOT NULL,
created_at TIMESTAMP,
updated_at TIMESTAMP,
version INT NOT NULL

-- orders
id BIGINT PK AUTO_INCREMENT,
order_number VARCHAR(20) UNIQUE NOT NULL,
customer_name VARCHAR(100) NOT NULL,
customer_email VARCHAR(255) NOT NULL,
customer_phone VARCHAR(20),
delivery_address TEXT NOT NULL,
total_amount DECIMAL(10,2) NOT NULL,
status VARCHAR(20) NOT NULL,
created_at TIMESTAMP

-- order_items
id BIGINT PK AUTO_INCREMENT,
order_id BIGINT NOT NULL,
product_id BIGINT NOT NULL,
quantity INT NOT NULL,
unit_price DECIMAL(10,2) NOT NULL

-- admins
id BIGINT PK AUTO_INCREMENT,
username VARCHAR(50) UNIQUE NOT NULL,
password VARCHAR(255) NOT NULL


7.2 エンティティ（抜粋）
@Entity
class Product {
  @Id @GeneratedValue Long id;
  String name; String brewery;
  BigDecimal price; Integer volume;
  BigDecimal alcoholContent;
  String description; String imagePath;
  Integer stockQuantity;
  @Version Integer version;
  LocalDateTime createdAt; LocalDateTime updatedAt;

  void reduceStock(int qty){
    if(qty <= 0) throw new IllegalArgumentException();
    if(stockQuantity < qty) throw new IllegalStateException("在庫不足");
    stockQuantity -= qty;
  }
}

@Entity
class Order {
  @Id @GeneratedValue Long id;
  String orderNumber;
  String customerName, customerEmail, customerPhone, deliveryAddress;
  BigDecimal totalAmount;
  @Enumerated(EnumType.STRING) OrderStatus status;
  LocalDateTime createdAt;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "order") List<OrderItem> items = new ArrayList<>();
}




8. API/画面I/F仕様（MVP最小）
8.1 画面I/F（HTTP + HTML）

入出力項目は「要件定義の項目＋バリデーション」をフォームにマッピング

画像アップロード：multipart/form-data、拡張子・最大サイズ制限（例：~2MB）

8.2 ステータス遷移

RECEIVED(受注) → SHIPPED(発送済み) → COMPLETED(完了)

不正遷移はバリデーションエラー



9. クロスカット（メール、画像、設定、ロギング）
9.1 メール（任意）

MailSender をラップした OrderMailService（テンプレ⇢Thymeleaf mail/order-confirm.html）

9.2 画像保存

ConoHa VPS：/var/app/images/ 等（Nginxで静的配信）

Render：Cloudinary推奨（CLOUDINARY_URL）

9.3 設定（Profiles）

src/main/resources/application.yml
spring:
  thymeleaf: { cache: false }
  jpa:
    hibernate.ddl-auto: update
    open-in-view: false
---
spring:
  config.activate.on-profile: dev
  datasource:
    url: jdbc:h2:mem:sake;MODE=MySQL;DB_CLOSE_DELAY=-1
    username: sa
    password:
  jpa.show-sql: true
---
spring:
  config.activate.on-profile: prod
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}




10. セッション設計

AGE_VERIFIED（Boolean）…ブラウザ閉じるまで有効

注文確認の入力値保持はPOST→Redirect→GET + サーバー側セッションに一時保存（CSRF対策有効）

11. 例外・エラーUX

在庫競合：メッセージ「在庫が更新されました。再度数量を確認してください」

入力不正：項目別エラー表示

500系：共通エラーページ /error（シンプル）

12. ビルド & 依存（Maven）

pom.xml（抜粋）
<dependencies>
  <dependency> <!-- web & thymeleaf -->
    <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency> <!-- JPA -->
    <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency> <!-- Security -->
    <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope>
  </dependency>
  <!-- 本番は PostgreSQL or MySQL のドライバを追加 -->
  <!-- BootstrapはCDN、Lombokは任意 -->
</dependencies>




13. シーケンス（主要2本・簡易記法）
13.1 注文確定
User → AgeInterceptor: AGE_VERIFIED? (OK)
User → ProductController: GET /products/{id}
User → OrderController: POST /orders/confirm (validate)
OrderController → ProductRepository: findById
OrderController → PlaceOrderService: preview(total)
User → OrderController: POST /orders/complete
PlaceOrderService → Product (reduceStock) [@Version]
PlaceOrderService → OrderRepository: save
(任意) → MailService: sendConfirm
→ View: order-complete




13.2 管理：商品登録
Admin → Auth: POST /admin/login
Admin → ProductAdminController: GET /admin/products/new
Admin → ProductAdminController: POST /admin/products (validate)
ProductAdminService → StorageAdapter: save(image)
→ ProductRepository.save
→ View: redirect /admin/products




14. テスト設計（MVP）

ユニット

Product.reduceStock()、PlaceOrderService（在庫不足/競合）

統合

@WebMvcTest：年齢確認のリダイレクト、フォーム→確認→完了のフロー

@DataJpaTest：リポジトリ、ユニーク制約

E2E（簡易）

H2 + MockMvcで主要導線のみ

15. デプロイ設計（要点のみ）
15.1 ConoHa VPS + Docker

Dockerfile（OpenJDK17-jdk-slim）→ java -jar app.jar

docker-compose.yml（app + mysql + nginx）

Nginx: /→Spring、/images/→静的、Let's Encrypt（certbot）

15.2 Render

Web Service（Gradle/Maven Build Command, Start Command）

PostgreSQLアドオン、環境変数（SPRING_PROFILES_ACTIVE=prod 他）

画像はCloudinary

16. リスクと設計上の手当

在庫競合 → @Versionで楽観ロック／トランザクション境界をPlaceOrderServiceに集約

画像アップロードの安全性 → 拡張子/サイズ制限＋保存先分離

管理画面の露出 → /adminのBasicは禁止、必ずフォーム認証＋CSRF有効

付録：実装ひな形（必要な最小コード断片）
WebMvc（年齢確認）
@Configuration
class WebConfig implements WebMvcConfigurer {
  @Override public void addInterceptors(InterceptorRegistry reg){
    reg.addInterceptor(new AgeVerificationInterceptor())
      .addPathPatterns("/", "/products/**", "/orders/**")
      .excludePathPatterns("/age-verification", "/css/**", "/js/**", "/images/**", "/admin/**");
  }
}

class AgeVerificationInterceptor implements HandlerInterceptor {
  @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object h) throws Exception {
    Boolean ok = (Boolean) req.getSession().getAttribute("AGE_VERIFIED");
    if(Boolean.TRUE.equals(ok)) return true;
    res.sendRedirect("/age-verification");
    return false;
  }
}

注文サービス（在庫更新＋保存の核心）
@Service
@Transactional
class PlaceOrderService {
  private final ProductRepository products; private final OrderRepository orders; private final OrderNumberGenerator gen;

  public String place(PlaceOrderCommand cmd){
    Product p = products.findById(cmd.productId()).orElseThrow();
    p.reduceStock(cmd.quantity());                // @Version で競合検知
    Order o = OrderFactory.create(cmd, p);       // 合計計算＆項目詰め
    orders.save(o);
    return o.getOrderNumber();
  }
}




次アクション（そのまま実作業に落とせます）

Spring Initializr（Maven, Java17, Web/Thymeleaf/Security/JPA/H2）で雛形生成

パッケージ構成を作る → エンティティ & リポジトリ作成

年齢確認Interceptor & ルーティング実装

商品一覧/詳細 → 注文フォーム → 確認 → 完了の一本線を通す

管理ログイン → 商品CRUD → 注文一覧/詳細

在庫の楽観ロックを有効化 → 競合時のメッセージ

デプロイ用のapplication-prod.ymlとDocker/Render設定
