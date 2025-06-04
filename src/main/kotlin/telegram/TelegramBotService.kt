package telegram

import kotlinx.serialization.json.Json
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

    fun sendMenu(json: Json, chatId: Long) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId, "Меню",
                ReplyMarkup(
                    listOf(
                        listOf(
                            InlineKeyboard(LEARN_WORDS, "Учить слова"),
                            InlineKeyboard(ADD_WORD, "Пополнить словарь")
                        ),
                        listOf(
                            InlineKeyboard(STATS, "Статистика"),
                            InlineKeyboard(SETTINGS, "Настройки")
                        )
                    )
                )
            )

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send menu: ${e.message}")
        }
    }

    fun sendMessage(json: Json, chatId: Long, message: String) {

        try {
            val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
            val urlSendMessage =
                "$API_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedMessage"

            val requestBody = SendMessageRequest(chatId, message)

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send message: ${e.message}")
        }
    }

    fun getUpdates(updateId: Long): String {

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

    fun checkNextQuestionAndSend(json: Json, chatId: Long, context: TrainContext) {
        if (context.trainingState.questionsRemaining <= NO_QUESTIONS_LEFT) {
            context.trainingState.completeTraining()
            return
        }

        val question = context.trainer.getNextQuestion()
        context.trainer.question = question

        if (question == null) {
            sendMessage(json, chatId, "Неизученный материал отсутствует. Добавьте новый.")
            context.trainingState.completeTraining()
            return
        }

        context.trainingState.startNewQuestion(question, context.onComplete)

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId, "Выберите перевод для ${question.learningWord.original}",
                ReplyMarkup(
                    question.translationsToPick.mapIndexed { index, word ->
                        InlineKeyboard(CALLBACK_DATA_ANSWER_PREFIX + index, word.translation)
                    }.chunked(COLUMNS)
                )
            )

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())

        } catch (e: Exception) {
            println("Failed to send question: ${e.message}")
            context.trainingState.completeTraining()
        }
    }

    fun handleAnswer(chatId: Long, callbackData: String, context: TrainContext) {

        with(context) {
            if (!trainingState.isWaitingForAnswer) {
                return
            }

            val answerIndex = callbackData.removePrefix(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()
            val currentQuestion = trainingState.currentQuestion

            if (currentQuestion == null || answerIndex == null) {
                sendMessage(json, chatId, "Произошла ошибка. Пожалуйста, начните тренировку заново.")
                trainingState.completeTraining()
                return
            }

            val isCorrect = trainer.checkAnswer(answerIndex)
            trainingState.handleAnswerProcessed()

            val correctAnswer = currentQuestion.learningWord.translation
            val message = if (isCorrect) {
                "Правильно!"
            } else {
                "Неверно. Правильный ответ: \"$correctAnswer\"."
            }

            sendMessage(json, chatId, message)

            if (trainingState.questionsRemaining > NO_QUESTIONS_LEFT) {
                checkNextQuestionAndSend(json, chatId, context)
            } else {
                trainingState.completeTraining()
            }
        }
    }

    fun sendTypeOfWordMenu(json: Json, chatId: Long) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId,
                "Выберите тип слова для добавления:",
                ReplyMarkup(
                    listOf(
                        listOf(InlineKeyboard(TYPE_WORD, "Слово (1 слово)")),
                        listOf(InlineKeyboard(TYPE_WORD_PAIR, "Словосочетание (2 слова)")),
                        listOf(InlineKeyboard(TYPE_EXPRESSION, "Выражение (3+ слова)")),
                        listOf(InlineKeyboard(START, "Вернуться в меню"))
                    )
                )
            )

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
            userStates[chatId] = AddWordState.AWAITING_WORD_TYPE
        } catch (e: Exception) {
            println("Failed to send word type menu: ${e.message}")
        }
    }

    fun handleWordTypeSelection(json: Json, chatId: Long, type: String) {

        val wordType = when (type) {
            TYPE_WORD -> "слово"
            TYPE_WORD_PAIR -> "словосочетание"
            TYPE_EXPRESSION -> "выражение"
            else -> return
        }

        userWordData[chatId] = Word("", "", wordType)
        userStates[chatId] = AddWordState.AWAITING_ORIGINAL

        sendMessage(json, chatId, getWordTypeRequirements(wordType))
    }

    fun handleOriginalWord(json: Json, chatId: Long, text: String): Boolean {

        val wordData = userWordData[chatId] ?: return false

        if (Dictionary().isWordInDictionary(text)) {
            sendMessage(json, chatId, "Это слово уже есть в словаре. Пожалуйста, введите другое слово.")
            return false
        }

        if (!Dictionary().isWordValid(text, wordData.type)) {
            sendMessage(json, chatId, "Некорректный формат. ${getWordTypeRequirements(wordData.type)}")
            return false
        }

        wordData.original = text.lowercase()
        userStates[chatId] = AddWordState.AWAITING_TRANSLATION
        sendMessage(json, chatId, "Введите перевод на русский:")
        return true
    }

    fun handleTranslation(json: Json, chatId: Long, text: String): Boolean {

        val wordData = userWordData[chatId] ?: return false

        val unicodeRegex = "\\\\u([0-9a-fA-F]{$UNICODE_DIGITS_IN_CHAR})".toRegex()
        val cleanText = unicodeRegex.replace(text) {
            it.groupValues[UNICODE_HEX_GROUP_INDEX].toInt(RADIX_HEXADECIMAL).toChar().toString()
        }.trim()

        if (!Dictionary().isTranslationValid(cleanText)) {
            sendMessage(json, chatId, "Некорректный перевод. Используйте только кириллицу. Введите перевод:")
            return false
        }

        wordData.translation = cleanText.lowercase()

        val word =
            Word(wordData.original, wordData.translation, wordData.type, START_CORRECT_ANSWERS_COUNT, START_USAGE_COUNT)

        Dictionary().addWordToDictionaryByBot(word)

        sendMessage(json, chatId, "Успешно выполнено.")
        resetUserState(chatId)
        sendMenu(json, chatId)

        return true
    }

    fun showStats(json: Json, chatId: Long, statistics: Statistics) {

        try {

            val message = """
            Статистика:
            
            В Вашем словаре ${statistics.wordsInFile} элементов
            Вы точно запомнили ${statistics.learnedWords}
            Вы прошли ${statistics.progressPercentage}% пути
        """.trimIndent()

            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId,
                message,
                ReplyMarkup(
                    listOf(
                        listOf(InlineKeyboard(START, "Вернуться в меню"))
                    )
                )
            )

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Error getting statistics: ${e.message}")
            sendMessage(json, chatId, "Произошла ошибка при получении статистики.")
        }
    }

    fun sendSettingsMenu(json: Json, chatId: Long) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId,
                "Настройки",
                ReplyMarkup(
                    listOf(
                        listOf(InlineKeyboard(CHANGE_NUMBER_OF_ITERATIONS, "Размер тренировки")),
                        listOf(InlineKeyboard(CHANGE_TYPE_OF_TRAIN, "Тип тренировки")),
                        listOf(InlineKeyboard(START, "Вернуться в меню"))
                    )
                )
            )

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send settings menu: ${e.message}")
        }
    }

    fun sendIterationsSettingMenu(json: Json, chatId: Long, trainer: LearnWordsTrainer) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId,
                "Введите размер одной тренировки.\nТекущее значение: ${trainer.settings.numberOfIterations}",
                ReplyMarkup(
                    listOf(
                        listOf(InlineKeyboard(SETTINGS, "Назад"))
                    )
                )
            )

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())

        } catch (e: Exception) {
            println("Failed to send change number of iterations menu: ${e.message}")
        }
    }

    fun handleIterationsSettingCallback(json: Json, chatId: Long, messageText: String, trainer: LearnWordsTrainer) {

        try {
            val numberOfIterations = messageText.trim().toIntOrNull()

            if (numberOfIterations == null || numberOfIterations <= WRONG_NUMBER_OF_TRAINS) {
                sendMessage(json, chatId, "Пожалуйста, введите число больше 0")
            } else {
                trainer.settings.numberOfIterations = numberOfIterations
                trainer.settings.saveSettings()
                sendMessage(json, chatId, "Количество слов в одной тренировке изменено на $numberOfIterations")
                sendSettingsMenu(json, chatId)
            }

        } catch (e: Exception) {
            println("Error handling iterations setting: ${e.message}")
            sendMessage(json, chatId, "Произошла ошибка при обработке запроса")
        }
    }

    fun sendFilterSettingMenu(json: Json, chatId: Long, trainer: LearnWordsTrainer) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId,
                "Выберите тип тренировки.\nТекущее значение: ${trainer.settings.filter}",
                ReplyMarkup(
                    listOf(
                        listOf(
                            InlineKeyboard(FILTER_WORD, "Слова"),
                            InlineKeyboard(FILTER_EXPRESSION, "Выражения")
                        ),
                        listOf(
                            InlineKeyboard(FILTER_WORD_PAIR, "Словосочетания"),
                            InlineKeyboard(FILTER_ALL, "Всё")
                        ),
                        listOf(InlineKeyboard(SETTINGS, "Назад"))
                    )
                )
            )

            val requestBodyString = json.encodeToString(requestBody)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendMessage))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Failed to send filter menu: ${e.message}")
        }
    }

    fun handleFilterSettingCallback(json: Json, chatId: Long, callbackData: String, trainer: LearnWordsTrainer) {

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
            sendMessage(json, chatId, "Тип тренировки изменён на $displayName")
            sendSettingsMenu(json, chatId)

        } catch (e: Exception) {
            println("Error handling filter setting: ${e.message}")
            sendMessage(json, chatId, "Произошла ошибка при изменении типа тренировки")
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
