# API仕様書

## REST
### 顧客向けAPI
- 商品一覧 -> GET /api/products
- 商品詳細 -> GET /api/products/{id}

- 注文登録 -> POST /api/orders

### 管理者向けAPI
- 管理人ログイン -> POST /api/admin/login

- 商品登録(管理) -> POST /api/admin/products
- 商品一覧（管理） -> GET /api/admin/products
- 商品更新(管理) -> PUT /api/admin/products/{id}
- 商品削除(管理) -> DELETE /api/admin/products/{id}

- 注文一覧取得 -> GET /api/admin/orders
- 注文ステータス変更 -> PUT /api/admin/orders/{id}/status


### 商品一覧

#### 基本情報
##### GET `/api/products`
- エンドポイント -> /api/products
- HTTPメソッド -> GET
- 概要 -> 商品一覧を取得する（最大30件、名称の簡易検索 `q` に対応）

#### リクエスト
##### クエリパラメータ
| 名称 | 型 | 必須 | 説明 |
|--------|-------|------|-------------|
| q | string | 任意 | 商品名の部分一致検索 |
| limit | integer | 任意 | 取得件数(未指定時30、最大30) |

#### レスポンス
##### 200 OK
配列形式で商品一覧を返す。

##### 商品オブジェクトの構造
|  フィールド名  |  型  |  説明  |
|----------------|------------|-------------------------|
| id | integer | 商品ID |
| name | string | 商品名 |
| price | integer | 商品の価格 |
| imageUrl | string | 商品の画像 |


## 商品詳細
### GET /api/products/{id}
#### 基本情報
- エンドポイント: `/api/products/{id}`
- メソッド: GET
- 概要: ID番号を指定して、その商品の情報を返すAPI

#### リクエスト
##### パスパラメータ
| 名称 | 型 | 必須 | 説明 |
|--------|------|--------|-----------|
| id | integer | 必須 | 取得する商品のID |

#### レスポンス
##### 200 OK
1件の商品情報をオブジェクト形式で返す。

| フィールド名 | 型 | 説明 |
|--------------|---------|------------------|
| id        | integer | 商品ID |
| name      | string | 商品名 |
| price     | integer | 商品の価格 |
|imageUrl   | string | 商品の画像 |


## 注文登録
### POST /api/orders
#### 基本情報
 - エンドポイント: `/api/orders`
 - メソッド: POST
 - 概要: 代引き注文登録を行うAPI

#### リクエスト
##### リクエストボディ(JSON: トップレベル)
| フィールド名 | 型    | 必須 | 説明 |
|------------|-------|-----|---------------|
| items      | array | 必須 | 注文する商品の配列 |
| customer   | object | 必須 | 購入者の情報 |
| paymentMethod | string | 必須 | 支払い方法(今回は固定で COD) |

#### リクエストボディ
##### items(1件分の構造)
| フィールド名 | 型     | 必須 | 説明 |
|--------------|--------|------|----------------|
|productId     | integer | 必須  | 商品のID |
|quantity      | integer | 必須  | 注文する数量 |

##### customer(購入者情報)
| フィールド名 | 型      | 必須 | 説明 |
|-------------|------------|--------|------------|
| name        | string | 必須 | フルネーム |
| email       | string | 必須 | メールアドレス |
| address     | string | 必須 | 住所 |

#### レスポンス
#####　201 Created
##### Location: /api/orders/{orderNumber}
注文処理が正常に完了した場合、注文情報を返す。

| フィールド名 | 型      | 説明 |
|------------|-----------|---------------------|
| orderNumber   | string | 注文番号 |
| totalAmount   | integer | 合計金額 |
| status        | string | 注文処理のステータス |


## 管理人ログイン
### POST /api/admin/login
#### 基本情報
- エンドポイント: `/api/admin/login`
- メソッド: POST
- 概要: 管理者がログインするためのAPI

#### リクエスト
#####　リクエストボディ(JSON)
| フィールド名 | 型        | 必須 | 説明 |
|--------------|-----------|-------|---------------|
| username    | string | 必須 | 管理者の名前 |
| password    | string | 必須 | ログイン時に使用するパスワード |

#### レスポンス
#####　200 OK
ログインに成功した場合、認証用のトークンを返す。

