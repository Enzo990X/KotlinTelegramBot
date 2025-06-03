package telegram

import trainer.LearnWordsTrainer
import trainer.model.Dictionary
import trainer.model.Word
import trainer.model.START_CORRECT_ANSWERS_COUNT
import trainer.model.START_USAGE_COUNT
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
                        {"text": "Пополнить словарь", "callback_data": "$ADD_WORD"}
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

    fun checkNextQuestionAndSend(
        trainer: LearnWordsTrainer, chatId: String, trainingState: TrainState, onComplete: () -> Unit = {}) {

        if (trainingState.questionsRemaining <= NO_QUESTIONS_LEFT) {
            onComplete()
            return
        }

        val question = trainer.getNextQuestion()
        trainer.question = question

        if (question == null) {
            sendMessage(chatId, "Неизученный материал отсутствует. Добавьте новый.")
            onComplete()
            return
        }

        trainingState.currentQuestion = question
        trainingState.questionsRemaining--

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val keyboardButtons = question.translationsToPick.mapIndexed { index, word ->
                """{"text": "${word.translation}", "callback_data": "$index"}"""
            }.chunked(COLUMNS)
                .joinToString(",\n") { row ->
                    "[${row.joinToString(",")}]"
                }

            val sendMessageBody = """
        {
            "chat_id": "$chatId",
            "text": "Выберите перевод для ${question.learningWord.original}",
            "reply_markup": {
                "inline_keyboard": [
                    $keyboardButtons
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

    fun sendTypeOfWordMenu(chatId: String) {

        val urlSendMessage = "$API_URL$botToken/sendMessage"

        val keyboardButtons = """
        [
            [{"text": "Слово (1 слово)", "callback_data": "$TYPE_WORD"}],
            [{"text": "Словосочетание (2 слова)", "callback_data": "$TYPE_WORD_PAIR"}],
            [{"text": "Выражение (3+ слова)", "callback_data": "$TYPE_EXPRESSION"}],
            [{"text": "Вернуться в меню", "callback_data": "$START"}]
                ]
            }
        }
        """.trimIndent()

        val sendMessageBody = """
        {
            "chat_id": "$chatId",
            "text": "Выберите тип слова для добавления:",
            "reply_markup": {
                "inline_keyboard": $keyboardButtons
            }
        }
        """.trimIndent()

        sendApiRequest(urlSendMessage, sendMessageBody)
        userStates[chatId] = AddWordState.AWAITING_WORD_TYPE
    }

    fun handleWordTypeSelection(chatId: String, type: String) {

        val wordType = when (type) {
            TYPE_WORD -> "слово"
            TYPE_WORD_PAIR -> "словосочетание"
            TYPE_EXPRESSION -> "выражение"
            else -> return
        }

        userWordData[chatId] = Word("", "", wordType)
        userStates[chatId] = AddWordState.AWAITING_ORIGINAL

        sendMessage(chatId, getWordTypeRequirements(wordType))
    }

    fun handleOriginalWord(chatId: String, text: String): Boolean {

        val wordData = userWordData[chatId] ?: return false

        if (!Dictionary().isWordValid(text, wordData.type)) {
            sendMessage(chatId, "Некорректный формат. ${getWordTypeRequirements(wordData.type)}")
            return false
        }

        wordData.original = text.lowercase()
        userStates[chatId] = AddWordState.AWAITING_TRANSLATION
        sendMessage(chatId, "Введите перевод на русский:")
        return true
    }

    fun handleTranslation(chatId: String, text: String): Boolean {

        val wordData = userWordData[chatId] ?: return false

        val unicodeRegex = "\\\\u([0-9a-fA-F]{$UNICODE_DIGITS_IN_CHAR})".toRegex()
        val cleanText = unicodeRegex.replace(text) {
            it.groupValues[UNICODE_HEX_GROUP_INDEX].toInt(RADIX_HEXADECIMAL).toChar().toString()
        }.trim()

        if (!Dictionary().isTranslationValid(cleanText)) {
            sendMessage(chatId, "Некорректный перевод. Используйте только кириллицу. Введите перевод:")
            return false
        }

        wordData.translation = cleanText.lowercase()

        val word =
            Word(wordData.original, wordData.translation, wordData.type, START_CORRECT_ANSWERS_COUNT, START_USAGE_COUNT)

        Dictionary().addWordToDictionaryByBot(word)

        sendMessage(chatId, "Успешно выполнено.")
        resetUserState(chatId)
        sendMenu(chatId)
        return true
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
                sendMessage(chatId, "Количество слов в одной тренировке изменено на $numberOfIterations")
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

    private fun sendApiRequest(url: String, requestBody: String) {

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send API request: ${e.message}")
        }
    }
}

const val CHANGE_NUMBER_OF_ITERATIONS = "change_number_of_iterations"
const val CHANGE_TYPE_OF_TRAIN = "change_type_of_train"

const val TYPE_WORD = "type_word"
const val TYPE_WORD_PAIR = "type_word_pair"
const val TYPE_EXPRESSION = "type_expression"
const val TYPE_ALL = "type_all"

const val FILTER_WORD = "filter_word"
const val FILTER_WORD_PAIR = "filter_word_pair"
const val FILTER_EXPRESSION = "filter_expression"
const val FILTER_ALL = "filter_all"

const val WRONG_NUMBER_OF_TRAINS = 0
const val NO_QUESTIONS_LEFT = 0

const val COLUMNS = 2
const val UNICODE_DIGITS_IN_CHAR = 4
const val UNICODE_HEX_GROUP_INDEX = 1
const val RADIX_HEXADECIMAL = 16
