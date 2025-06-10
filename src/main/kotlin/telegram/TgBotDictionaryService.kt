package telegram

import kotlinx.serialization.json.Json
import trainer.COLLOCATION_SIZE
import trainer.EXPRESSION_SIZE
import trainer.LearnWordsTrainer
import trainer.WORD_SIZE
import trainer.model.Dictionary
import trainer.model.Word
import trainer.model.START_CORRECT_ANSWERS_COUNT
import trainer.model.START_USAGE_COUNT
import trainer.model.Statistics
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


class TgBotDictionaryService(private val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()
    private val json: Json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val API_URL = "https://api.telegram.org/bot"
    }

    fun checkNextQuestionAndSend(
        chatId: Long,
        trainer: LearnWordsTrainer,
        trainingState: TrainState,
        onComplete: () -> Unit
    ) {

        val question = trainer.getNextQuestion()
        trainer.question = question

        if (question == null) {
            TgBotMainService(botToken).sendMessage(chatId, "Неизученный материал отсутствует. Добавьте новый.")
            trainingState.completeTraining()
            return
        }

        trainingState.startNewQuestion(question, onComplete)

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"
            val requestBody = SendMessageRequest(
                chatId,
                "Выберите перевод для ${question.learningWord.original}",
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
            trainingState.completeTraining()
        }
    }

    fun handleAnswer(
        chatId: Long,
        callbackData: String,
        trainer: LearnWordsTrainer,
        trainingState: TrainState,
        onComplete: () -> Unit
    ) {

        if (!trainingState.isWaitingForAnswer) {
            return
        }

        val answerIndex = callbackData.removePrefix(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()
        val currentQuestion = trainingState.currentQuestion

        if (currentQuestion == null || answerIndex == null) {
            TgBotMainService(botToken).sendMessage(
                chatId,
                "Произошла ошибка. Пожалуйста, начните тренировку заново."
            )
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

        TgBotMainService(botToken).sendMessage(chatId, message)

        if (trainingState.questionsRemaining > NO_QUESTIONS_LEFT) {
            checkNextQuestionAndSend(chatId, trainer, trainingState, onComplete)
        } else {
            trainingState.completeTraining()
        }
    }


    fun sendTypeOfWordMenu(chatId: Long) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId,
                "Выбери тип слова для добавления:",
                ReplyMarkup(
                    listOf(
                        listOf(InlineKeyboard(TYPE_WORD, "Слово (1 слово)")),
                        listOf(InlineKeyboard(TYPE_WORD_PAIR, "Словосочетание (2 слова)")),
                        listOf(InlineKeyboard(TYPE_EXPRESSION, "Выражение (3+ слова)")),
                        listOf(InlineKeyboard(MENU, "Вернуться в меню"))
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

    fun handleWordTypeSelection(chatId: Long, type: String) {

        val wordType = when (type) {
            TYPE_WORD -> "слово"
            TYPE_WORD_PAIR -> "словосочетание"
            TYPE_EXPRESSION -> "выражение"
            else -> "слово"
        }

        userWordData[chatId] = Word("", "", wordType, START_CORRECT_ANSWERS_COUNT, START_USAGE_COUNT)
        userStates[chatId] = AddWordState.AWAITING_ORIGINAL
        TgBotMainService(botToken).sendMessage(chatId, "Введи $wordType на иностранном языке")
    }

    fun handleOriginalWord(chatId: Long, original: String, dictionary: Dictionary) {
        val wordData = userWordData[chatId] ?: run {
            TgBotMainService(botToken).sendMessage(chatId, "Пожалуйста, сначала выбери тип слова")
            return
        }

        val wordCount = original.split(" ").size
        val countError = when (wordData.type) {
            "слово" -> if (wordCount != WORD_SIZE) "Слово должно состоять из 1 слова." else null
            "словосочетание" -> if (wordCount != COLLOCATION_SIZE) "Словосочетание должно состоять из 2 слов." else null
            "выражение" -> if (wordCount < EXPRESSION_SIZE) "Выражение должно содержать 3 или более слов." else null
            else -> "Некорректный тип слова."
        }
        countError?.let {
            TgBotMainService(botToken).sendMessage(chatId, it)
            return
        }

        if (dictionary.isWordInDictionary(original)) {
            TgBotMainService(botToken).sendMessage(chatId,
                "Это слово уже есть в словаре. Пожалуйста, введи другое слово.")
            return
        }

        if (!dictionary.checkLatinWords(original)) {
            TgBotMainService(botToken).sendMessage(chatId,
                "Ошибка: используй только латинские символы.")
            return
        }

        wordData.original = original
        userStates[chatId] = AddWordState.AWAITING_TRANSLATION
        TgBotMainService(botToken).sendMessage(chatId, "Введи перевод")
    }

    fun handleTranslation(chatId: Long, translation: String, dictionary: Dictionary) {
        val wordData = userWordData[chatId] ?: run {
            TgBotMainService(botToken).sendMessage(chatId, "Ошибка: не найдено оригинальное слово")
            return
        }

        if (translation.isBlank()) {
            TgBotMainService(botToken).sendMessage(chatId, "Ошибка: перевод не может быть пустым.")
            return
        }

        if (!dictionary.checkCyrillicWords(translation)) {
            TgBotMainService(botToken).sendMessage(chatId,
                "Ошибка: используй только кириллические символы.")
            return
        }

        wordData.translation = translation
        wordData.correctAnswersCount = START_CORRECT_ANSWERS_COUNT
        wordData.usageCount = START_USAGE_COUNT

        val currentWords = dictionary.loadDictionary().toMutableList()
        currentWords.add(wordData)
        dictionary.saveDictionary(currentWords)

        TgBotMainService(botToken).sendMessage(chatId, "Слово успешно добавлено в словарь!")
        resetUserState(chatId)
        TgBotMainService(botToken).sendMenu(chatId)
    }

    fun showStats(chatId: Long, statistics: Statistics) {

        try {
            val message = """
            Статистика:
            
            В словаре ${statistics.wordsInFile} элементов
            В твоей памяти осталось ${statistics.learnedWords}
            Пройдено ${statistics.progressPercentage}% пути
        """.trimIndent()

            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId,
                message,
                ReplyMarkup(
                    listOf(
                        listOf(InlineKeyboard(MENU, "Вернуться в меню"))
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
            TgBotMainService(botToken).sendMessage(chatId, "Произошла ошибка при получении статистики.")
        }
    }

    fun sendIterationsSettingMenu(chatId: Long, trainer: LearnWordsTrainer) {

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

    fun handleIterationsSettingCallback(chatId: Long, messageText: String, trainer: LearnWordsTrainer) {

        try {
            val numberOfIterations = messageText.trim().toIntOrNull()

            if (numberOfIterations == null || numberOfIterations <= WRONG_NUMBER_OF_TRAINS) {
                TgBotMainService(botToken).sendMessage(chatId, "Пожалуйста, введи число больше 0")
            } else {
                trainer.settings.numberOfIterations = numberOfIterations
                trainer.settings.saveSettings()
                TgBotMainService(botToken).sendMessage(
                    chatId,
                    "Количество слов в одной тренировке изменено на $numberOfIterations"
                )
                TgBotMainService(botToken).sendSettingsMenu(chatId)
            }

        } catch (e: Exception) {
            println("Error handling iterations setting: ${e.message}")
            TgBotMainService(botToken).sendMessage(chatId, "Произошла ошибка при обработке запроса")
        }
    }

    fun sendFilterSettingMenu(chatId: Long, trainer: LearnWordsTrainer) {

        try {
            val urlSendMessage = "$API_URL$botToken/sendMessage"

            val requestBody = SendMessageRequest(
                chatId,
                "Выбери тип тренировки.\nТекущее значение: ${trainer.settings.filter}",
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

    fun handleFilterSettingCallback(chatId: Long, callbackData: String, trainer: LearnWordsTrainer) {

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
            TgBotMainService(botToken).sendMessage(chatId, "Тип тренировки изменён на $displayName")
            TgBotMainService(botToken).sendSettingsMenu(chatId)

        } catch (e: Exception) {
            println("Error handling filter setting: ${e.message}")
            TgBotMainService(botToken).sendMessage(chatId, "Произошла ошибка при изменении типа тренировки")
        }
    }

    fun handleResetProgress(chatId: Long) {

        try {
            val dictionary = Dictionary(chatId)
            dictionary.resetProgress()

            TgBotMainService(botToken).sendMessage(chatId, "Прогресс успешно сброшен.")
            TgBotMainService(botToken).sendSettingsMenu(chatId)
        } catch (e: Exception) {
            println("Error resetting progress: ${e.message}")
            TgBotMainService(botToken).sendMessage(chatId, "Произошла ошибка при сбросе прогресса")
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
