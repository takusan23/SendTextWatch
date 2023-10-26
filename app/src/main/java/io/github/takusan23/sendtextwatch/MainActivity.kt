package io.github.takusan23.sendtextwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.takusan23.sendtextwatch.common.DeviceNodeData
import io.github.takusan23.sendtextwatch.common.SendTextTool
import io.github.takusan23.sendtextwatch.ui.theme.SendTextWatchTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SendTextWatchTheme {
                MessageScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen() {
    val context = LocalContext.current
    val deviceNodeData = remember { mutableStateOf<DeviceNodeData?>(null) }
    val textList = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val inputText = remember { mutableStateOf("") }

    LaunchedEffect(key1 = Unit) {
        // NodeId を取得する
        launch {
            SendTextTool.collectDeviceNodeId(context, SendTextTool.WEAR_TRANSCRIPTION_CAPABILITY_NAME)
                .collect { deviceNodeData.value = it }
        }

        // メッセージを受信する
        launch {
            SendTextTool.receiveTest(context)
                .collect { textList.add(it) }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "SendTextWatch") }) }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(text = deviceNodeData.value?.deviceName ?: "未接続")

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(textList) { text ->
                    Text(
                        modifier = Modifier.padding(10.dp),
                        text = text
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = inputText.value,
                    onValueChange = { inputText.value = it }
                )
                Button(onClick = {
                    scope.launch {
                        val nodeId = deviceNodeData.value?.nodeId ?: return@launch
                        SendTextTool.sendText(
                            context = context,
                            nodeId = nodeId,
                            capability = SendTextTool.WEAR_TRANSCRIPTION_CAPABILITY_NAME,
                            text = inputText.value
                        )
                    }
                }) { Text(text = "送信") }
            }
        }
    }
}