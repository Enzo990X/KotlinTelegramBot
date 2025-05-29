package Telegram

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramBotService(val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    companion object {
        private const val API_URL = "https://api.telegram.org/bot"
    }

    fun sendMenu(botToken: String, chatId: String) {
        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val sendMessageBody = """
        {
            "chat_id": "$chatId",
            "text": "Меню",
            "reply_markup": {
                "inline_keyboard": [
                    [
                        {"text": "Учить слова", "callback_data": "learn_words"},
                        {"text": "Добавить слово", "callback_data": "add_word"}
                    ],
                    [
                        {"text": "Статистика", "callback_data": "stats"},
                        {"text": "Настройки", "callback_data": "settings"}
                    ]
                ]
            }
        }
        """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sendMessageBody))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send message: ${e.message}")
        }
    }

    fun sendMessage(botToken: String, chatId: String, message: String) {

        try {
            val encodedMessage = message.replace(" ", "%20")
            val urlSendMessage =
                "$API_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedMessage"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send message: ${e.message}")
        }
    }

    fun getUpdates(botToken: String, updateId: Int): String {

        return try {
            val urlGetUpdates = "$API_URL$botToken/getUpdates?offset=$updateId&timeout=30"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlGetUpdates))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            response.body()
        } catch (e: Exception) {
            println("Error getting updates: ${e.message}")
            ""
        }
    }
}
