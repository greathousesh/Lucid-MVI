# <img src="m-icon.svg" alt="MVI" width="32" height="32" align="center"> Lucid MVI

> A simple, elegant, and powerful MVI (Model-View-Intent) architecture framework for Android

[![GitHub release](https://img.shields.io/github/v/release/greathousesh/Lucid-MVI)](https://github.com/greathousesh/Lucid-MVI/releases)
[![JitPack](https://jitpack.io/v/greathousesh/Lucid-MVI.svg)](https://jitpack.io/#greathousesh/Lucid-MVI)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Coroutines](https://img.shields.io/badge/Coroutines-1.9.0-orange.svg?logo=kotlin)](https://kotlinlang.org/docs/coroutines-overview.html)
[![Android API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7.6-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Build Status](https://github.com/greathousesh/Lucid-MVI/workflows/Publish%20AAR%20to%20GitHub%20Packages/badge.svg)](https://github.com/greathousesh/Lucid-MVI/actions)

## 🚀 Why Lucid MVI?

Lucid MVI brings **predictable state management** and **unidirectional data flow** to Android development. Built on Kotlin Coroutines, it provides a reactive, type-safe, and testable architecture that scales from simple counters to complex applications.

```kotlin
// Define your state, actions, and events
data class CounterState(val count: Int = 0, val isLoading: Boolean = false)
sealed class CounterAction { object Increment : CounterAction() }
sealed class CounterEvent { object CountSaved : CounterEvent() }

// Create your ViewModel
class CounterViewModel : BaseMVIViewModel<CounterState, CounterAction, CounterEffect, CounterEvent>(
    reducer = CounterReducer(),
    effectHandler = CounterEffectHandler()
) {
    override fun initialState() = CounterState()
    fun increment() = sendAction(CounterAction.Increment)
}
```

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| 🏗️ **Kotlin Coroutines Based** | Reactive architecture with fully asynchronous processing |
| 🔄 **Unidirectional Data Flow** | Predictable state management, easy to debug |
| 🎯 **Type Safe** | Compile-time type checking reduces runtime errors |
| 🧪 **Easy to Test** | Pure functional reducers, predictable side effect handling |
| 📦 **Lightweight** | No additional dependencies, < 20KB |
| 🚀 **Production Ready** | Lifecycle awareness and thread safety built-in |
| 📱 **Compose Ready** | First-class Jetpack Compose integration |

## 📱 Demo Application

This repository includes a comprehensive demo app showcasing MVI in action:

| Demo | Tech Stack | Complexity | Learning Focus |
|------|------------|------------|----------------|
| 🏠 **HomeActivity** | Material 3 Navigation | ⭐ | App structure & navigation |
| 🧮 **CounterActivity** | Traditional Views | ⭐⭐ | MVI basics & state management |
| 📱 **CounterComposeActivity** | Jetpack Compose | ⭐⭐⭐ | Modern UI with MVI |
| ✅ **TodoActivity** | Complex State Logic | ⭐⭐⭐⭐ | Real-world MVI patterns |

### Screenshots
<p align="center">
  <img src="https://via.placeholder.com/200x400/4285F4/FFFFFF?text=Home" alt="Home" width="180"/>
  <img src="https://via.placeholder.com/200x400/FF6B35/FFFFFF?text=Counter" alt="Counter" width="180"/>
  <img src="https://via.placeholder.com/200x400/4CAF50/FFFFFF?text=Compose" alt="Compose" width="180"/>
  <img src="https://via.placeholder.com/200x400/9C27B0/FFFFFF?text=Todo" alt="Todo" width="180"/>
</p>

## 🛠️ Installation

### Method 1: JitPack (Recommended)

Add JitPack repository to your project's `build.gradle.kts`:

```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.greathousesh:Lucid-MVI:0.0.6")
}
```

### Method 2: GitHub Packages

See [GitHub Packages setup guide](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry) for authentication.

## ⚡ Quick Start

### 1. Define Your MVI Components

```kotlin
// State - represents your app's state at any moment
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

// Actions - represent user intents
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
}

// Effects - represent side effects (async operations)
sealed class CounterEffect {
    object SaveCount : CounterEffect()
}

// Events - represent one-time UI events
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
            CounterAction.Increment -> state.copy(count = state.count + 1)
            CounterAction.Decrement -> state.copy(count = state.count - 1)
            CounterAction.Reset -> state.copy(count = 0)
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
            CounterEffect.SaveCount -> {
                try {
                    // Simulate API call
                    delay(1000)
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
    
    // Public API
    fun increment() = sendAction(CounterAction.Increment)
    fun decrement() = sendAction(CounterAction.Decrement)
    fun reset() = sendAction(CounterAction.Reset)
    fun saveCount() = sendEffect(CounterEffect.SaveCount)
}
```

### 4. Use in UI

<details>
<summary><strong>🎨 Jetpack Compose (Recommended)</strong></summary>

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle one-time events
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                CounterEvent.CountSaved -> {
                    Toast.makeText(context, "Count saved!", Toast.LENGTH_SHORT).show()
                }
                is CounterEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    // UI
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = state.count.toString(),
            style = MaterialTheme.typography.headlineLarge
        )
        
        Row {
            Button(onClick = viewModel::decrement) { Text("-") }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = viewModel::increment) { Text("+") }
        }
        
        Button(
            onClick = viewModel::reset,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Reset")
        }
        
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
```

</details>

<details>
<summary><strong>🎭 Traditional Views</strong></summary>

```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModel: CounterViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeState()
        observeEvents()
    }
    
    private fun setupUI() {
        binding.incrementButton.setOnClickListener { viewModel.increment() }
        binding.decrementButton.setOnClickListener { viewModel.decrement() }
        binding.resetButton.setOnClickListener { viewModel.reset() }
    }
    
    private fun observeState() {
        lifecycleScope.launch {
            viewModel.stateFlow.collect { state ->
                binding.countText.text = state.count.toString()
                binding.progressBar.isVisible = state.isLoading
            }
        }
    }
    
    private fun observeEvents() {
        lifecycleScope.launch {
            viewModel.eventFlow.collect { event ->
                when (event) {
                    CounterEvent.CountSaved -> {
                        Toast.makeText(this@MainActivity, "Count saved!", Toast.LENGTH_SHORT).show()
                    }
                    is CounterEvent.ShowError -> {
                        Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
```

</details>

## 🏗️ Architecture

Lucid MVI follows a strict unidirectional data flow:

```
┌─────────────┐    Action     ┌──────────────┐    New State    ┌─────────────┐
│     UI      │──────────────▶│   ViewModel  │────────────────▶│    State    │
│             │               │              │                 │             │
│             │◀──────────────│              │◀────────────────│             │
└─────────────┘    Event      └──────────────┘                 └─────────────┘
                                      │                                ▲
                                      │ Effect                         │
                                      ▼                                │
                               ┌──────────────┐     Action             │
                               │ EffectHandler│────────────────────────┘
                               └──────────────┘
```

## 📈 Roadmap

- [x] ~~Compose integration support~~
- [ ] Debug tools and logging
- [ ] Kotlin Multiplatform support
- [ ] State persistence helpers
- [ ] Testing utilities
- [ ] Performance monitoring tools

## 📚 Documentation

- 📖 [**Complete Wiki**](WIKI.md) - Comprehensive framework documentation
- 🎯 [**Quick Start Guide**](#quick-start) - Get up and running in 5 minutes
- 🏗️ [**Architecture Guide**](WIKI.md#architecture-deep-dive) - Deep dive into MVI patterns
- 🧪 [**Testing Guide**](WIKI.md#best-practices) - Best practices for testing MVI
- 📱 [**Compose Integration**](WIKI.md#quick-start) - Modern UI with Jetpack Compose

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository
2. Open in Android Studio Arctic Fox or later
3. Run `./gradlew check` to verify setup
4. Run the demo app to see examples

### Areas for Contribution

- 📖 Documentation improvements
- 🐛 Bug fixes and performance optimizations
- ✨ New features and enhancements
- 🧪 Additional testing utilities
- 📱 More demo examples

## 💡 Why MVI?

| Benefit | Description |
|---------|-------------|
| **Predictable** | State changes are always triggered by actions, making the app behavior predictable |
| **Debuggable** | Unidirectional data flow makes it easy to trace bugs and understand state changes |
| **Testable** | Pure functions and clear separation of concerns make testing straightforward |
| **Scalable** | Handles complex state management and async operations elegantly |
| **Maintainable** | Clear architecture patterns make code easy to understand and modify |

## 📊 Comparison

| Feature | Lucid MVI | Redux | MvRx | Mobius |
|---------|-----------|-------|------|--------|
| **Learning Curve** | ⭐⭐ Easy | ⭐⭐⭐ Moderate | ⭐⭐⭐⭐ Hard | ⭐⭐⭐ Moderate |
| **Boilerplate** | ⭐⭐⭐⭐ Minimal | ⭐⭐ Some | ⭐⭐⭐ Moderate | ⭐⭐ Some |
| **Type Safety** | ⭐⭐⭐⭐⭐ Full | ⭐⭐⭐ Good | ⭐⭐⭐⭐⭐ Full | ⭐⭐⭐⭐ Very Good |
| **Async Handling** | ⭐⭐⭐⭐⭐ Excellent | ⭐⭐⭐ Good | ⭐⭐⭐⭐ Very Good | ⭐⭐⭐⭐ Very Good |
| **Compose Support** | ⭐⭐⭐⭐⭐ Native | ⭐⭐⭐ Good | ⭐⭐⭐⭐ Very Good | ⭐⭐ Limited |
| **Bundle Size** | ⭐⭐⭐⭐⭐ < 20KB | ⭐⭐⭐ ~50KB | ⭐⭐ ~100KB | ⭐⭐⭐ ~40KB |

## ⚙️ Requirements

- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36
- **Kotlin**: 2.0.21+
- **Coroutines**: 1.9.0+
- **Compose**: 1.7.6+ (optional)

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

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=greathousesh/Lucid-MVI&type=Date)](https://star-history.com/#greathousesh/Lucid-MVI&Date)

---

<p align="center">
  <strong>Built with ❤️ for the Android community</strong><br>
  <a href="https://github.com/greathousesh/Lucid-MVI/issues">Report Issues</a> •
  <a href="https://github.com/greathousesh/Lucid-MVI/discussions">Join Discussions</a> •
  <a href="https://github.com/greathousesh/Lucid-MVI/wiki">Read Wiki</a>
</p>