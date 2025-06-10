package telegram

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys

data class Message(
    @SerialName("message_id") val messageId: Long,
    @SerialName("text") val text: String? = null,
    @SerialName("chat") val chat: Chat
)
