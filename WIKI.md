# Lucid MVI Framework Wiki

## 📋 目录

1. [框架介绍](#框架介绍)
2. [设计理念](#设计理念)
3. [核心概念](#核心概念)
4. [架构详解](#架构详解)
5. [快速开始](#快速开始)
6. [详细教程](#详细教程)
7. [最佳实践](#最佳实践)
8. [高级用法](#高级用法)
9. [常见问题](#常见问题)
10. [示例项目](#示例项目)

---

## 框架介绍

Lucid MVI 是一个轻量级、类型安全的Android MVI (Model-View-Intent) 架构框架。它基于Kotlin协程构建，提供了响应式的状态管理和清晰的单向数据流。

### 🎯 核心特性

- **🏗️ 基于Kotlin协程** - 响应式架构，完全异步处理
- **🔄 单向数据流** - 可预测的状态管理，易于调试
- **🎯 类型安全** - 编译时类型检查，减少运行时错误  
- **🧪 易于测试** - 纯函数式reducer，可预测的副作用处理
- **📦 轻量级** - 无额外依赖，仅核心MVI实现
- **🚀 生产就绪** - 包含生命周期感知和线程安全特性

### 📊 技术规格

- **最小Android SDK**: API 24 (Android 7.0)
- **Kotlin版本**: 2.0.21+
- **Coroutines版本**: 1.9.0+
- **包大小**: < 20KB
- **依赖**: 仅Android标准库

---

## 设计理念

### 🎭 MVI 架构模式

MVI (Model-View-Intent) 是一种受函数式编程启发的架构模式，具有以下特点：

```
Intent → Model → View → Intent
   ↑                    ↓
   └────────────────────┘
```

#### 核心原则

1. **单一数据源 (Single Source of Truth)**
   - 应用状态存储在单一的State对象中
   - 所有UI更新都基于State变化

2. **不可变性 (Immutability)**
   - State对象是不可变的
   - 状态更新通过创建新的State实例

3. **纯函数式更新 (Pure Functions)**
   - Reducer是纯函数，相同输入总是产生相同输出
   - 无副作用，易于测试和调试

4. **单向数据流 (Unidirectional Data Flow)**
   - 数据只能单向流动：Intent → Model → View
   - 避免双向绑定的复杂性

### 🏛️ Lucid MVI 设计哲学

#### 简洁性 (Simplicity)
- 最小化样板代码
- 直观的API设计
- 清晰的概念分离

#### 可预测性 (Predictability)
- 确定性的状态转换
- 可追踪的数据流
- 时间旅行调试支持

#### 可扩展性 (Scalability)
- 支持复杂的业务逻辑
- 模块化的架构设计
- 易于团队协作

#### 性能优化 (Performance)
- 基于Kotlin协程的异步处理
- 高效的状态更新机制
- 内存友好的设计

---

## 核心概念

### 🏗️ 四大核心组件

#### 1. State (状态)
**定义**: 应用在某一时刻的完整状态快照

```kotlin
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

**特点**:
- 不可变数据类
- 包含UI所需的所有信息
- 可序列化，支持状态保存和恢复

#### 2. Action (动作)
**定义**: 描述用户意图或系统事件的不可变对象

```kotlin
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
    data class SetCount(val count: Int) : CounterAction()
}
```

**特点**:
- 密封类，类型安全
- 携带操作所需的数据
- 表达"做什么"而不是"怎么做"

#### 3. Effect (副作用)
**定义**: 需要异步处理的操作，如网络请求、数据库操作

```kotlin
sealed class CounterEffect {
    object SaveCount : CounterEffect()
    data class LoadCount(val userId: String) : CounterEffect()
}
```

**特点**:
- 与reducer分离的异步操作
- 可以触发新的Action
- 可以产生Event

#### 4. Event (事件)
**定义**: 一次性的UI事件，如Toast、导航、对话框

```kotlin
sealed class CounterEvent {
    object CountSaved : CounterEvent()
    data class ShowError(val message: String) : CounterEvent()
    object NavigateToSettings : CounterEvent()
}
```

**特点**:
- 一次性消费
- 不影响State
- 用于UI反馈

### 🔧 核心接口

#### StateReducer
```kotlin
interface StateReducer<State, Action> {
    fun reduce(state: State, action: Action): State
}
```

**职责**:
- 根据当前状态和动作产生新状态
- 纯函数，无副作用
- 同步执行

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

**职责**:
- 处理异步副作用
- 可以派发新的Action
- 可以发出Event

---

## 架构详解

### 🔄 数据流图

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

### 🏗️ BaseMVIViewModel 架构

```kotlin
abstract class BaseMVIViewModel<State, Action, Effect, Event>(
    private val reducer: StateReducer<State, Action>,
    private val effectHandler: EffectHandler<State, Action, Effect, Event>
) : ViewModel()
```

#### 核心机制

1. **Action Channel**: 无限容量通道，处理用户动作
2. **Effect Channel**: 无限容量通道，处理副作用
3. **State Flow**: 热流，发布状态更新
4. **Event Flow**: 热流，发布一次性事件

#### 处理流程

1. **Action处理**:
   ```kotlin
   Action → Reducer → New State → StateFlow
   ```

2. **Effect处理**:
   ```kotlin
   Effect → EffectHandler → Action/Event
   ```

3. **生命周期管理**:
   - 基于ViewModel的作用域
   - 自动清理资源
   - 线程安全

---

## 快速开始

### 📦 1. 添加依赖

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

### 🎯 2. 定义MVI组件

```kotlin
// 1. 定义状态
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
)

// 2. 定义动作
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
}

// 3. 定义副作用
sealed class CounterEffect {
    object SaveCount : CounterEffect()
}

// 4. 定义事件
sealed class CounterEvent {
    object CountSaved : CounterEvent()
    data class ShowError(val message: String) : CounterEvent()
}
```

### 🔧 3. 实现Reducer和EffectHandler

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
                    // 模拟网络请求
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

### 🎭 4. 创建ViewModel

```kotlin
class CounterViewModel : BaseMVIViewModel<CounterState, CounterAction, CounterEffect, CounterEvent>(
    reducer = CounterReducer(),
    effectHandler = CounterEffectHandler()
) {
    override fun initialState(): CounterState = CounterState()
    
    // 便捷方法
    fun increment() = sendAction(CounterAction.Increment)
    fun decrement() = sendAction(CounterAction.Decrement)
    fun reset() = sendAction(CounterAction.Reset)
    fun saveCount() = sendEffect(CounterEffect.SaveCount)
}
```

### 📱 5. 在Activity/Fragment中使用

#### 传统View
```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModel: CounterViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 观察状态
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
}
```

#### Jetpack Compose
```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // 处理事件
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
    
    // UI内容
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

## 详细教程

### 📚 教程1: 构建Todo应用

让我们通过构建一个Todo应用来深入学习Lucid MVI。

#### 步骤1: 定义数据模型

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

#### 步骤2: 定义MVI组件

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

#### 步骤3: 实现Reducer

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

#### 步骤4: 实现EffectHandler

```kotlin
class TodoEffectHandler : EffectHandler<TodoState, TodoAction, TodoEffect, TodoEvent> {
    
    private val repository = TodoRepository() // 假设的数据仓库
    
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
                    // 批量添加todos
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

#### 步骤5: 创建ViewModel

```kotlin
class TodoViewModel : BaseMVIViewModel<TodoState, TodoAction, TodoEffect, TodoEvent>(
    reducer = TodoReducer(),
    effectHandler = TodoEffectHandler()
) {
    override fun initialState(): TodoState = TodoState()
    
    // 公共API
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
    
    // 计算属性
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

## 最佳实践

### 🎯 1. State设计原则

#### ✅ 良好的State设计
```kotlin
data class UserState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)
```

#### ❌ 避免的设计
```kotlin
// 不要在State中包含UI组件引用
data class BadState(
    val user: User? = null,
    val textView: TextView? = null // ❌ 不要这样做
)

// 不要在State中包含回调函数
data class BadState(
    val user: User? = null,
    val onUserClick: (User) -> Unit // ❌ 不要这样做
)
```

### 🔧 2. Action设计指南

#### ✅ 清晰的Action命名
```kotlin
sealed class UserAction {
    object LoadUser : UserAction()                    // 明确的动作
    data class UpdateUserName(val name: String) : UserAction()  // 携带必要数据
    object RefreshUser : UserAction()                 // 区分不同的加载场景
}
```

#### ❌ 避免的Action设计
```kotlin
sealed class BadAction {
    data class DoSomething(val data: Any) : BadAction()  // ❌ 含糊不清
    object Action1 : BadAction()                         // ❌ 无意义的命名
}
```

### 🎪 3. Effect vs Action 选择

#### 使用Action的场景
- 同步状态更新
- 简单的业务逻辑
- 不需要外部依赖的操作

```kotlin
// ✅ 适合用Action
CounterAction.Increment  // 简单的计数增加
CounterAction.Reset      // 重置状态
```

#### 使用Effect的场景
- 异步操作
- 网络请求
- 数据库操作
- 文件I/O

```kotlin
// ✅ 适合用Effect
UserEffect.LoadUserFromApi(userId)     // 网络请求
UserEffect.SaveUserToDatabase(user)   // 数据库操作
```

### 🌊 4. 事件处理最佳实践

#### ✅ 正确的事件使用
```kotlin
sealed class UserEvent {
    object UserSaved : UserEvent()                    // 成功反馈
    data class ShowError(val message: String) : UserEvent()  // 错误处理
    data class NavigateToProfile(val userId: String) : UserEvent()  // 导航
}
```

#### 在UI中正确处理事件
```kotlin
// Compose中
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

### 🧪 5. 测试策略

#### Reducer测试
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

#### EffectHandler测试
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

#### ViewModel集成测试
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

## 高级用法

### 🔀 1. 组合多个Reducer

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
                // 处理全局动作
                handleGlobalAction(state, action.globalAction)
            }
        }
    }
}
```

### 🔗 2. Effect链式调用

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
                    // 1. 验证用户
                    val authResult = authService.authenticate(effect.credentials)
                    dispatch(UserAction.SetAuthToken(authResult.token))
                    
                    // 2. 加载用户资料
                    val user = userService.getUserProfile(authResult.userId)
                    dispatch(UserAction.SetUser(user))
                    
                    // 3. 加载用户设置
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

### 🎯 3. 条件Effect处理

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
                // 只有当todos发生变化时才保存
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
                // 基于状态条件决定是否自动保存
                if (state.todos.isNotEmpty() && !state.isLoading) {
                    dispatch(TodoAction.TriggerSave)
                }
            }
        }
    }
}
```

### 🕰️ 4. 状态时间旅行调试

```kotlin
class TimeTravel<State> {
    private val history = mutableListOf<State>()
    private var currentIndex = -1
    
    fun record(state: State) {
        // 移除当前位置之后的历史
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

// 在ViewModel中使用
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

### 🔄 5. 中间件模式

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
        // 记录分析数据
        analytics.track("action_dispatched", mapOf(
            "action_type" to action::class.simpleName,
            "state_type" to state::class.simpleName
        ))
        next(action)
    }
}
```

---

## 常见问题

### ❓ Q1: State应该包含哪些数据？

**A**: State应该包含UI渲染所需的所有数据，包括：
- 业务数据（用户信息、列表等）
- UI状态（loading、error、选中项等）
- 导航状态（当前页面、对话框状态等）

**不应该包含**：
- UI组件引用
- 回调函数
- 非序列化的对象

### ❓ Q2: 何时使用Effect而不是Action？

**A**: 
- **使用Action**: 同步状态更新、简单计算、不需要外部依赖
- **使用Effect**: 异步操作、网络请求、数据库操作、文件I/O

```kotlin
// Action - 同步更新
CounterAction.Increment

// Effect - 异步操作
UserEffect.LoadFromApi
```

### ❓ Q3: 如何处理复杂的异步操作？

**A**: 使用Effect链式调用或条件处理：

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

### ❓ Q4: 如何优化性能？

**A**: 
1. **使用不可变集合**
2. **避免频繁的状态更新**
3. **合理使用协程作用域**
4. **实现状态差异检查**

```kotlin
// 优化状态更新
override fun reduce(state: State, action: Action): State {
    val newData = when (action) {
        is Action.UpdateList -> {
            if (action.newList == state.list) {
                return state // 避免不必要的更新
            }
            action.newList
        }
        else -> state.list
    }
    
    return state.copy(list = newData)
}
```

### ❓ Q5: 如何处理内存泄漏？

**A**: Lucid MVI自动处理生命周期：
- 基于ViewModel作用域的协程
- 自动清理Channel和Flow
- onCleared()中释放资源

```kotlin
// 框架自动处理
override fun onCleared() {
    super.onCleared()
    actionChannel.close()
    effectChannel.close()
}
```

---

## 示例项目

### 🏠 Demo应用结构

本项目包含完整的示例应用，展示不同复杂度的MVI实现：

#### 1. HomeActivity - 导航中心
- Material 3设计
- 统一的示例入口
- 响应式布局

#### 2. CounterActivity - 基础计数器
- **技术栈**: Traditional Views + ViewBinding
- **复杂度**: ⭐
- **学习重点**: MVI基础概念

```kotlin
// 完整的计数器实现
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

#### 3. CounterComposeActivity - 现代计数器
- **技术栈**: Jetpack Compose + Material 3
- **复杂度**: ⭐⭐
- **学习重点**: Compose与MVI集成

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            // 处理事件
        }
    }
    
    // UI组件
    CounterContent(
        state = state,
        onIncrement = viewModel::increment,
        onDecrement = viewModel::decrement
    )
}
```

#### 4. TodoActivity - 复杂状态管理
- **技术栈**: Jetpack Compose + 复杂业务逻辑
- **复杂度**: ⭐⭐⭐⭐
- **学习重点**: 
  - CRUD操作
  - 列表管理
  - 过滤和搜索
  - 批量操作
  - 状态持久化

```kotlin
// 复杂的Todo状态管理
data class TodoState(
    val todos: List<TodoItem> = emptyList(),
    val filter: TodoFilter = TodoFilter.ALL,
    val isLoading: Boolean = false,
    val editingTodo: TodoItem? = null,
    val searchQuery: String = ""
)

