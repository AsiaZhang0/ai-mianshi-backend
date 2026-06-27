#!/bin/bash
# ============================================================
# Sentinel Push 模式 - Nacos 规则初始化脚本
# ============================================================
# 用法: bash sentinel-nacos-init.sh
# 功能: 将 Sentinel 默认规则推送到 Nacos Config，供 Push 模式使用
# 前置: Nacos Server 已启动 (localhost:8848)
# ============================================================

NACOS_ADDR="localhost:8848"
NACOS_USER="nacos"
NACOS_PASS="123456"
GROUP="SENTINEL_GROUP"

# 登录 Nacos 获取 token
echo ">>> 登录 Nacos..."
LOGIN_RESP=$(curl -s -X POST "http://${NACOS_ADDR}/nacos/v1/auth/login" \
  -d "username=${NACOS_USER}&password=${NACOS_PASS}")
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "ERROR: Nacos 登录失败，请检查用户名密码"
  echo "Response: $LOGIN_RESP"
  exit 1
fi
echo "登录成功，Token: ${TOKEN:0:20}..."

# ============================================================
# 1. 网关 API 分组定义
# ============================================================
API_DEFINITIONS='[
  {
    "apiName": "gateway-global",
    "predicateItems": [
      {
        "pattern": "/api/**",
        "matchStrategy": 0
      }
    ]
  }
]'

echo ">>> 推送网关 API 分组定义..."
curl -s -X POST "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
  -d "dataId=sentinel-gateway-api-definitions" \
  -d "group=${GROUP}" \
  -d "content=${API_DEFINITIONS}" \
  -d "type=json" \
  -d "accessToken=${TOKEN}"
echo ""

# ============================================================
# 2. 网关限流规则
# ============================================================
FLOW_RULES='[
  {
    "resource": "gateway-global",
    "resourceMode": 0,
    "count": 1826.0,
    "grade": 1,
    "controlBehavior": 0
  },
  {
    "resource": "user-service",
    "resourceMode": 0,
    "count": 608.0,
    "grade": 1,
    "controlBehavior": 0
  },
  {
    "resource": "user-service",
    "resourceMode": 0,
    "count": 100.0,
    "grade": 1,
    "controlBehavior": 0,
    "paramItem": {
      "parseStrategy": 3,
      "fieldName": null,
      "pattern": null,
      "matchStrategy": null
    }
  },
  {
    "resource": "questions-service",
    "resourceMode": 0,
    "count": 608.0,
    "grade": 1,
    "controlBehavior": 0
  },
  {
    "resource": "questions-service",
    "resourceMode": 0,
    "count": 100.0,
    "grade": 1,
    "controlBehavior": 0,
    "paramItem": {
      "parseStrategy": 3,
      "fieldName": null,
      "pattern": null,
      "matchStrategy": null
    }
  },
  {
    "resource": "questionbank",
    "resourceMode": 0,
    "count": 608.0,
    "grade": 1,
    "controlBehavior": 0
  },
  {
    "resource": "questionbank",
    "resourceMode": 0,
    "count": 100.0,
    "grade": 1,
    "controlBehavior": 0,
    "paramItem": {
      "parseStrategy": 3,
      "fieldName": null,
      "pattern": null,
      "matchStrategy": null
    }
  }
]'

echo ">>> 推送网关限流规则..."
curl -s -X POST "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
  -d "dataId=sentinel-gateway-flow-rules" \
  -d "group=${GROUP}" \
  -d "content=${FLOW_RULES}" \
  -d "type=json" \
  -d "accessToken=${TOKEN}"
echo ""

# ============================================================
# 3. 熔断降级规则
# ============================================================
DEGRADE_RULES='[
  {
    "resource": "gateway-global",
    "grade": 1,
    "count": 0.3,
    "timeWindow": 10,
    "minRequestAmount": 50,
    "statIntervalMs": 10000,
    "slowRatioThreshold": 1.0
  }
]'

echo ">>> 推送熔断降级规则..."
curl -s -X POST "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
  -d "dataId=sentinel-degrade-rules" \
  -d "group=${GROUP}" \
  -d "content=${DEGRADE_RULES}" \
  -d "type=json" \
  -d "accessToken=${TOKEN}"
echo ""

echo ""
echo "==============================================="
echo "Sentinel 规则已成功推送到 Nacos!"
echo "Group:  ${GROUP}"
echo "Data ID 列表:"
echo "  1. sentinel-gateway-api-definitions"
echo "  2. sentinel-gateway-flow-rules"
echo "  3. sentinel-degrade-rules"
echo ""
echo "现在启动 Gateway 服务即可自动拉取 Nacos 中的规则"
echo "==============================================="
