#!/bin/bash

# 生成签名密钥库的脚本
# 运行此脚本前请确保已安装 JDK

KEYSTORE_FILE="periodtracker.keystore"
KEY_ALIAS="periodtracker"
KEY_PASSWORD="periodtracker2024"
STORE_PASSWORD="periodtracker2024"
VALIDITY_DAYS=10000

echo "正在生成签名密钥库..."
echo "文件名: $KEYSTORE_FILE"
echo "别名: $KEY_ALIAS"
echo "有效期: $VALIDITY_DAYS 天"

keytool -genkey -v \
    -keystore $KEYSTORE_FILE \
    -alias $KEY_ALIAS \
    -keyalg RSA \
    -keysize 2048 \
    -validity $VALIDITY_DAYS \
    -storepass $STORE_PASSWORD \
    -keypass $KEY_PASSWORD \
    -dname "CN=PeriodTracker, OU=Development, O=PeriodTracker, L=City, ST=State, C=CN"

if [ $? -eq 0 ]; then
    echo "✅ 密钥库生成成功!"
    echo ""
    echo "📋 签名信息:"
    echo "   - 文件名: $KEYSTORE_FILE"
    echo "   - 别名: $KEY_ALIAS"
    echo "   - 密钥库密码: $STORE_PASSWORD"
    echo "   - 密钥密码: $KEY_PASSWORD"
    echo ""
    echo "⚠️  请妥善保管此密钥库文件和密码!"
    echo "   密钥库位置: $(pwd)/$KEYSTORE_FILE"
else
    echo "❌ 密钥库生成失败"
    exit 1
fi
