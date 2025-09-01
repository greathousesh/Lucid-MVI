package com.example.mvi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mvi.ui.theme.MVITheme

class CounterComposeActivity : ComponentActivity() {
    
    private val viewModel: CounterViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MVITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CounterScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // 监听事件
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is CounterEvent.CountSaved -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.count_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is CounterEvent.ShowError -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_occurred, event.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.counter_title_compose),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            )
        }
    ) { paddingValues ->
        CounterContent(
            state = state,
            onIncrement = viewModel::increment,
            onDecrement = viewModel::decrement,
            onReset = viewModel::reset,
            onSave = viewModel::saveCount,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        )
    }
}

@Composable
fun CounterContent(
    state: CounterState,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 计数显示
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = state.count.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 主要操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 减少按钮
                FloatingActionButton(
                    onClick = { if (!state.isLoading) onDecrement() },
                    containerColor = if (state.isLoading) 
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) 
                    else 
                        MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        text = "−",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isLoading) 
                            MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                        else 
                            MaterialTheme.colorScheme.onSecondary
                    )
                }
                
                // 增加按钮
                FloatingActionButton(
                    onClick = { if (!state.isLoading) onIncrement() },
                    containerColor = if (state.isLoading) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                    else 
                        MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "+",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isLoading) 
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        else 
                            MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 辅助操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.reset))
                }
                
                Button(
                    onClick = onSave,
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
        
        // 加载指示器
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CounterScreenPreview() {
    MVITheme {
        CounterContent(
            state = CounterState(count = 42, isLoading = false),
            onIncrement = {},
            onDecrement = {},
            onReset = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterScreenLoadingPreview() {
    MVITheme {
        CounterContent(
            state = CounterState(count = 42, isLoading = true),
            onIncrement = {},
            onDecrement = {},
            onReset = {},
            onSave = {}
        )
    }
}
