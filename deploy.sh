#!/usr/bin/env bash
#
# deploy.sh — 手元のPCからビルドして ConoHa VPS へデプロイ（別ポート公開・共存版）
# 構成: Spring Boot (Maven) + React (Vite)
#
# 使い方（手元のPCで実行）:
#   bash deploy.sh
#
# 前提:
#   - 先に VPS 上で server-setup.sh を1回実行済みであること
#   - `ssh ${SSH_USER}@${SERVER_IP}` で入れる状態であること
#
set -euo pipefail

# ===== 設定（必ず自分の環境に合わせて変更）=============================
SERVER_IP="160.251.234.95"        # ← VPS のグローバルIP
SSH_USER="admin"                  # ← SSH ログインユーザー
SSH_PORT="22"                     # ← SSH ポート

APP_NAME="myapp"                  # server-setup.sh と揃える
APP_DIR="/opt/${APP_NAME}"        # JAR の配置先
WEB_ROOT="/var/www/${APP_NAME}"   # React の配置先
PUBLIC_PORT="8090"                # server-setup.sh の PUBLIC_PORT と揃える（表示用）

FRONTEND_DIR="./frontend"         # React(Vite) プロジェクトのパス
BACKEND_DIR="/Users/sugawara/Desktop/ECsite.java/backend/demo"           # Spring Boot(Maven) プロジェクトのパス
# =======================================================================

SSH_CMD="ssh -p ${SSH_PORT} ${SSH_USER}@${SERVER_IP}"
if [[ "${SSH_USER}" == "root" ]]; then SUDO=""; else SUDO="sudo "; fi

echo "==> [1/5] React(Vite) を本番ビルドします"
pushd "${FRONTEND_DIR}" >/dev/null
if [[ -f package-lock.json ]]; then
  npm ci
else
  npm install
fi
npm run build                      # Vite の出力先は dist/
popd >/dev/null
if [[ ! -d "${FRONTEND_DIR}/dist" ]]; then
  echo "エラー: ${FRONTEND_DIR}/dist が見つかりません。Vite のビルド出力先を確認してください。" >&2
  exit 1
fi

echo "==> [2/5] Spring Boot(Maven) を JAR にビルドします"
pushd "${BACKEND_DIR}" >/dev/null
if [[ -x ./mvnw ]]; then
  ./mvnw clean package -DskipTests
else
  mvn clean package -DskipTests
fi
popd >/dev/null

# Spring Boot の実行可能JARを特定（*.jar.original は除外）
JAR_FILE="$(ls -1 "${BACKEND_DIR}"/target/*.jar 2>/dev/null | grep -v '\.original$' | head -n1 || true)"
if [[ -z "${JAR_FILE}" ]]; then
  echo "エラー: ${BACKEND_DIR}/target に JAR が見つかりません。ビルドが成功したか確認してください。" >&2
  exit 1
fi
echo "    使用する JAR: ${JAR_FILE}"

echo "==> [3/5] JAR をサーバーへ転送します"
scp -P "${SSH_PORT}" "${JAR_FILE}" "${SSH_USER}@${SERVER_IP}:${APP_DIR}/app.jar"

echo "==> [4/5] React のビルド成果物を転送します"
rsync -az --delete -e "ssh -p ${SSH_PORT}" \
  "${FRONTEND_DIR}/dist/" "${SSH_USER}@${SERVER_IP}:${WEB_ROOT}/"

echo "==> [5/5] 読み取り権限を整えて Java サービスを再起動します"
# 静的ファイル更新だけなら Nginx 再起動は不要（共有Nginxには触れません）。
# chmod は所有者(${SSH_USER})が行うので sudo 不要。再起動のみ sudo(NOPASSWD設定済み)。
${SSH_CMD} "chmod 644 ${APP_DIR}/app.jar && ${SUDO}systemctl restart ${APP_NAME}"

echo ""
echo "==> デプロイ完了！"
echo "    公開URL: http://${SERVER_IP}:${PUBLIC_PORT}"
echo "    サーバー内からの確認:"
echo "      ${SSH_CMD} 'curl -s -o /dev/null -w \"%{http_code}\\n\" http://localhost:${PUBLIC_PORT}'"
