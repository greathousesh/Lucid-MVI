#!/bin/bash

# 版本管理脚本 - 创建标签并更新版本信息
# Usage: ./release_tag.sh <new_version>
# Example: ./release_tag.sh 0.0.4

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印彩色信息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查参数
if [ $# -eq 0 ]; then
    print_error "请提供新版本号"
    echo "用法: $0 <新版本号>"
    echo "示例: $0 0.0.4"
    exit 1
fi

NEW_VERSION=$1

# 验证版本号格式 (支持 x.y.z 格式)
if ! [[ $NEW_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    print_error "版本号格式无效。请使用 x.y.z 格式 (例如: 0.0.4)"
    exit 1
fi

print_info "准备发布版本: $NEW_VERSION"

# 检查是否在git仓库中
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_error "当前目录不是git仓库"
    exit 1
fi

# 检查工作目录是否干净
if ! git diff-index --quiet HEAD --; then
    print_warning "工作目录有未提交的更改"
    read -p "是否继续? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "操作已取消"
        exit 1
    fi
fi

# 检查标签是否已存在
if git rev-parse --verify --quiet "v$NEW_VERSION" > /dev/null; then
    print_error "标签 v$NEW_VERSION 已存在"
    exit 1
fi

# 获取当前版本 (从mvi/build.gradle.kts)
CURRENT_VERSION=$(grep -oP 'version = "\K[^"]+' mvi/build.gradle.kts)
print_info "当前版本: $CURRENT_VERSION"

# 文件路径
BUILD_GRADLE_FILE="mvi/build.gradle.kts"
README_FILE="README.md"

# 检查文件是否存在
if [ ! -f "$BUILD_GRADLE_FILE" ]; then
    print_error "找不到文件: $BUILD_GRADLE_FILE"
    exit 1
fi

if [ ! -f "$README_FILE" ]; then
    print_error "找不到文件: $README_FILE"
    exit 1
fi

print_info "开始更新版本信息..."

# 备份原文件
cp "$BUILD_GRADLE_FILE" "$BUILD_GRADLE_FILE.bak"
cp "$README_FILE" "$README_FILE.bak"

print_info "已创建备份文件"

# 更新 build.gradle.kts 中的版本
if sed -i.tmp "s/version = \"$CURRENT_VERSION\"/version = \"$NEW_VERSION\"/g" "$BUILD_GRADLE_FILE"; then
    rm -f "$BUILD_GRADLE_FILE.tmp"
    print_success "已更新 $BUILD_GRADLE_FILE"
else
    print_error "更新 $BUILD_GRADLE_FILE 失败"
    exit 1
fi

# 更新 README.md 中的版本 (两处: 中文和英文部分)
if sed -i.tmp "s/Lucid-MVI:$CURRENT_VERSION/Lucid-MVI:$NEW_VERSION/g" "$README_FILE"; then
    rm -f "$README_FILE.tmp"
    print_success "已更新 $README_FILE"
else
    print_error "更新 $README_FILE 失败"
    exit 1
fi

# 显示更改的内容
print_info "版本更新摘要:"
echo "  版本: $CURRENT_VERSION → $NEW_VERSION"
echo "  更新的文件:"
echo "    - $BUILD_GRADLE_FILE"
echo "    - $README_FILE"

# 询问是否继续
echo
read -p "是否继续创建标签和提交更改? (y/N): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_warning "操作已取消，正在恢复文件..."
    mv "$BUILD_GRADLE_FILE.bak" "$BUILD_GRADLE_FILE"
    mv "$README_FILE.bak" "$README_FILE"
    print_info "文件已恢复"
    exit 1
fi

# 删除备份文件
rm -f "$BUILD_GRADLE_FILE.bak" "$README_FILE.bak"

# 提交更改
print_info "添加文件到git..."
git add "$BUILD_GRADLE_FILE" "$README_FILE"

print_info "提交更改..."
COMMIT_MESSAGE="chore: bump version to $NEW_VERSION

- Update version in mvi/build.gradle.kts
- Update version references in README.md"

git commit -m "$COMMIT_MESSAGE"

# 创建标签
print_info "创建标签 v$NEW_VERSION..."
git tag -a "v$NEW_VERSION" -m "Release version $NEW_VERSION"

print_success "版本发布完成!"
echo
print_info "后续步骤:"
echo "  1. 推送提交: git push origin main"
echo "  2. 推送标签: git push origin v$NEW_VERSION"
echo "  3. 或者一次性推送: git push origin main --tags"

# 询问是否立即推送
echo
read -p "是否立即推送到远程仓库? (y/N): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_info "推送到远程仓库..."
    
    # 检查远程分支
    CURRENT_BRANCH=$(git branch --show-current)
    print_info "当前分支: $CURRENT_BRANCH"
    
    # 推送提交
    if git push origin "$CURRENT_BRANCH"; then
        print_success "提交推送成功"
    else
        print_error "提交推送失败"
        exit 1
    fi
    
    # 推送标签
    if git push origin "v$NEW_VERSION"; then
        print_success "标签推送成功"
    else
        print_error "标签推送失败"
        exit 1
    fi
    
    print_success "版本 $NEW_VERSION 已成功发布到远程仓库!"
else
    print_info "记得手动推送提交和标签到远程仓库"
fi

print_success "脚本执行完成!"
