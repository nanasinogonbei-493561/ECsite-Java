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


## 商品一覧

## 基本情報
### GET `/api/products`
- エンドポイント -> /api/products
- HTTPメソッド -> GET
- 概要 -> 商品一覧を取得する（最大30件、名称の簡易検索 `q` に対応）

## レスポンス
### 200 OK
配列形式で商品一覧を返す。

#### 商品オブジェクトの構造
|  フィールド名  |  型  |  説明  |
|----------------|------------|-------------------------|
| id | number | 商品ID |
| name | string | 商品名 |
| price | number | 商品の価格 |
| imageUrl | string | 商品の画像 |


### GET /api/products/{id}
#### 基本情報
- エンドポイント: `/api/products/{id}`
- メソッド: GET
- 概要: ID番号を指定して、その商品の情報を返すAPI

## レスポンス
### 200 OK
1件の商品情報をオブジェクト形式で返す。

| フィールド名 | 型 | 説明 |
|--------------|---------|------------------|
| id        | number | 商品ID |
| name      | string | 商品名 |
| price     | number | 商品の価格 |
|imageUrl   | string | 商品の画像 |

## リクエストボディ
### items(1件分の構造)
| フィールド名 | 型     | 必須 | 説明 |
|--------------|--------|------|----------------|
|productId     | number | 必須  | 商品のID |
|quantity      | number | 必須  | 注文する数量 |

### customer(購入者情報)
| フィールド名 | 型      | 必須 | 説明 |
|-------------|------------|--------|------------|
| name        | string | 必須 | フルネーム |
| email       | string | 必須 | メールアドレス |
| address     | string | 必須 | 住所 |

## レスポンス
###　200 OK
注文処理が正常に完了した場合、注文情報を返す。

| フィールド名 | 型      | 説明 |
|------------|-----------|---------------------|
| orderNumber   | string | 注文番号 |
| totalAmount   | number | 合計金額 |
| status        | string | 注文処理のステータス |


### POST /api/admin/login
#### 基本情報
- エンドポイント: `/api/admin/login`
- メソッド: POST
- 概要; 管理者がログインするためのAPI

## リクエスト
###　リクエストボディ(JSON)
| フィールド名 | 型        | 必須 | 説明 |
|--------------|-----------|-------|---------------|
| username    | string | 必須 | 管理者の名前 |
| password    | string | 必須 | ログイン時に使用するパスワード |

## レスポンス
###　200 OK
ログインに成功した場合、認証用のトークンを返す。

| フィールド名 | 型     | 説明 |
|------------|-----------|----------------------------------|
| token      | string    | ログイン状態を証明するためのJWTトークン |

####　バリデーション
- username
 - 必須: true
 - 文字数: 8~20文字
 - 備考: 英数字・記号を許可。ただし記号のみの入力は不可

- password
 - 必須: ture
 - 文字数: 8~100文字
 - 備考: 
  - 英数字混在が望ましい
  - 空白のみは禁止
  - 記号は使用可(任意)

#### エラー

### 400 Bad Request
バリデーションエラーや必須項目の欠落など、リクエスト形式が不正な場合。

- 発生例
 - username が空
 - password が空
 - 文字数が不足している　など

- レスポンス例
　```json
{
    "message": "validation error",
    "errors": {
        "username": ["username is required"]
    }
}
```

### 401 Unauthorized
username または password が正しくない場合に返す。

- 発生例
 - username と password は送られてきたが組み合わせが間違っている

- レスポンス例
 ```json
 {
    "message": "invalid credentials"
 }
```

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
    {"message": "logged out"}
}
```
#### エラー
##### 401 Unauthorized
```json
{
    "message": "unauthorized"
}
```

##### 500 Internal Server Error
```json
{
    "message": "internal server erroe"
}
```
