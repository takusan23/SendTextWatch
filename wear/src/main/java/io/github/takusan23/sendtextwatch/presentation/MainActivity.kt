package io.github.takusan23.sendtextwatch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import io.github.takusan23.sendtextwatch.common.DeviceNodeData
import io.github.takusan23.sendtextwatch.common.SendTextTool
import io.github.takusan23.sendtextwatch.presentation.theme.SendTextWatchTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SendTextWatchTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background),
                    verticalArrangement = Arrangement.Center
                ) {
                    MessageScreen()
                }
            }
        }
    }
}

@Composable
fun MessageScreen() {
    val context = LocalContext.current
    val deviceNodeData = remember { mutableStateOf<DeviceNodeData?>(null) }
    val textList = remember { mutableStateListOf<String>() }
    val inputText = remember { mutableStateOf("Hello World") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = Unit) {
        // NodeId を取得する
        launch {
            SendTextTool.collectDeviceNodeId(context)
                .collect { deviceNodeData.value = it }
        }

        // メッセージを受信する
        launch {
            SendTextTool.receiveTest(context)
                .collect { textList.add(it) }
        }
    }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(text = deviceNodeData.value?.deviceName ?: "未接続")
        }
        item {
            BasicTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .border(1.dp, Color.White),
                value = inputText.value,
                onValueChange = { inputText.value = it },
                textStyle = TextStyle(color = Color.White, textAlign = TextAlign.Center)
            )
        }
        item {
            Button(onClick = {
                scope.launch {
                    val nodeId = deviceNodeData.value?.nodeId ?: return@launch
                    SendTextTool.sendText(context, nodeId, inputText.value)
                }
            }) { Text(text = "送信") }
        }
        items(textList) { text ->
            Text(
                modifier = Modifier.padding(10.dp),
                text = text
            )
        }
    }
}