| フィールド名 | 型     | 説明 |
|------------|-----------|----------------------------------|
| token      | string    | ログイン状態を証明するためのJWTトークン |

###### バリデーション
- username
 - 必須: true
 - 文字数: 8~20文字
 - 備考: 英数字・記号を許可。ただし記号のみの入力は不可

- password
 - 必須: true
 - 文字数: 8~100文字
 - 備考: 
  - 英数字混在が望ましい
  - 空白のみは禁止
  - 記号は使用可(任意)

### エラー
#### 基本形(401/403/404/500)
```json
{
    "message": "string",
    "code": "string",
    "traceId": "string"
}
```
####　バリデーション時(400のみ)
```json
{
    "message": "validation error",
    "code": "VALIDATION_ERROR",
    "traceId": "string",
    "errors": {
        "field": ["reason"]
    }
}
```
##### エラーコード一覧
| HTTP | code |
|------|------------------|
| 400 | VALIDATION_ERROR |
| 401 | UNAUTHORIZED |
| 401 | TOKEN_EXPIRED |
| 401 | TOKEN_INVALID |
| 401 | INVALID_CREDENTIALS |
| 404 | NOT_FOUND |
| 500 | INTERNAL_ERROR |

###### どの条件でどのcodeを返すか
UNAUTHORIZED：Authorizationヘッダ未送信
TOKEN_EXPIRED：期限切れ
TOKEN_INVALID：検証失敗/形式不正
INVALID_CREDENTIALS：ログインのID/PW不一致

### 共通エラーレスポンス仕様

- 全てのエラーは共通フォーマット `ErrorResponse` を返す
- `code` はフロントエンドが処理分岐するための固定値
- `traceId` は障害調査用の識別子
- HTTP 400 の場合のみ `errors` フィールドを含む



## 管理人ログアウト
### POST /api/admin/logout
#### 基本情報
- エンドポイント: `/api/admin/logout`
- メソッド: POST
- 備考: 管理者がログアウトするAPI

#### リクエスト
##### ヘッダー
| 名称 | 必須 | 説明 |
|---------|--------|----------------|
| Authorization | 必須 | `Bearer <JWT>` を指定する |

#### レスポンス
##### 200 OK
```json
{
    "message": "logged out"
}
```
#### エラー
##### 401 Unauthorized
```json
{
    "message": "unauthorized",
    "code": "UNAUTHORIZED",
    "traceId": "string"
}
```

##### 500 Internal Server Error
```json
{
    "message": "internal server error",
    "code": "INTERNAL_ERROR",
    "traceId": "string"
}
```


## 商品一覧(管理)
### GET /api/admin/products
#### 基本情報
 - エンドポイント: `/api/admin/products`
 - メソッド: GET
 - 概要: 管理者が商品一覧を取得する（最大30件、名称の簡易検索 `q` に対応）

#### リクエスト
##### クエリパラメータ
| 名称 | 型 | 必須 | 説明 |
|--------|-------|------|-------------|
| q | string | 任意 | 商品名の部分一致検索 |
| limit | integer | 任意 | 取得件数(未指定時30、最大30) |

##### ヘッダー
| 名称 | 必須 | 説明 |
|--------|------|-------------|
| Authorization | 必須 | `Bearer <JWT>` |

#### レスポンス
##### 200 OK
配列形式で商品一覧を返す。

| フィールド名 | 型 | 説明 |
|--------------|----------|-----------------|
| id | integer | 商品ID |
| name | string | 商品名 |
| price | integer | 金額 |
| imageUrl | string | 商品画像 |

#### エラー
 - 401 Unauthorized(トークンなし/不正)


## 商品登録(管理)
### POST /api/admin/products
#### 基本情報
 - エンドポイント: `/api/admin/products`
 - メソッド: POST
 - 概要: 管理者が商品の新規登録

#### リクエスト
##### ヘッダー
| 名称 | 必須 | 説明 |
|---------|-----|-----------|
| Authorization | 必須 | `Bearer <JWT>` |

##### リクエストボディ(JSON)
| フィールド名 | 型 | 必須 | 説明 |
|------------|-------|------|----------------|
| name | string | 必須 | 商品名 |
| price | integer | 必須 | 価格 |
| imageUrl | string | 任意 | 商品画像URL |

