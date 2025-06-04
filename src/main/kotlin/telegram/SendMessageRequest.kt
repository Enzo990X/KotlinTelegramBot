package telegram

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys

data class SendMessageRequest (
    @SerialName("chat_id") val chatId: Long,
    @SerialName("text") val text: String,
    @SerialName("reply_markup") val replyMarkup: ReplyMarkup? = null,
)
