package telegram

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys

data class ReplyMarkup (
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>)