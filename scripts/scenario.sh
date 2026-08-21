#!/usr/bin/env bash
# wallet 钱包工程联调场景脚本
# 前置：MySQL(wallet库已建表) + Redis(6379) + 应用已启动(默认 8080)
# 场景：充值→发积分→领券→设密码→混合拆分单→校验→提交→自动回调→部分退款→全额退款
set -euo pipefail

B="${BASE_URL:-http://localhost:8080}"
UID1="${X_UID:-10001}"
JQ="python3 -c 'import sys,json;d=json.load(sys.stdin);print(d${1:-\"[\\\"code\\\"]\"})'"

say() { printf '\n\033[1;34m== %s\033[0m\n' "$*"; }

# 取响应某字段的辅助函数: jget <json> <path>；jcount 取 JSON 数组长度
jget() { echo "$1" | python3 -c "import sys,json;print(json.load(sys.stdin)$2)"; }
jcount() { echo "$1" | python3 -c "import sys,json;print(len(json.load(sys.stdin)$2))"; }
jcode() { jget "$1" "['code']"; }

expect_ok() { # expect_ok <响应> <说明>
  local code; code=$(jget "$1" "['code']")
  if [ "$code" != "0" ]; then echo "   ✗ [$2] code=$code msg=$(jget "$1" "['message']")"; exit 1; fi
  echo "   ✓ $2"
}

say "1. 充值 100 元 + 发 500 积分 + 领券 + 设支付密码"
R=$(curl -s -X POST $B/api/asset/recharge -H "X-Uid: $UID1" -H "Content-Type: application/json" -d '{"amount":10000}')
expect_ok "$R" "充值 10000 分 → 余额 $(jget "$R" "['data']['money']")"
R=$(curl -s -X POST $B/api/asset/point/add -H "X-Uid: $UID1" -H "Content-Type: application/json" -d '{"count":500}')
expect_ok "$R" "发 500 积分 → 积分 $(jget "$R" "['data']['point']")"
R=$(curl -s -X POST $B/api/asset/coupon/take -H "X-Uid: $UID1" -H "Content-Type: application/json" -d '{"couponId":1}')
expect_ok "$R" "领券"
UCID=$(jget "$R" "['data']['id']")
R=$(curl -s -X POST $B/api/password/set -H "X-Uid: $UID1" -H "Content-Type: application/json" -d '{"password":"123456"}')
expect_ok "$R" "设置支付密码"

say "2. 创建混合拆分支付单: 券10 + 积分5 + 余额35 + 三方50 = 100 元"
R=$(curl -s -X POST $B/api/pay/create -H "X-Uid: $UID1" -H "Content-Type: application/json" -d "{
  \"bizOrderNo\":\"BIZ-$(date +%s)\",\"totalAmount\":10000,\"currency\":\"TWD\",
  \"parts\":[
    {\"payType\":\"COUPON\",\"amount\":1000,\"userCouponId\":$UCID},
    {\"payType\":\"POINT\",\"amount\":500,\"pointCount\":500},
    {\"payType\":\"MONEY\",\"amount\":3500},
    {\"payType\":\"CHANNEL\",\"amount\":5000,\"channelCode\":\"MOCK\"}
  ]}")
expect_ok "$R" "创建支付单"
ORDER=$(jget "$R" "['data']['orderNo']")
echo "   orderNo=$ORDER"

say "3. 校验支付密码签发票据 + 提交支付"
R=$(curl -s -X POST $B/api/password/verify -H "X-Uid: $UID1" -H "Content-Type: application/json" -d "{\"password\":\"123456\",\"orderNo\":\"$ORDER\",\"amount\":10000}")
expect_ok "$R" "校验密码"
TICKET=$(jget "$R" "['data']['ticket']")
R=$(curl -s -X POST $B/api/pay/submit -H "X-Uid: $UID1" -H "Content-Type: application/json" -d "{\"orderNo\":\"$ORDER\",\"ticket\":\"$TICKET\"}")
expect_ok "$R" "提交支付 → 状态 $(jget "$R" "['data']['state']")"

say "4. 等 mock 自动回调(5s) → 订单应为 SUCCESS"
sleep 6
R=$(curl -s $B/api/pay/order/$ORDER -H "X-Uid: $UID1")
STATE=$(jget "$R" "['data']['order']['state']")
echo "   ✓ 订单状态 = $STATE"
[ "$STATE" = "SUCCESS" ] || { echo "   ✗ 期望 SUCCESS"; exit 1; }
R=$(curl -s $B/api/asset/summary -H "X-Uid: $UID1")
echo "   余额=$(jget "$R" "['data']['money']") 积分=$(jget "$R" "['data']['point']") 可用券=$(jcount "$R" "['data']['usableCoupons']")"
[ "$(jget "$R" "['data']['money']")" = "6500" ] && echo "   ✓ 余额扣减正确" || { echo "   ✗ 余额应为 6500"; exit 1; }

say "5. 部分退款 30 元（应先退三方 30）"
R=$(curl -s -X POST $B/api/refund/create -H "X-Uid: $UID1" -H "Content-Type: application/json" -d "{\"orderNo\":\"$ORDER\",\"amount\":3000,\"reason\":\"部分退款\"}")
expect_ok "$R" "部分退款 → 状态 $(jget "$R" "['data']['state']")"
REFUND=$(jget "$R" "['data']['refundNo']")
R=$(curl -s $B/api/refund/$REFUND -H "X-Uid: $UID1")
echo "   退款单=$(jget "$R" "['data']['refundOrder']['state']") 分段: $(jget "$R" "['data']['parts']")"

say "6. 全额退款（券应返还，金额=剩余可退额）"
REST=$(curl -s $B/api/pay/order/$ORDER -H "X-Uid: $UID1")
REMAIN=$(jget "$REST" "['data']['order']['refundableAmount']")
echo "   剩余可退额 = $REMAIN"
R=$(curl -s -X POST $B/api/refund/create -H "X-Uid: $UID1" -H "Content-Type: application/json" -d "{\"orderNo\":\"$ORDER\",\"amount\":$REMAIN,\"reason\":\"全额退款\"}")
expect_ok "$R" "全额退款 → 状态 $(jget "$R" "['data']['state']")"
R=$(curl -s $B/api/asset/summary -H "X-Uid: $UID1")
echo "   最终余额=$(jget "$R" "['data']['money']") 积分=$(jget "$R" "['data']['point']") 可用券=$(jcount "$R" "['data']['usableCoupons']")"

say "全部场景通过 ✅"