#### レスポンス
##### 201 Created
登録した商品情報を返す(例)。

| フィールド名 | 型 | 説明 |
|-------------|-------|---------|
| id | integer | 商品ID |
| name | string | 商品名 |
| price | integer | 価格 |
| imageUrl | string | 商品画像URL |

#### バリデーション

 - name
  - 必須: true
  - 文字数: 1~100文字
  - 備考: 空白のみは禁止

 - price
  - 必須: true
  - 範囲: 1~999999
  - 形式: 整数のみ
  - 備考: 0以下・小数は不可

 - imageUrl
  - 必須: false
  - 文字数: 1~255文字
  - 備考: URL形式(`/images/...`のような相対パスも許可)

#### エラー
 - 400 Bad Request(バリデーションNG)
 - 401 Unauthorized

##### 400 Bad Request
バリデーションエラーや必須項目の欠落など、リクエスト形式が不正な場合。
```json
{
    "message": "validation error",
    "code": "VALIDATION_ERROR",
    "traceId": "string",
    "errors": {
        "name": ["name is required"],
        "price": ["price must be an integer between 1 and 999999"]
    }
}
```

##### 401 Unauthorized
認証トークンが存在しない、または無効な場合。
```json
{
    "message": "unauthorized",
    "code": "UNAUTHORIZED",
    "traceId": "string"
}
```


## 商品削除(管理)
### DELETE /api/admin/products/{id}
#### 基本情報
 - エンドポイント: `/api/admin/products/{id}`
 - メソッド: DELETE
 - 概要: 管理者が指定した商品を削除する

#### リクエスト
##### パスパラメータ
| 名称 | 型 | 必須 | 説明 |
|--------|-----|--------|-----------|
| id | integer | 必須 | 削除対象の商品ID |

##### ヘッダー
| 名称 | 必須 | 説明 |
|-------|-----|----------|
| Authorization | 必須 | `Bearer <JWT>` |

#### レスポンス
##### 204 No Content
削除成功時はレスポンスボディを返さない。

#### エラー
##### 401 Unauthorized
```json
{
    "message": "unauthorized",
    "code": "UNAUTHORIZED",
    "traceId": "string"
}
```

##### 404 Not Found
指定したIDの商品が存在しない場合。
```json
{
    "message": "product not found",
    "code": "NOT_FOUND",
    "traceId": "string"
}
```


## 商品更新(管理)
### PUT /api/admin/products/{id}
#### 基本情報
 - エンドポイント: `/api/admin/products/{id}`
 - メソッド: PUT
 - 概要: 管理者が指定した商品情報を更新する

####　リクエスト
#####　パスパラメータ
| 名称 | 型 | 必須 | 説明 |
|--------|-----|------|-------------|
| id | integer | 必須 | 更新対象の商品ID |

##### ヘッダー
| 名称 | 必須 | 説明 |
|-------|-----|-------------|
| Authorization | 必須 | `Bearer <JWT>` |

##### リクエストボディ(JSON)
| フィールド名 | 型 | 必須 | 説明 |
|------------|-------|--------|-------------|
| name | string | 必須 | 商品名 |
| price | integer | 必須 | 価格 |
| imageUrl | string | 任意 | 商品画像URL |


#### バリデーション
- name
 - 必須: true
 - 文字数: 1~100文字
 - 備考: 空白のみは禁止

- price
 - 必須: true
 - 文字数: 1~999999
 - 形式: 整数のみ
 - 備考: 0以下・小数は不可

- imageUrl
 - 必須: false
 - 文字数: 1~255文字
 - 備考: URL形式(`/images/...`のような相対パスも許可)

 #### レスポンス
 #####　200 OK
 更新後の商品情報を返す(例).

 | フィールド名 | 型 | 説明 |
 |------------|-------|---------|
 | id | integer | 商品ID |
 | name | string | 商品名 |
 | price | integer | 価格 |
 | imageUrl | string | 商品画像URL |

 #### エラー
 #####　400 Bad Request
 バリデーションエラーなど、リクエストが不正な場合。

 ```json
 {
    "message": "validation error",
    "code": "VALIDATION_ERROR",
    "traceId": "string",
    "errors": {
        "name": ["name is required"],
        "price": ["price must be an integer between 1 and 999999"]
    }
 }
 ```

 ##### 401 Unauthorized
 ```json
 {
    "message": "unauthorized",
    "code": "UNAUTHORIZED",
    "traceId": "string"
 }
 ```

 ##### 404 Not Found
 指定したIDの商品が存在しない場合。
 ```json
 {
    "message": "product not found",
    "code": "NOT_FOUND",
    "traceId": "string"
 }
 ```