// 丰富的业务操作
class TodoViewModel : BaseMVIViewModel<...> {
    fun addTodo(title: String, description: String = "") { ... }
    fun updateTodo(todo: TodoItem) { ... }
    fun deleteTodo(todoId: String) { ... }
    fun toggleTodo(todoId: String) { ... }
    fun setFilter(filter: TodoFilter) { ... }
    fun searchTodos(query: String) { ... }
    fun clearCompleted() { ... }
    
    // 计算属性
    fun getFilteredTodos(state: TodoState): List<TodoItem> { ... }
    fun getStats(state: TodoState): TodoStats { ... }
}
```

### 🎯 运行示例

1. **克隆项目**
   ```bash
   git clone https://github.com/greathousesh/Lucid-MVI.git
   ```

2. **打开Android Studio**
   - 导入项目
   - 同步Gradle

3. **运行应用**
   - 选择app模块
   - 运行到设备或模拟器

4. **探索示例**
   - 从首页选择不同示例
   - 体验MVI架构的威力

### 📚 学习路径建议

1. **初学者**: CounterActivity → CounterComposeActivity
2. **进阶者**: TodoActivity → 自定义实现
3. **专家级**: 研究源码 → 贡献代码

---

## 📞 支持和社区

### 🐛 问题报告
- [GitHub Issues](https://github.com/greathousesh/Lucid-MVI/issues)

### 💬 讨论和支持
- [GitHub Discussions](https://github.com/greathousesh/Lucid-MVI/discussions)

### 🤝 贡献指南
- Fork项目
- 创建feature分支
- 提交Pull Request

### 📄 许可证
Apache 2.0 License - 详见 [LICENSE](LICENSE) 文件

---

**Happy Coding with Lucid MVI! 🚀**
