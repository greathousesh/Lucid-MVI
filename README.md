# <img src="m-icon.svg" alt="MVI" width="32" height="32" align="center"> MVI Library / MVI

[![GitHub release](https://img.shields.io/github/v/release/greathousesh/Simple-MVI)](https://github.com/greathousesh/Simple-MVI/releases)
[![JitPack](https://jitpack.io/v/greathousesh/Simple-MVI.svg)](https://jitpack.io/#greathousesh/Simple-MVI)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Build Status](https://github.com/greathousesh/Simple-MVI/workflows/Publish%20AAR%20to%20GitHub%20Packages/badge.svg)](https://github.com/greathousesh/Simple-MVI/actions)

<details>
<summary><strong>🇨🇳 中文文档</strong></summary>

## 简介

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
    implementation("com.github.greathousesh:Simple-MVI:0.0.3")
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

## 📈 路线图

- [ ] 支持Compose集成
- [ ] 添加调试工具和日志
- [ ] Kotlin Multiplatform支持

## ❓ 常见问题

**Q: 如何处理复杂的异步操作？**  
A: 使用EffectHandler处理所有副作用，保持Reducer的纯函数特性。

**Q: 如何与现有的ViewModel集成？**  
A: BaseMVIViewModel继承自ViewModel，可以直接替换现有的ViewModel。

**Q: 性能如何？**  
A: 库非常轻量级（仅16KB），基于Kotlin协程，性能优异。

</details>

<details open>
<summary><strong>🇺🇸 English Documentation</strong></summary>

## Introduction

A simple and elegant MVI (Model-View-Intent) architecture implementation library for Android.

## ✨ Features

- 🏗️ **Kotlin Coroutines Based** - Reactive architecture with fully asynchronous processing
- 🔄 **Clear Unidirectional Data Flow** - Predictable state management
- 🎯 **Type Safe** - Compile-time type checking reduces runtime errors
- 🧪 **Easy to Test** - Complete unit test coverage
- 📦 **Lightweight** - No additional dependencies, only 16KB AAR package
- 🚀 **Production Ready** - Includes lifecycle awareness and thread safety features

## 📦 Installation

### Via JitPack

JitPack provides the simplest installation method with no authentication configuration required:

#### 1. Add JitPack Repository

Add the JitPack repository to your project's root `build.gradle.kts` file:

```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

#### 2. Add Dependency

Add the dependency to your module's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.github.greathousesh:Simple-MVI:0.0.3")
}
```

### System Requirements

- **Minimum Android SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36
- **Kotlin Version**: 2.0.21+

## 🚀 Quick Start

### 1. Define Your MVI Components

```kotlin
// State
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
)

// Actions
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
}

// Effects
sealed class CounterEffect {
    object SaveCount : CounterEffect()
}

// Events
sealed class CounterEvent {
    object CountSaved : CounterEvent()
    data class ShowError(val message: String) : CounterEvent()
}
```

### 2. Implement Reducer and EffectHandler

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
                    // Save count logic
                    emit(CounterEvent.CountSaved)
                } catch (e: Exception) {
                    emit(CounterEvent.ShowError(e.message ?: "Unknown error"))
                }
            }
        }
    }
}
```

### 3. Create ViewModel

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

### 4. Use in Activity/Fragment

```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModel: CounterViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Observe state changes
        lifecycleScope.launch {
            viewModel.stateFlow.collect { state ->
                updateUI(state)
            }
        }
        
        // Observe events
        lifecycleScope.launch {
            viewModel.eventFlow.collect { event ->
                handleEvent(event)
            }
        }
        
        // Send actions
        incrementButton.setOnClickListener {
            viewModel.increment()
        }
    }
}
```

## 🏗️ Architecture Overview

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

## 📈 Roadmap

- [ ] Compose integration support
- [ ] Debug tools and logging
- [ ] Kotlin Multiplatform support

## ❓ FAQ

**Q: How to handle complex asynchronous operations?**  
A: Use EffectHandler to handle all side effects, keeping the Reducer as a pure function.

**Q: How to integrate with existing ViewModels?**  
A: BaseMVIViewModel inherits from ViewModel and can directly replace existing ViewModels.

**Q: How is the performance?**  
A: The library is very lightweight (only 16KB), based on Kotlin coroutines, with excellent performance.

</details>

---

## 🧪 Testing

The library includes a complete test suite to ensure code quality and stability:

```bash
# Run unit tests
./gradlew :mvi:test

# Run Android integration tests
./gradlew :mvi:connectedAndroidTest
```

## 📚 Documentation

- 🚀 [GitHub Actions](https://github.com/greathousesh/Simple-MVI/actions) - Automated build and publishing
- 📦 [JitPack](https://jitpack.io/#greathousesh/Simple-MVI) - Package distribution platform

## 🤝 Contributing

We welcome community contributions! Please follow these steps:

1. Fork this repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Environment Requirements
- Android Studio Arctic Fox or higher
- JDK 11 or higher
- Android SDK API 24+

## 📊 Library Information

- 📦 **Package Size**: 16KB AAR
- 📱 **Supported Platforms**: Android API 24+
- 🔧 **Dependencies**: Android standard library only
- ⭐ **GitHub Stars**: [Give us a Star!](https://github.com/greathousesh/Simple-MVI)

## 🔗 Related Links

- [MVI Architecture Pattern Introduction](https://hannesdorfmann.com/android/model-view-intent/)
- [Kotlin Coroutines Official Documentation](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture)

## 📄 License

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

</details>

---

## 🧪 Testing

The library includes a complete test suite to ensure code quality and stability:

```bash
# Run unit tests
./gradlew :mvi:test

# Run Android integration tests
./gradlew :mvi:connectedAndroidTest
```

## 📚 Documentation

- 🚀 [GitHub Actions](https://github.com/greathousesh/Simple-MVI/actions) - Automated build and publishing
- 📦 [JitPack](https://jitpack.io/#greathousesh/Simple-MVI) - Package distribution platform

## 🤝 Contributing

We welcome community contributions! Please follow these steps:

1. Fork this repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Environment Requirements
- Android Studio Arctic Fox or higher
- JDK 11 or higher
- Android SDK API 24+

## 📊 Library Information

- 📦 **Package Size**: 16KB AAR
- 📱 **Supported Platforms**: Android API 24+
- 🔧 **Dependencies**: Android standard library only
- ⭐ **GitHub Stars**: [Give us a Star!](https://github.com/greathousesh/Simple-MVI)

## 🔗 Related Links

- [MVI Architecture Pattern Introduction](https://hannesdorfmann.com/android/model-view-intent/)
- [Kotlin Coroutines Official Documentation](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture)

## 📄 License

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
