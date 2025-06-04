package telegram

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys

data class InlineKeyboard(
    @SerialName("callback_data") val callbackData: String,
    @SerialName("text") val text: String,
)
