package telegram

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys

data class Update (
    @SerialName("update_id") val updateId: Long,
    @SerialName("message") val message: Message? = null,
    @SerialName("callback_query") val callbackQuery: CallbackQuery? = null,
)
