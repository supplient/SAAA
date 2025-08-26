# 编译指南

## 环境要求

### 必需组件
1. **Android Studio** (推荐最新版本)
2. **Android SDK** (API 33+)
3. **Java 11** 或更高版本
4. **Gradle 8.13**

### 环境变量设置
```bash
export ANDROID_HOME=/path/to/your/android/sdk
export JAVA_HOME=/path/to/your/java11
```

## 编译步骤

### 1. 克隆项目
```bash
git clone <repository-url>
cd StrategicAssetAllocationAssistant
```

### 2. 修复依赖问题
由于我们移除了Hilt依赖，需要选择以下方案之一：

#### 方案A: 重新添加Hilt依赖
在 `build.gradle.kts` 中添加：
```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.48" apply false
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
}
```

#### 方案B: 移除所有Hilt代码
需要修改以下文件：
- `MainActivity.kt`
- `MainApplication.kt`
- `PortfolioViewModel.kt`
- `TradingOpportunityViewModel.kt`
- 其他包含Hilt注解的ViewModel

### 3. 同步项目
```bash
./gradlew clean
./gradlew build
```

### 4. 编译APK
```bash
./gradlew assembleDebug
```

## 常见问题

### 1. SDK路径问题
```
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable
```
**解决方案**: 设置正确的ANDROID_HOME环境变量

### 2. Hilt依赖问题
```
Plugin [id: 'com.google.dagger.hilt.android'] was not found
```
**解决方案**: 按照上述方案A或B处理

### 3. Compose编译器问题
```
The Compose compiler plugin is now a part of Kotlin
```
**解决方案**: 使用正确的Compose编译器插件版本

## 测试建议

### 1. 功能测试
- 测试可用现金编辑功能
- 验证资产刷新失败状态显示
- 检查交易机会界面改进

### 2. UI测试
- 验证不同屏幕尺寸下的显示效果
- 测试深色/浅色主题
- 检查无障碍功能

### 3. 性能测试
- 监控内存使用
- 检查UI响应性
- 验证数据刷新性能

## 部署

### 开发版本
```bash
./gradlew assembleDebug
```
输出: `app/build/outputs/apk/debug/app-debug.apk`

### 发布版本
```bash
./gradlew assembleRelease
```
输出: `app/build/outputs/apk/release/app-release.apk`

## 注意事项

1. **首次编译**可能需要较长时间下载依赖
2. **Hilt代码**需要完整处理，避免部分移除导致的编译错误
3. **版本兼容性**确保所有依赖版本兼容
4. **内存设置**对于大型项目，可能需要增加Gradle内存设置

## 联系支持

如果遇到编译问题，请：
1. 检查环境配置
2. 查看Gradle错误日志
3. 确认依赖版本兼容性
4. 参考Android官方文档