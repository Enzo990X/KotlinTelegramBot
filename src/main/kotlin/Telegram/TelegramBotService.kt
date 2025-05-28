package Telegram

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramBotService(val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()

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
            val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId&timeout=30"
            val client = HttpClient.newBuilder().build()
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

const val API_URL = "https://api.telegram.org/bot"

