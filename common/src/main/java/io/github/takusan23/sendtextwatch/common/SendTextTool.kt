package io.github.takusan23.sendtextwatch.common

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object SendTextTool {

    private const val TRANSCRIPTION_CAPABILITY_NAME = "text_transcription"

    /** [TRANSCRIPTION_CAPABILITY_NAME]をサポートしている WearOS デバイスを見つける */
    fun collectDeviceNodeId(context: Context) = callbackFlow {

        fun sendResult(capabilityInfo: CapabilityInfo) {
            val nodes = capabilityInfo.nodes
            val findNode = (nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()) ?: return
            trySend(DeviceNodeData(findNode.id, findNode.displayName))
        }

        val client = Wearable.getCapabilityClient(context)

        // addListener は変化したときしか呼ばれない
        // 初回時はこれ
        withContext(Dispatchers.IO) {
            Tasks.await(client.getCapability(TRANSCRIPTION_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE))
        }.also { capabilityInfo ->
            println(capabilityInfo.nodes)
            sendResult(capabilityInfo)
        }

        // 後で接続された場合
        val listener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            sendResult(capabilityInfo)
        }
        client.addListener(listener, TRANSCRIPTION_CAPABILITY_NAME)
        awaitClose { client.removeListener(listener) }
    }

    /**
     * テキストを送信する
     *
     * @param context [Context]
     * @param nodeId [collectDeviceNodeId]で取得した送信先 Node ID
     * @param text テキスト
     * @return 失敗したら例外
     */
    suspend fun sendText(context: Context, nodeId: String, text: String) = suspendCancellableCoroutine { continuation ->
        Wearable.getMessageClient(context).sendMessage(
            nodeId,
            TRANSCRIPTION_CAPABILITY_NAME,
            text.toByteArray(charset = Charsets.UTF_8)
        ).apply {
            addOnSuccessListener { continuation.resume(Unit) }
            addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    /**
     * メッセージを Flow で受信する
     *
     * @param context [Context]
     */
    fun receiveTest(context: Context) = callbackFlow {
        val client = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener {
            trySend(it.data.toString(charset = Charsets.UTF_8))
        }

        client.addListener(listener)
        awaitClose { client.removeListener(listener) }
    }

}