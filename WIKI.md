# Lucid MVI Framework Wiki

## 📋 Table of Contents

1. [Framework Introduction](#framework-introduction)
2. [Design Philosophy](#design-philosophy)
3. [Core Concepts](#core-concepts)
4. [Architecture Deep Dive](#architecture-deep-dive)
5. [Quick Start](#quick-start)
6. [Detailed Tutorial](#detailed-tutorial)
7. [Best Practices](#best-practices)
8. [Advanced Usage](#advanced-usage)
9. [FAQ](#faq)
10. [Example Projects](#example-projects)

---

## Framework Introduction

Lucid MVI is a lightweight, type-safe Android MVI (Model-View-Intent) architecture framework. Built on Kotlin Coroutines, it provides reactive state management and clear unidirectional data flow.

### 🎯 Core Features

- **🏗️ Kotlin Coroutines Based** - Reactive architecture with fully asynchronous processing
- **🔄 Unidirectional Data Flow** - Predictable state management, easy to debug
- **🎯 Type Safe** - Compile-time type checking reduces runtime errors  
- **🧪 Easy to Test** - Pure functional reducers, predictable side effect handling
- **📦 Lightweight** - No additional dependencies, core MVI implementation only
- **🚀 Production Ready** - Includes lifecycle awareness and thread safety features

### 📊 Technical Specifications

- **Minimum Android SDK**: API 24 (Android 7.0)
- **Kotlin Version**: 2.0.21+
- **Coroutines Version**: 1.9.0+
- **Package Size**: < 20KB
- **Dependencies**: Android standard library only

---

## Design Philosophy

### 🎭 MVI Architecture Pattern

MVI (Model-View-Intent) is an architecture pattern inspired by functional programming, featuring:

```
Intent → Model → View → Intent
   ↑                    ↓
   └────────────────────┘
```

#### Core Principles

1. **Single Source of Truth**
   - Application state is stored in a single State object
   - All UI updates are based on State changes

2. **Immutability**
   - State objects are immutable
   - State updates are done by creating new State instances

3. **Pure Functions**
   - Reducers are pure functions, same input always produces same output
   - No side effects, easy to test and debug

4. **Unidirectional Data Flow**
   - Data can only flow in one direction: Intent → Model → View
   - Avoids the complexity of two-way binding

### 🏛️ Lucid MVI Design Philosophy

#### Simplicity
- Minimize boilerplate code
- Intuitive API design
- Clear separation of concerns

#### Predictability
- Deterministic state transitions
- Traceable data flow
- Time-travel debugging support

#### Scalability
- Support for complex business logic
- Modular architecture design
- Easy team collaboration

#### Performance
- Asynchronous processing based on Kotlin Coroutines
- Efficient state update mechanism
- Memory-friendly design

---

## Core Concepts

### 🏗️ Four Core Components

#### 1. State
**Definition**: Complete state snapshot of the application at a given moment

```kotlin
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

**Characteristics**:
- Immutable data class
- Contains all information needed by the UI
- Serializable, supports state saving and restoration

#### 2. Action
**Definition**: Immutable objects describing user intent or system events

```kotlin
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
    data class SetCount(val count: Int) : CounterAction()
}
```

**Characteristics**:
- Sealed classes, type-safe
- Carries data needed for the operation
- Expresses "what to do" rather than "how to do it"

#### 3. Effect
**Definition**: Operations requiring asynchronous processing, such as network requests, database operations

```kotlin
sealed class CounterEffect {
    object SaveCount : CounterEffect()
    data class LoadCount(val userId: String) : CounterEffect()
}
```

**Characteristics**:
- Asynchronous operations separated from reducer
- Can trigger new Actions
- Can produce Events

#### 4. Event
**Definition**: One-time UI events, such as Toast, navigation, dialogs

```kotlin
sealed class CounterEvent {
    object CountSaved : CounterEvent()
    data class ShowError(val message: String) : CounterEvent()
    object NavigateToSettings : CounterEvent()
}
```

**Characteristics**:
- One-time consumption
- Does not affect State
- Used for UI feedback

### 🔧 Core Interfaces

#### StateReducer
```kotlin
interface StateReducer<State, Action> {
    fun reduce(state: State, action: Action): State
}
```

**Responsibilities**:
- Produce new state based on current state and action
- Pure function, no side effects
- Synchronous execution

#### EffectHandler
```kotlin
interface EffectHandler<State, Action, Effect, Event> {
    suspend fun handle(
        state: State,
        effect: Effect,
        dispatch: suspend (Action) -> Unit,
        emit: suspend (Event) -> Unit
    )
}
```

**Responsibilities**:
- Handle asynchronous side effects
- Can dispatch new Actions
- Can emit Events

---

## Architecture Deep Dive

### 🔄 Data Flow Diagram

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

### 🏗️ BaseMVIViewModel Architecture

```kotlin
abstract class BaseMVIViewModel<State, Action, Effect, Event>(
    private val reducer: StateReducer<State, Action>,
    private val effectHandler: EffectHandler<State, Action, Effect, Event>
) : ViewModel()
```

#### Core Mechanisms

1. **Action Channel**: Unlimited capacity channel for handling user actions
2. **Effect Channel**: Unlimited capacity channel for handling side effects
3. **State Flow**: Hot flow for publishing state updates
4. **Event Flow**: Hot flow for publishing one-time events

#### Processing Flow

1. **Action Processing**:
   ```kotlin
   Action → Reducer → New State → StateFlow
   ```

2. **Effect Processing**:
   ```kotlin
   Effect → EffectHandler → Action/Event
   ```

3. **Lifecycle Management**:
   - Based on ViewModel scope
   - Automatic resource cleanup
   - Thread-safe operations

---

## Quick Start

### 📦 1. Add Dependencies

#### build.gradle.kts (Project)
```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

#### build.gradle.kts (Module)
```kotlin
dependencies {
    implementation("com.github.greathousesh:Lucid-MVI:0.0.6")
}
```

### 🎯 2. Define MVI Components

```kotlin
// 1. Define State
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
)

// 2. Define Actions
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
}

// 3. Define Effects
sealed class CounterEffect {
    object SaveCount : CounterEffect()
}

// 4. Define Events
sealed class CounterEvent {
    object CountSaved : CounterEvent()
    data class ShowError(val message: String) : CounterEvent()
}
```

### 🔧 3. Implement Reducer and EffectHandler

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
                    // Simulate network request
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

### 🎭 4. Create ViewModel

```kotlin
class CounterViewModel : BaseMVIViewModel<CounterState, CounterAction, CounterEffect, CounterEvent>(
    reducer = CounterReducer(),
    effectHandler = CounterEffectHandler()
) {
    override fun initialState(): CounterState = CounterState()
    
    // Convenience methods
    fun increment() = sendAction(CounterAction.Increment)
    fun decrement() = sendAction(CounterAction.Decrement)
    fun reset() = sendAction(CounterAction.Reset)
    fun saveCount() = sendEffect(CounterEffect.SaveCount)
}
```

### 📱 5. Use in Activity/Fragment

#### Traditional Views
```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModel: CounterViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Observe state
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
}
```

#### Jetpack Compose
```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle events
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is CounterEvent.CountSaved -> {
                    Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                }
                is CounterEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    // UI content
    Column {
        Text(
            text = state.count.toString(),
            style = MaterialTheme.typography.headlineLarge
        )
        
        Row {
            Button(onClick = { viewModel.decrement() }) {
                Text("-")
            }
            Button(onClick = { viewModel.increment() }) {
                Text("+")
            }
        }
        
        if (state.isLoading) {
            CircularProgressIndicator()
        }
    }
}
```

---

## Detailed Tutorial

### 📚 Tutorial: Building a Todo Application

Let's learn Lucid MVI in depth by building a Todo application.

#### Step 1: Define Data Models

```kotlin
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TodoFilter {
    ALL, ACTIVE, COMPLETED
}
```

#### Step 2: Define MVI Components

```kotlin
// State
data class TodoState(
    val todos: List<TodoItem> = emptyList(),
    val filter: TodoFilter = TodoFilter.ALL,
    val isLoading: Boolean = false,
    val editingTodo: TodoItem? = null
)

// Actions
sealed class TodoAction {
    data class AddTodo(val title: String, val description: String = "") : TodoAction()
    data class UpdateTodo(val todo: TodoItem) : TodoAction()
    data class DeleteTodo(val todoId: String) : TodoAction()
    data class ToggleTodo(val todoId: String) : TodoAction()
    data class SetFilter(val filter: TodoFilter) : TodoAction()
    data class StartEditing(val todo: TodoItem) : TodoAction()
    object CancelEditing : TodoAction()
    object LoadTodos : TodoAction()
    object LoadTodosCompleted : TodoAction()
}

// Effects
sealed class TodoEffect {
    object LoadTodos : TodoEffect()
    object SaveTodos : TodoEffect()
    data class DeleteTodo(val todoId: String) : TodoEffect()
}

// Events
sealed class TodoEvent {
    object TodosLoaded : TodoEvent()
    object TodosSaved : TodoEvent()
    data class TodoDeleted(val title: String) : TodoEvent()
    data class ShowError(val message: String) : TodoEvent()
}
```

#### Step 3: Implement Reducer

```kotlin
class TodoReducer : StateReducer<TodoState, TodoAction> {
    override fun reduce(state: TodoState, action: TodoAction): TodoState {
        return when (action) {
            is TodoAction.AddTodo -> {
                val newTodo = TodoItem(title = action.title, description = action.description)
                state.copy(todos = state.todos + newTodo)
            }
            
            is TodoAction.UpdateTodo -> {
                state.copy(
                    todos = state.todos.map { todo ->
                        if (todo.id == action.todo.id) action.todo else todo
                    },
                    editingTodo = null
                )
            }
            
            is TodoAction.DeleteTodo -> {
                state.copy(todos = state.todos.filter { it.id != action.todoId })
            }
            
            is TodoAction.ToggleTodo -> {
                state.copy(
                    todos = state.todos.map { todo ->
                        if (todo.id == action.todoId) {
                            todo.copy(isCompleted = !todo.isCompleted)
                        } else todo
                    }
                )
            }
            
            is TodoAction.SetFilter -> state.copy(filter = action.filter)
            
            is TodoAction.StartEditing -> state.copy(editingTodo = action.todo)
            
            is TodoAction.CancelEditing -> state.copy(editingTodo = null)
            
            is TodoAction.LoadTodos -> state.copy(isLoading = true)
            
            is TodoAction.LoadTodosCompleted -> state.copy(isLoading = false)
        }
    }
}
```

#### Step 4: Implement EffectHandler

```kotlin
class TodoEffectHandler : EffectHandler<TodoState, TodoAction, TodoEffect, TodoEvent> {
    
    private val repository = TodoRepository() // Hypothetical data repository
    
    override suspend fun handle(
        state: TodoState,
        effect: TodoEffect,
        dispatch: suspend (TodoAction) -> Unit,
        emit: suspend (TodoEvent) -> Unit
    ) {
        when (effect) {
            is TodoEffect.LoadTodos -> {
                try {
                    val todos = repository.loadTodos()
                    // Batch add todos
                    todos.forEach { todo ->
                        dispatch(TodoAction.AddTodo(todo.title, todo.description))
                    }
                    dispatch(TodoAction.LoadTodosCompleted)
                    emit(TodoEvent.TodosLoaded)
                } catch (e: Exception) {
                    dispatch(TodoAction.LoadTodosCompleted)
                    emit(TodoEvent.ShowError("Failed to load todos: ${e.message}"))
                }
            }
            
            is TodoEffect.SaveTodos -> {
                try {
                    repository.saveTodos(state.todos)
                    emit(TodoEvent.TodosSaved)
                } catch (e: Exception) {
                    emit(TodoEvent.ShowError("Failed to save todos: ${e.message}"))
                }
            }
            
            is TodoEffect.DeleteTodo -> {
                try {
                    repository.deleteTodo(effect.todoId)
                    val deletedTodo = state.todos.find { it.id == effect.todoId }
                    deletedTodo?.let {
                        emit(TodoEvent.TodoDeleted(it.title))
                    }
                } catch (e: Exception) {
                    emit(TodoEvent.ShowError("Failed to delete todo: ${e.message}"))
                }
            }
        }
    }
}
```

#### Step 5: Create ViewModel

```kotlin
class TodoViewModel : BaseMVIViewModel<TodoState, TodoAction, TodoEffect, TodoEvent>(
    reducer = TodoReducer(),
    effectHandler = TodoEffectHandler()
) {
    override fun initialState(): TodoState = TodoState()
    
    // Public API
    fun addTodo(title: String, description: String = "") {
        if (title.isNotBlank()) {
            sendAction(TodoAction.AddTodo(title.trim(), description.trim()))
            sendEffect(TodoEffect.SaveTodos)
        }
    }
    
    fun toggleTodo(todoId: String) {
        sendAction(TodoAction.ToggleTodo(todoId))
        sendEffect(TodoEffect.SaveTodos)
    }
    
    fun deleteTodo(todoId: String) {
        sendAction(TodoAction.DeleteTodo(todoId))
        sendEffect(TodoEffect.DeleteTodo(todoId))
    }
    
    fun setFilter(filter: TodoFilter) {
        sendAction(TodoAction.SetFilter(filter))
    }
    
    fun loadTodos() {
        sendAction(TodoAction.LoadTodos)
        sendEffect(TodoEffect.LoadTodos)
    }
    
    // Computed properties
    fun getFilteredTodos(state: TodoState): List<TodoItem> {
        return when (state.filter) {
            TodoFilter.ALL -> state.todos
            TodoFilter.ACTIVE -> state.todos.filter { !it.isCompleted }
            TodoFilter.COMPLETED -> state.todos.filter { it.isCompleted }
        }
    }
    
    fun getStats(state: TodoState): Triple<Int, Int, Int> {
        val total = state.todos.size
        val completed = state.todos.count { it.isCompleted }
        val active = total - completed
        return Triple(total, active, completed)
    }
}
```

---

## Best Practices

### 🎯 1. State Design Principles

#### ✅ Good State Design
```kotlin
data class UserState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)
```

#### ❌ Avoid These Designs
```kotlin
// Don't include UI component references in State
data class BadState(
    val user: User? = null,
    val textView: TextView? = null // ❌ Don't do this
)

// Don't include callback functions in State
data class BadState(
    val user: User? = null,
    val onUserClick: (User) -> Unit // ❌ Don't do this
)
```

### 🔧 2. Action Design Guidelines

#### ✅ Clear Action Naming
```kotlin
sealed class UserAction {
    object LoadUser : UserAction()                    // Clear action
    data class UpdateUserName(val name: String) : UserAction()  // Carries necessary data
    object RefreshUser : UserAction()                 // Distinguishes different loading scenarios
}
```

#### ❌ Avoid These Action Designs
```kotlin
sealed class BadAction {
    data class DoSomething(val data: Any) : BadAction()  // ❌ Vague
    object Action1 : BadAction()                         // ❌ Meaningless naming
}
```

### 🎪 3. Effect vs Action Choice

#### Use Action for:
- Synchronous state updates
- Simple business logic
- Operations that don't require external dependencies

```kotlin
// ✅ Suitable for Action
CounterAction.Increment  // Simple counter increment
CounterAction.Reset      // Reset state
```

#### Use Effect for:
- Asynchronous operations
- Network requests
- Database operations
- File I/O

```kotlin
// ✅ Suitable for Effect
UserEffect.LoadUserFromApi(userId)     // Network request
UserEffect.SaveUserToDatabase(user)   // Database operation
```

### 🌊 4. Event Handling Best Practices

#### ✅ Correct Event Usage
```kotlin
sealed class UserEvent {
    object UserSaved : UserEvent()                    // Success feedback
    data class ShowError(val message: String) : UserEvent()  // Error handling
    data class NavigateToProfile(val userId: String) : UserEvent()  // Navigation
}
```

#### Proper Event Handling in UI
```kotlin
// In Compose
LaunchedEffect(viewModel) {
    viewModel.eventFlow.collect { event ->
        when (event) {
            is UserEvent.UserSaved -> {
                snackbarHostState.showSnackbar("User saved successfully")
            }
            is UserEvent.ShowError -> {
                snackbarHostState.showSnackbar(event.message)
            }
            is UserEvent.NavigateToProfile -> {
                navController.navigate("profile/${event.userId}")
            }
        }
    }
}
```

### 🧪 5. Testing Strategy

#### Reducer Testing
```kotlin
class UserReducerTest {
    private val reducer = UserReducer()
    
    @Test
    fun `should update user name when UpdateUserName action is dispatched`() {
        // Given
        val initialState = UserState(user = User(id = "1", name = "John"))
        val action = UserAction.UpdateUserName("Jane")
        
        // When
        val newState = reducer.reduce(initialState, action)
        
        // Then
        assertEquals("Jane", newState.user?.name)
    }
}
```

#### EffectHandler Testing
```kotlin
class UserEffectHandlerTest {
    private val mockRepository = mockk<UserRepository>()
    private val effectHandler = UserEffectHandler(mockRepository)
    
    @Test
    fun `should emit UserLoaded event when LoadUser effect succeeds`() = runTest {
        // Given
        val user = User(id = "1", name = "John")
        coEvery { mockRepository.getUser("1") } returns user
        
        val events = mutableListOf<UserEvent>()
        val actions = mutableListOf<UserAction>()
        
        // When
        effectHandler.handle(
            state = UserState(),
            effect = UserEffect.LoadUser("1"),
            dispatch = { actions.add(it) },
            emit = { events.add(it) }
        )
        
        // Then
        assertTrue(events.contains(UserEvent.UserLoaded(user)))
    }
}
```

#### ViewModel Integration Testing
```kotlin
class UserViewModelTest {
    @get:Rule
    val testDispatcherRule = TestDispatcherRule()
    
    private val mockRepository = mockk<UserRepository>()
    private lateinit var viewModel: UserViewModel
    
    @Before
    fun setup() {
        viewModel = UserViewModel(
            reducer = UserReducer(),
            effectHandler = UserEffectHandler(mockRepository)
        )
    }
    
    @Test
    fun `should load user when loadUser is called`() = runTest {
        // Given
        val user = User(id = "1", name = "John")
        coEvery { mockRepository.getUser("1") } returns user
        
        // When
        viewModel.loadUser("1")
        
        // Then
        viewModel.stateFlow.test {
            val state = awaitItem()
            assertEquals(user, state.user)
        }
    }
}
```

---

## Advanced Usage

### 🔀 1. Composing Multiple Reducers

```kotlin
class CompositeReducer : StateReducer<AppState, AppAction> {
    private val userReducer = UserReducer()
    private val todoReducer = TodoReducer()
    
    override fun reduce(state: AppState, action: AppAction): AppState {
        return when (action) {
            is AppAction.UserAction -> {
                state.copy(userState = userReducer.reduce(state.userState, action.userAction))
            }
            is AppAction.TodoAction -> {
                state.copy(todoState = todoReducer.reduce(state.todoState, action.todoAction))
            }
            is AppAction.GlobalAction -> {
                // Handle global actions
                handleGlobalAction(state, action.globalAction)
            }
        }
    }
}
```

### 🔗 2. Effect Chaining

```kotlin
class ChainedEffectHandler : EffectHandler<UserState, UserAction, UserEffect, UserEvent> {
    override suspend fun handle(
        state: UserState,
        effect: UserEffect,
        dispatch: suspend (UserAction) -> Unit,
        emit: suspend (UserEvent) -> Unit
    ) {
        when (effect) {
            is UserEffect.LoginUser -> {
                try {
                    // 1. Authenticate user
                    val authResult = authService.authenticate(effect.credentials)
                    dispatch(UserAction.SetAuthToken(authResult.token))
                    
                    // 2. Load user profile
                    val user = userService.getUserProfile(authResult.userId)
                    dispatch(UserAction.SetUser(user))
                    
                    // 3. Load user settings
                    val settings = settingsService.getUserSettings(authResult.userId)
                    dispatch(UserAction.SetUserSettings(settings))
                    
                    emit(UserEvent.LoginSuccess)
                } catch (e: Exception) {
                    emit(UserEvent.LoginError(e.message ?: "Unknown error"))
                }
            }
        }
    }
}
```

### 🎯 3. Conditional Effect Handling

```kotlin
class ConditionalEffectHandler : EffectHandler<TodoState, TodoAction, TodoEffect, TodoEvent> {
    override suspend fun handle(
        state: TodoState,
        effect: TodoEffect,
        dispatch: suspend (TodoAction) -> Unit,
        emit: suspend (TodoEvent) -> Unit
    ) {
        when (effect) {
            is TodoEffect.SaveTodos -> {
                // Only save when todos have actually changed
                if (state.todos != lastSavedTodos) {
                    try {
                        repository.saveTodos(state.todos)
                        lastSavedTodos = state.todos.toList()
                        emit(TodoEvent.TodosSaved)
                    } catch (e: Exception) {
                        emit(TodoEvent.SaveError(e.message ?: "Save failed"))
                    }
                }
            }
            
            is TodoEffect.AutoSave -> {
                // Decide whether to auto-save based on state conditions
                if (state.todos.isNotEmpty() && !state.isLoading) {
                    dispatch(TodoAction.TriggerSave)
                }
            }
        }
    }
}
```

### 🕰️ 4. Time Travel Debugging

```kotlin
class TimeTravel<State> {
    private val history = mutableListOf<State>()
    private var currentIndex = -1
    
    fun record(state: State) {
        // Remove history after current position
        if (currentIndex < history.size - 1) {
            history.removeAll(history.drop(currentIndex + 1))
        }
        
        history.add(state)
        currentIndex = history.size - 1
    }
    
    fun undo(): State? {
        return if (canUndo()) {
            currentIndex--
            history[currentIndex]
        } else null
    }
    
    fun redo(): State? {
        return if (canRedo()) {
            currentIndex++
            history[currentIndex]
        } else null
    }
    
    fun canUndo(): Boolean = currentIndex > 0
    fun canRedo(): Boolean = currentIndex < history.size - 1
}

// Usage in ViewModel
class DebuggableViewModel : BaseMVIViewModel<State, Action, Effect, Event>(
    reducer = DebuggableReducer(),
    effectHandler = EffectHandler()
) {
    private val timeTravel = TimeTravel<State>()
    
    override suspend fun setState(reducer: State.() -> State) {
        val newState = latestState.reducer()
        timeTravel.record(newState)
        super.setState { newState }
    }
    
    fun undo() {
        timeTravel.undo()?.let { state ->
            super.setState { state }
        }
    }
    
    fun redo() {
        timeTravel.redo()?.let { state ->
            super.setState { state }
        }
    }
}
```

### 🔄 5. Middleware Pattern

```kotlin
interface Middleware<State, Action> {
    suspend fun process(
        state: State,
        action: Action,
        next: suspend (Action) -> Unit
    )
}

class LoggingMiddleware<State, Action> : Middleware<State, Action> {
    override suspend fun process(
        state: State,
        action: Action,
        next: suspend (Action) -> Unit
    ) {
        println("Processing action: $action")
        println("Current state: $state")
        next(action)
    }
}

class AnalyticsMiddleware<State, Action> : Middleware<State, Action> {
    override suspend fun process(
        state: State,
        action: Action,
        next: suspend (Action) -> Unit
    ) {
        // Record analytics data
        analytics.track("action_dispatched", mapOf(
            "action_type" to action::class.simpleName,
            "state_type" to state::class.simpleName
        ))
        next(action)
    }
}
```

---

## FAQ

### ❓ Q1: What data should State contain?

**A**: State should contain all data needed for UI rendering, including:
- Business data (user info, lists, etc.)
- UI state (loading, error, selected items, etc.)
- Navigation state (current page, dialog state, etc.)

**Should NOT contain**:
- UI component references
- Callback functions
- Non-serializable objects

### ❓ Q2: When to use Effect vs Action?

**A**: 
- **Use Action**: Synchronous state updates, simple calculations, no external dependencies
- **Use Effect**: Asynchronous operations, network requests, database operations, file I/O

```kotlin
// Action - synchronous update
CounterAction.Increment

// Effect - asynchronous operation
UserEffect.LoadFromApi
```

### ❓ Q3: How to handle complex asynchronous operations?

**A**: Use Effect chaining or conditional handling:

```kotlin
override suspend fun handle(effect: Effect, dispatch: ..., emit: ...) {
    when (effect) {
        is ComplexEffect.MultiStep -> {
            try {
                // Step 1
                val result1 = step1()
                dispatch(Action.Step1Complete(result1))
                
                // Step 2
                val result2 = step2(result1)
                dispatch(Action.Step2Complete(result2))
                
                emit(Event.AllStepsComplete)
            } catch (e: Exception) {
                emit(Event.Error(e.message))
            }
        }
    }
}
```

### ❓ Q4: How to optimize performance?

**A**: 
1. **Use immutable collections**
2. **Avoid frequent state updates**
3. **Use coroutine scopes properly**
4. **Implement state diff checking**

```kotlin
// Optimize state updates
override fun reduce(state: State, action: Action): State {
    val newData = when (action) {
        is Action.UpdateList -> {
            if (action.newList == state.list) {
                return state // Avoid unnecessary updates
            }
            action.newList
        }
        else -> state.list
    }
    
    return state.copy(list = newData)
}
```

### ❓ Q5: How to handle memory leaks?

**A**: Lucid MVI automatically handles lifecycle:
- Coroutines based on ViewModel scope
- Automatic Channel and Flow cleanup
- Resource release in onCleared()

```kotlin
// Framework handles this automatically
override fun onCleared() {
    super.onCleared()
    actionChannel.close()
    effectChannel.close()
}
```

---

## Example Projects

### 🏠 Demo Application Structure

This project includes a comprehensive demo application showcasing different levels of MVI implementation:

#### 1. HomeActivity - Navigation Hub
- Material 3 design
- Unified example entry point
- Responsive layout

#### 2. CounterActivity - Basic Counter
- **Tech Stack**: Traditional Views + ViewBinding
- **Complexity**: ⭐
- **Learning Focus**: MVI basics

```kotlin
// Complete counter implementation
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

#### 3. CounterComposeActivity - Modern Counter
- **Tech Stack**: Jetpack Compose + Material 3
- **Complexity**: ⭐⭐
- **Learning Focus**: Compose integration with MVI

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            // Handle events
        }
    }
    
    // UI components
    CounterContent(
        state = state,
        onIncrement = viewModel::increment,
        onDecrement = viewModel::decrement
    )
}
```

#### 4. TodoActivity - Complex State Management
- **Tech Stack**: Jetpack Compose + Complex Business Logic
- **Complexity**: ⭐⭐⭐⭐
- **Learning Focus**: 
  - CRUD operations
  - List management
  - Filtering and search
  - Batch operations
  - State persistence

```kotlin
// Complex Todo state management
data class TodoState(
    val todos: List<TodoItem> = emptyList(),
    val filter: TodoFilter = TodoFilter.ALL,
    val isLoading: Boolean = false,
    val editingTodo: TodoItem? = null,
    val searchQuery: String = ""
)

// Rich business operations
class TodoViewModel : BaseMVIViewModel<...> {
    fun addTodo(title: String, description: String = "") { ... }
    fun updateTodo(todo: TodoItem) { ... }
    fun deleteTodo(todoId: String) { ... }
    fun toggleTodo(todoId: String) { ... }
    fun setFilter(filter: TodoFilter) { ... }
    fun searchTodos(query: String) { ... }
    fun clearCompleted() { ... }
    
    // Computed properties
    fun getFilteredTodos(state: TodoState): List<TodoItem> { ... }
    fun getStats(state: TodoState): TodoStats { ... }
}
```

### 🎯 Running Examples

1. **Clone the project**
   ```bash
   git clone https://github.com/greathousesh/Lucid-MVI.git
   ```

2. **Open Android Studio**
   - Import project
   - Sync Gradle

3. **Run the application**
   - Select app module
   - Run on device or emulator

4. **Explore examples**
   - Select different examples from home screen
   - Experience the power of MVI architecture

### 📚 Suggested Learning Path

1. **Beginners**: CounterActivity → CounterComposeActivity
2. **Intermediate**: TodoActivity → Custom implementation
3. **Expert**: Study source code → Contribute code

---

## 📞 Support and Community

### 🐛 Issue Reporting
- [GitHub Issues](https://github.com/greathousesh/Lucid-MVI/issues)

### 💬 Discussion and Support
- [GitHub Discussions](https://github.com/greathousesh/Lucid-MVI/discussions)

### 🤝 Contributing
- Fork the project
- Create feature branch
- Submit Pull Request

### 📄 License
Apache 2.0 License - See [LICENSE](LICENSE) file

---

**Happy Coding with Lucid MVI! 🚀**