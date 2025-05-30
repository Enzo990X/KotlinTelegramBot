package Telegram

import ktb.trainer.NUMBER_OF_CORRECT_ANSWERS
import ktb.trainer.PERCENTAGE
import trainer.model.Statistics
import trainer.model.Word
import trainer.model.Dictionary
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramBotService(private val botToken: String, private val dictionary: Dictionary) {

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
                        {"text": "Учить слова", "callback_data": "$LEARN_WORDS"},
                        {"text": "Добавить слово", "callback_data": "$ADD_WORD"}
                    ],
                    [
                        {"text": "Статистика", "callback_data": "$STATS"},
                        {"text": "Настройки", "callback_data": "$SETTINGS"}
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
            val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
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

    private fun getStatistics(): Statistics {

        val currentDictionary: List<Word> = dictionary.loadDictionary()

        val learnedWords = (currentDictionary.filter { it.correctAnswersCount >= NUMBER_OF_CORRECT_ANSWERS }).size
        val wordsInFile = currentDictionary.size
        val progressPercentage = (learnedWords.toFloat() / wordsInFile * PERCENTAGE).toInt()

        return Statistics(learnedWords, wordsInFile, progressPercentage)
    }

    fun showStats(chatId: String) {
        try {
            val statistics = getStatistics()
            val message = """
            | <b>Статистика</b>:
            |Выучено: ${statistics.learnedWords} из ${statistics.wordsInFile} слов
            |Прогресс: ${statistics.progressPercentage}%
        """.trimMargin()

            val urlSendMessage = "$API_URL$botToken/sendMessage"
            val sendMessageBody = """
        {
            "chat_id": "$chatId",
            "text": "$message",
            "parse_mode": "HTML",
            "reply_markup": {
                "inline_keyboard": [
                    [
                        {"text": "Вернуться в меню", "callback_data": "$START"}
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

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            sendMessage(botToken, chatId, "Произошла ошибка при получении статистики")
        }
    }

}
