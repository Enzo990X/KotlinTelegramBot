package Telegram

import ktb.trainer.LearnWordsTrainer
import trainer.model.Question
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramBotService(private val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    companion object {
        private const val API_URL = "https://api.telegram.org/bot"

        const val LEARN_WORDS = "learn_words"
        const val ADD_WORD = "add_word"
        const val STATS = "stats"
        const val SETTINGS = "settings"
        const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

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

    fun sendQuestion(botToken: String, chatId: String, question: Question) {
        try {
            println("Preparing to send question for word: ${question.learningWord.original}")
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            // Create answer buttons in rows of 2
            val buttons = question.translationsToPick.mapIndexed { index, word ->
                mapOf(
                    "text" to word.translations,
                    "callback_data" to "${CALLBACK_DATA_ANSWER_PREFIX}$index"
                )
            }.chunked(2)


            // Create the keyboard structure that Telegram expects
            val keyboard = mapOf(
                "inline_keyboard" to buttons
            )

            // Manually construct the JSON to ensure proper structure
            val keyboardJson = """
            {
                "inline_keyboard": [
                    ${buttons.joinToString(",\n                    ") { row ->
                "[${row.joinToString(", ") { button ->
                    """{"text":"${button["text"]}", "callback_data":"${button["callback_data"]}"}"""
                }}]"
            }}
                ]
            }
            """.trimIndent()

            println("Generated keyboard JSON: $keyboardJson")

            val sendMessageBody = """
            {
                "chat_id": "$chatId",
                "text": "Выберите перевод слова *${question.learningWord.original}*:",
                "parse_mode": "Markdown",
                "reply_markup": $keyboardJson
            }
            """.trimIndent()

            println("Sending request to Telegram API...")
            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sendMessageBody))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            println("Telegram API response status: ${response.statusCode()}")
            println("Telegram API response body: ${response.body()}")
        } catch (e: Exception) {
            println("Failed to send question: ${e.message}")
            e.printStackTrace()
            sendMessage(botToken, chatId, "Произошла ошибка при загрузке вопроса. Пожалуйста, попробуйте ещё раз.")
        }
    }



    // Helper extension function to convert Map to JSON string
    private fun Map<*, *>.toJsonString(): String {
        return entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ","
        ) { (key, value) ->
            "\"$key\": ${
                when (value) {
                    is String -> "\"${value.escapeJson()}\""
                    is Map<*, *> -> value.toJsonString()
                    is List<*> -> value.joinToString(
                        prefix = "[",
                        postfix = "]",
                        separator = ","
                    ) { item ->
                        when (item) {
                            is Map<*, *> -> item.toJsonString()
                            else -> "\"${item.toString().escapeJson()}\""
                        }
                    }
                    else -> value
                }
            }"
        }
    }

    // Helper function to escape special JSON characters
    private fun String.escapeJson(): String {
        val result = StringBuilder()
        for (char in this) {
            when (char) {
                '\\' -> result.append("\\\\")
                '"' -> result.append("\\\"")
                '\b' -> result.append("\\\\b")
                '\u000C' -> result.append("\\\\f")  // Form feed
                '\n' -> result.append("\\\\n")
                '\r' -> result.append("\\\\r")
                '\t' -> result.append("\\\\t")
                else -> result.append(char)
            }
        }
        return result.toString()
    }



    fun showStats(trainer: LearnWordsTrainer, chatId: String) {
        try {
            val statistics = trainer.getStatistics()
            val message = """
                | Ваша статистика
                |
                | Выучено: ${statistics.learnedWords} из ${statistics.wordsInFile} слов
                | Прогресс: ${statistics.progressPercentage}%
            """.trimMargin()

            sendMessage(botToken, chatId, message)
        } catch (e: Exception) {
            println("Failed to show statistics: ${e.message}")
        }
    }
}
