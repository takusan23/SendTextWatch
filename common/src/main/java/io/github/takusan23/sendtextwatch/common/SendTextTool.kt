package io.github.takusan23.sendtextwatch.common

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object SendTextTool {

    // wear.xml の値。Android / WearOS 側で違う値にする必要がある模様

    /** WearOS 側アプリの wear.xml の値 */
    const val WEAR_TRANSCRIPTION_CAPABILITY_NAME = "wear_text_transcription"

    /** Android 側アプリの wear.xml の値 */
    const val PHONE_TRANSCRIPTION_CAPABILITY_NAME = "phone_text_transcription"

    /**
     * WearOS / Android デバイスを見つける
     *
     * @param context [Context]
     * @param capability [WEAR_TRANSCRIPTION_CAPABILITY_NAME] [PHONE_TRANSCRIPTION_CAPABILITY_NAME]
     */
    fun collectDeviceNodeId(
        context: Context,
        capability: String
    ) = callbackFlow {

        fun sendResult(nodes: Set<Node>) {
            val findNode = (nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()) ?: return
            trySend(DeviceNodeData(findNode.id, findNode.displayName))
        }

        val client = Wearable.getCapabilityClient(context)

        // addListener は変化したときしか呼ばれない
        withContext(Dispatchers.IO) {
            // 初回時はこれ
            Tasks.await(client.getCapability(capability, CapabilityClient.FILTER_REACHABLE)).nodes
                // 取れない場合は接続済みノードを返す、こっちも試す。WearOS -> Android がどうしても取れない
                .ifEmpty { Tasks.await(Wearable.getNodeClient(context).connectedNodes) }
        }.also { nodes ->
            sendResult(nodes.toSet())
        }


        // 後で接続された場合
        val listener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            sendResult(capabilityInfo.nodes)
        }
        client.addListener(listener, capability)
        awaitClose { client.removeListener(listener) }
    }

    /**
     * テキストを送信する
     *
     * @param context [Context]
     * @param nodeId [collectDeviceNodeId]で取得した送信先 Node ID
     * @param capability 送り先 [WEAR_TRANSCRIPTION_CAPABILITY_NAME] [PHONE_TRANSCRIPTION_CAPABILITY_NAME]
     * @param text テキスト
     * @return 失敗したら例外
     */
    suspend fun sendText(
        context: Context,
        nodeId: String,
        capability: String,
        text: String
    ) = suspendCancellableCoroutine { continuation ->
        Wearable.getMessageClient(context).sendMessage(
            nodeId,
            capability,
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