## 注文一覧取得
### GET /api/admin/orders
#### 基本情報
 - エンドポイント: `/api/admin/orders`
 - メソッド: GET
 - 概要: 代引き注文一覧取得を行うAPI

##### ヘッダー
| 名称 | 必須 | 説明 |
|--------|------|-------------|
| Authorization | 必須 | `Bearer <JWT>` |

#### リクエスト
##### リクエストボディ(JSON: トップレベル)
| フィールド名 | 型    | 必須 | 説明 |
|------------|-------|-----|---------------|
| items      | array | 必須 | 注文する商品の配列 |
| customer   | object | 必須 | 購入者の情報 |
| paymentMethod | string | 必須 | 支払い方法(今回は固定で COD) |

#### リクエストボディ
##### items(1件分の構造)
| フィールド名 | 型     | 必須 | 説明 |
|--------------|--------|------|----------------|
|productId     | integer | 必須  | 商品のID |
|quantity      | integer | 必須  | 注文する数量 |

##### customer(購入者情報)
| フィールド名 | 型      | 必須 | 説明 |
|-------------|------------|--------|------------|
| name        | string | 必須 | フルネーム |
| email       | string | 必須 | メールアドレス |
| address     | string | 必須 | 住所 |

#### レスポンス
#####　200
##### Location: /api/admin/orders/{orderNumber}
注文処理が正常に完了した場合、注文情報を返す。

| フィールド名 | 型      | 説明 |
|------------|-----------|---------------------|
| orderNumber   | string | 注文番号 |
| totalAmount   | integer | 合計金額 |
| status        | string | 注文処理のステータス |

## 注文ステータス変更
### PUT /api/admin/orders/{id}/status
#### 基本情報
 - エンドポイント: `/api/admin/orders/{id}/status`
 - メソッド: PUT
 - 概要: 注文した時のステータスを変更するAPI

#### リクエスト
##### パスパラメータ
| 名称 | 型 | 必須 | 説明 |
|--------|-----|------|-------------|
| id | integer | 必須 | 更新対象の注文ID |

#### ヘッダー
| 名称 | 必須 | 説明 |
|-------|-----|-------------|
| Authorization | 必須 | `Bearer <JWT>` |

##### リクエストボディ(JSON)
| フィールド名 | 型 | 必須 | 説明 |
|------------|-------|--------|-------------|
| status | string | 必須 | 注文した際のステータスの変更 |

```json
{
    "status": "PENDING",
    "status": "SHIPPED",
    "status": "DELIVERED",
    "status": "CANCELLED"
}
```
enum
PENDING | SHIPPED | DELIVERED | CANCELLED

##### 状態偏移
###### 許可
 - PENDING -> SHIPPED
 - PENDING -> CANCELLED
 - SHIPPED -> DELIVERED
それ以外: すべて拒否

#### レスポンス
##### 200 OK
```json
{
  "id": 123,
  "status": "SHIPPED",
  "updatedAt": "2026-01-11T08:00:00+09:00"
}
```

##### 400 Bad Request
 - status不正(enum外、必須違反)
```json
 {
    "message": "validation error",
    "code": "VALIDATION_ERROR",
    "traceId": "string",
    "errors": {
        "status": ["status is required"],
    }
 }
```

##### 401 Unauthorized
 - 管理者認証NG
```json
 {
    "message": "unauthorized",
    "code": "UNAUTHORIZED",
    "traceId": "string"
 }
 ```

##### 404 Not Found
 - 注文が存在しない
 ```json
 {
    "message": "product not found",
    "code": "NOT_FOUND",
    "traceId": "string"
 }
 ```

##### 409 Conflict
 - 状態遷移違反
```json
{
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Cannot change status from SHIPPED to CANCELLED",
  "currentStatus": "SHIPPED"
}
```