# MVI Library

[![GitHub release](https://img.shields.io/github/v/release/greathousesh/Simple-MVI)](https://github.com/greathousesh/Simple-MVI/releases)
[![JitPack](https://jitpack.io/v/greathousesh/Simple-MVI.svg)](https://jitpack.io/#greathousesh/Simple-MVI)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Build Status](https://github.com/greathousesh/Simple-MVI/workflows/Publish%20AAR%20to%20GitHub%20Packages/badge.svg)](https://github.com/greathousesh/Simple-MVI/actions)

**Language**: [中文](README.md) | **English**

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
    implementation("com.greathouse:mvi:0.0.1")
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

## 📈 Roadmap

- [ ] Compose integration support
- [ ] Debug tools and logging
- [ ] Kotlin Multiplatform support

## ❓ FAQ

### Q: How to handle complex asynchronous operations?
A: Use EffectHandler to handle all side effects, keeping the Reducer as a pure function.

### Q: How to integrate with existing ViewModels?
A: BaseMVIViewModel inherits from ViewModel and can directly replace existing ViewModels.

### Q: How is the performance?
A: The library is very lightweight (only 16KB), based on Kotlin coroutines, with excellent performance.

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
