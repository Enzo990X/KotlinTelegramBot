package telegram

import trainer.LearnWordsTrainer
import trainer.model.Statistics
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramBotService(
    private val botToken: String,
) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    companion object {
        private const val API_URL = "https://api.telegram.org/bot"
    }

    fun sendMenu(chatId: String) {

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

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send menu: ${e.message}")
        }
    }

    fun sendMessage(chatId: String, message: String) {

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

    fun getUpdates(updateId: Int): String {

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

    fun checkNextQuestionAndSend(trainer: LearnWordsTrainer, chatId: String, onComplete: () -> Unit = {}) {

        val question = trainer.getNextQuestion()
        trainer.question = question

        if (question == null) {
            sendMessage(chatId, "Неизученный материал отсутствует. Добавьте новый.")
            onComplete()
            return
        }

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"
            val sendMessageBody = """
        {
            "chat_id": "$chatId",
            "text": "Выберите перевод для ${question.learningWord.original}",
            "reply_markup": {
                "inline_keyboard": [
                    [
                        {"text": "${question.translationsToPick[TRANSLATION_INDEX_ONE].translation}", "callback_data": "TRANSLATION_INDEX_ONE"},
                        {"text": "${question.translationsToPick[TRANSLATION_INDEX_TWO].translation}", "callback_data": "TRANSLATION_INDEX_TWO"}
                    ],
                    [
                        {"text": "${question.translationsToPick[TRANSLATION_INDEX_THREE].translation}", "callback_data": "TRANSLATION_INDEX_THREE"},
                        {"text": "${question.translationsToPick[TRANSLATION_INDEX_FOUR].translation}", "callback_data": "TRANSLATION_INDEX_FOUR"}
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
            println("Failed to send question: ${e.message}")
            onComplete()
        }
    }

    fun showStats(chatId: String, statistics: Statistics) {

        try {

            val message = """
            | <b>Статистика</b>:
            | 
            |В Вашем словаре ${statistics.wordsInFile} элементов
            |Вы точно запомнили ${statistics.learnedWords}
            |Вы прошли ${statistics.progressPercentage}% пути
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
            println("Error getting statistics: ${e.message}")
            sendMessage(chatId, "Произошла ошибка при получении статистики.")
        }
    }

    fun sendSettingsMenu(chatId: String) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"
            val sendMessageBody = """
        {
            "chat_id": "$chatId",
            "text": "Настройки",
            "reply_markup": {
                "inline_keyboard": [
                    [{"text": "Размер тренировки", "callback_data": "$CHANGE_NUMBER_OF_ITERATIONS"}],
                    [{"text": "Тип тренировки", "callback_data": "$CHANGE_TYPE_OF_TRAIN"}],
                    [{"text": "Вернуться в меню", "callback_data": "$START"}]
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
            println("Failed to send settings menu: ${e.message}")
        }
    }

    fun sendIterationsSettingMenu(chatId: String, trainer: LearnWordsTrainer) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"
            val sendMessageBody = """
        {
            "chat_id": "$chatId",
            "text": "Введите размер одной тренировки.
            |Текущее значение: ${trainer.settings.numberOfIterations}",
            "reply_markup": {
                "inline_keyboard": [
                    [{"text": "Назад", "callback_data": "$SETTINGS"}]
                ]
            }
        }
        """.trimMargin()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sendMessageBody))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())

        } catch (e: Exception) {
            println("Failed to send change number of iterations menu: ${e.message}")
        }
    }

    fun handleIterationsSettingCallback(chatId: String, messageText: String, trainer: LearnWordsTrainer) {
        try {
            val numberOfIterations = messageText.trim().toIntOrNull()

            if (numberOfIterations == null || numberOfIterations <= WRONG_NUMBER_OF_TRAINS) {
                sendMessage(chatId, "Пожалуйста, введите число больше 0")
            } else {
                trainer.settings.numberOfIterations = numberOfIterations
                trainer.settings.saveSettings()
                sendMessage(chatId, "Количество итераций изменено на $numberOfIterations")
                sendSettingsMenu(chatId)
            }

        } catch (e: Exception) {
            println("Error handling iterations setting: ${e.message}")
            sendMessage(chatId, "Произошла ошибка при обработке запроса")
        }
    }

    fun sendFilterSettingMenu(chatId: String, trainer: LearnWordsTrainer) {
        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"
            val sendMessageBody = """
        {
            "chat_id": "$chatId",
            "text": "Выберите тип тренировки.
            |Текущее значение: ${trainer.settings.filter}",
            "reply_markup": {
                "inline_keyboard": [
                    [
                    |{"text": "Слова", "callback_data": "$FILTER_WORD"},
                    {"text": "Выражения", "callback_data": "$FILTER_EXPRESSION"}
                    ],
                    [
                    {"text": "Словосочетания", "callback_data": "$FILTER_WORD_PAIR"},
                    {"text": "Всё", "callback_data": "$FILTER_ALL$"}
                    ],
                    [{"text": "Назад", "callback_data": "$SETTINGS"}]
                ]
            }
        }
        """.trimMargin()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sendMessageBody))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send filter menu: ${e.message}")
        }
    }

    fun handleFilterSettingCallback(chatId: String, callbackData: String, trainer: LearnWordsTrainer) {
        try {
            val (filter, displayName) = when (callbackData) {
                FILTER_WORD -> "word" to "слова"
                FILTER_WORD_PAIR -> "word_pair" to "словосочетания"
                FILTER_EXPRESSION -> "expression" to "выражения"
                FILTER_ALL -> "all" to "всё"
                else -> throw IllegalArgumentException("Invalid callback data: $callbackData")
            }

            trainer.settings.filter = displayName
            trainer.settings.saveSettings()
            sendMessage(chatId, "Тип тренировки изменён на $displayName")
            sendSettingsMenu(chatId)

        } catch (e: Exception) {
            println("Error handling filter setting: ${e.message}")
            sendMessage(chatId, "Произошла ошибка при изменении типа тренировки")
        }
    }
}

const val CHANGE_NUMBER_OF_ITERATIONS = "change_number_of_iterations"
const val CHANGE_TYPE_OF_TRAIN = "change_type_of_train"

const val FILTER_WORD = "filter_word"
const val FILTER_WORD_PAIR = "filter_word_pair"
const val FILTER_EXPRESSION = "filter_expression"
const val FILTER_ALL = "filter_all"

const val WRONG_NUMBER_OF_TRAINS = 0

const val TRANSLATION_INDEX_ONE = 0
const val TRANSLATION_INDEX_TWO = 1
const val TRANSLATION_INDEX_THREE = 2
const val TRANSLATION_INDEX_FOUR = 3
