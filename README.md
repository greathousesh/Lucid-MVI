# MVI Library

[![GitHub release](https://img.shields.io/github/v/release/greathousesh/Simple-MVI)](https://github.com/greathousesh/Simple-MVI/releases)
[![JitPack](https://jitpack.io/v/greathousesh/Simple-MVI.svg)](https://jitpack.io/#greathousesh/Simple-MVI)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Build Status](https://github.com/greathousesh/Simple-MVI/workflows/Publish%20AAR%20to%20GitHub%20Packages/badge.svg)](https://github.com/greathousesh/Simple-MVI/actions)

**语言**: **中文** | [English](README_EN.md)

一个简单而优雅的Android MVI (Model-View-Intent) 架构实现库。

## ✨ 特性

- 🏗️ **基于Kotlin协程** - 响应式架构，完全异步处理
- 🔄 **清晰的单向数据流** - 可预测的状态管理
- 🎯 **类型安全** - 编译时类型检查，减少运行时错误
- 🧪 **易于测试** - 完整的单元测试覆盖
- 📦 **轻量级** - 无额外依赖，仅16KB的AAR包
- 🚀 **生产就绪** - 包含生命周期感知和线程安全特性

## 📦 安装

### 通过JitPack

JitPack提供了最简单的安装方式，无需任何认证配置：

#### 1. 添加JitPack仓库

在你的项目根目录的 `build.gradle.kts` 文件中添加JitPack仓库：

```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

#### 2. 添加依赖

在你的模块的 `build.gradle.kts` 文件中添加依赖：

```kotlin
dependencies {
    implementation("com.greathouse:mvi:0.0.1")
}
```

### 系统要求

- **最小Android SDK**: API 24 (Android 7.0)
- **目标SDK**: API 36
- **Kotlin版本**: 2.0.21+

## 🚀 快速开始

### 1. 定义你的MVI组件

```kotlin
// 状态
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
)

// 动作
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
}

// 副作用
sealed class CounterEffect {
    object SaveCount : CounterEffect()
}

// 事件
sealed class CounterEvent {
    object CountSaved : CounterEvent()
    data class ShowError(val message: String) : CounterEvent()
}
```

### 2. 实现Reducer和EffectHandler

```kotlin
class CounterReducer : StateReducer<CounterState, CounterAction> {
    override fun reduce(state: CounterState, action: CounterAction): CounterState {
        return when (action) {
            is CounterAction.Increment -> state.copy(count = state.count + 1)
            is CounterAction.Decrement -> state.copy(count = state.count - 1)
            is CounterAction.Reset -> state.copy(count = 0)
        }
    }
}

class CounterEffectHandler : EffectHandler<CounterState, CounterAction, CounterEffect, CounterEvent> {
    override suspend fun handle(
        state: CounterState,
        effect: CounterEffect,
        dispatch: suspend (CounterAction) -> Unit,
        emit: suspend (CounterEvent) -> Unit
    ) {
        when (effect) {
            is CounterEffect.SaveCount -> {
                try {
                    // 保存计数逻辑
                    emit(CounterEvent.CountSaved)
                } catch (e: Exception) {
                    emit(CounterEvent.ShowError(e.message ?: "Unknown error"))
                }
            }
        }
    }
}
```

### 3. 创建ViewModel

```kotlin
class CounterViewModel : BaseMVIViewModel<CounterState, CounterAction, CounterEffect, CounterEvent>(
    reducer = CounterReducer(),
    effectHandler = CounterEffectHandler()
) {
    override fun initialState(): CounterState = CounterState()
    
    fun increment() = sendAction(CounterAction.Increment)
    fun decrement() = sendAction(CounterAction.Decrement)
    fun reset() = sendAction(CounterAction.Reset)
    fun saveCount() = sendEffect(CounterEffect.SaveCount)
}
```

### 4. 在Activity/Fragment中使用

```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModel: CounterViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 观察状态变化
        lifecycleScope.launch {
            viewModel.stateFlow.collect { state ->
                updateUI(state)
            }
        }
        
        // 观察事件
        lifecycleScope.launch {
            viewModel.eventFlow.collect { event ->
                handleEvent(event)
            }
        }
        
        // 发送动作
        incrementButton.setOnClickListener {
            viewModel.increment()
        }
    }
    
    private fun updateUI(state: CounterState) {
        countText.text = state.count.toString()
        progressBar.isVisible = state.isLoading
    }
    
    private fun handleEvent(event: CounterEvent) {
        when (event) {
            is CounterEvent.CountSaved -> {
                Toast.makeText(this, "Count saved!", Toast.LENGTH_SHORT).show()
            }
            is CounterEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
```

## 🏗️ 架构概览

```
┌─────────────┐    Action    ┌─────────────┐    New State    ┌─────────────┐
│     UI      │─────────────▶│  ViewModel  │────────────────▶│    State    │
│             │              │             │                 │             │
│             │◀─────────────│             │◀────────────────│             │
└─────────────┘    Event     └─────────────┘                 └─────────────┘
                                     │                               ▲
                                     │ Effect                        │
                                     ▼                               │
                              ┌─────────────┐    Action              │
                              │ EffectHandler│───────────────────────┘
                              └─────────────┘
```

## 🧪 测试

该库包含完整的测试套件，确保代码质量和稳定性：

```bash
# 运行单元测试
./gradlew :mvi:test

# 运行Android集成测试
./gradlew :mvi:connectedAndroidTest
```

## 📚 文档

- 🚀 [GitHub Actions](https://github.com/greathousesh/Simple-MVI/actions) - 自动化构建和发布
- 📦 [JitPack](https://jitpack.io/#greathousesh/Simple-MVI) - 包分发平台

## 🤝 贡献

我们欢迎社区贡献！请遵循以下步骤：

1. Fork 这个仓库
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

### 开发环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK API 24+

## 📈 路线图

- [ ] 支持Compose集成
- [ ] 添加调试工具和日志
- [ ] Kotlin Multiplatform支持

## ❓ 常见问题

### Q: 如何处理复杂的异步操作？
A: 使用EffectHandler处理所有副作用，保持Reducer的纯函数特性。

### Q: 如何与现有的ViewModel集成？
A: BaseMVIViewModel继承自ViewModel，可以直接替换现有的ViewModel。

### Q: 性能如何？
A: 库非常轻量级（仅16KB），基于Kotlin协程，性能优异。

## 📊 库信息

- 📦 **包大小**: 16KB AAR
- 📱 **支持平台**: Android API 24+
- 🔧 **依赖**: 仅Android标准库
- ⭐ **GitHub Stars**: [给我们一个Star!](https://github.com/greathousesh/Simple-MVI)

## 🔗 相关链接

- [MVI架构模式介绍](https://hannesdorfmann.com/android/model-view-intent/)
- [Kotlin协程官方文档](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android架构组件](https://developer.android.com/topic/libraries/architecture)

## 📄 许可证

```
Copyright 2024 Fang Wei

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```