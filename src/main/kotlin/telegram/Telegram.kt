package telegram

import console.WORDS_FILE
import kotlinx.serialization.json.Json
import trainer.LearnWordsTrainer
import trainer.model.Dictionary
import java.io.File

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        println("Usage: java Telegram <bot_token>")
        return
    }

    val json = Json { ignoreUnknownKeys = true }

    val dictionary = Dictionary()

    var lastUpdateId = START_UPDATE_ID

    val service = TelegramBotService(args[FIRST_INDEX])
    val trainer = LearnWordsTrainer(dictionary)
    val activeTrain = mutableMapOf<Long?, TrainState>()
    val userIterationSettingState = mutableMapOf<Long?, Boolean>()

    fun completeTraining(chatId: Long) {

        activeTrain.remove(chatId)
        service.sendMessage(json, chatId, "Тренировка завершена!")
        service.sendMenu(json, chatId)
    }

    fun train(chatId: Long) {

        val numberOfWordsToTrain = trainer.settings.numberOfIterations
        trainer.resetUsage()

        val trainState = TrainState(chatId, numberOfWordsToTrain)
        activeTrain[chatId] = trainState

        val context = TrainContext(json, trainer, trainState, { completeTraining(chatId) })

        service.checkNextQuestionAndSend(json, chatId, context)
    }

    fun addWordToFileWithBot(chatId: Long) {

        val wordsFile = File(WORDS_FILE)

        if (!wordsFile.exists()) {
            wordsFile.createNewFile()
        }

        service.sendMessage(json, chatId, "Пополнение словаря")
        service.sendTypeOfWordMenu(json, chatId)
    }

    loop@ while (true) {
        Thread.sleep(SLEEP)
        val firstUpdate = json.decodeFromString<Response>(service.getUpdates(lastUpdateId))
            .result.firstOrNull() ?: continue
        lastUpdateId = firstUpdate.updateId + INCREMENT

        val messageText = firstUpdate.message?.text
        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
        if (chatId == null) {
            println("Warning: Received update with no chat ID")
            continue@loop
        }
        val data = firstUpdate.callbackQuery?.data

        if (data != null) {

            when {
                data == START -> service.sendMenu(json, chatId)
                data == LEARN_WORDS -> train(chatId)
                data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                    activeTrain[chatId]?.let { trainState ->
                        val context = TrainContext(json, trainer, trainState, { completeTraining(chatId) } )
                        service.handleAnswer(chatId, data, context)
                    } ?: service.sendMessage(json, chatId, "Тренировка не начата. Начните новую тренировку.")
                }

                data == ADD_WORD -> addWordToFileWithBot(chatId)
                data == TYPE_WORD || data == TYPE_WORD_PAIR || data == TYPE_EXPRESSION || data == TYPE_ALL ->
                    service.handleWordTypeSelection(json, chatId, data)

                data == STATS -> service.showStats(json, chatId, trainer.getStatistics())
                data == SETTINGS -> {
                    userIterationSettingState[chatId] = false
                    service.sendSettingsMenu(json, chatId)
                }

                data == CHANGE_NUMBER_OF_ITERATIONS -> {
                    userIterationSettingState[chatId] = true
                    service.sendIterationsSettingMenu(json, chatId, trainer)
                }

                data == CHANGE_TYPE_OF_TRAIN -> service.sendFilterSettingMenu(json, chatId, trainer)
                data == FILTER_WORD || data == FILTER_WORD_PAIR || data == FILTER_EXPRESSION || data == FILTER_ALL ->
                    service.handleFilterSettingCallback(json, chatId, data, trainer)
            }
        } else if (messageText != null) {

            when {
                messageText == START -> service.sendMenu(json, chatId)
                messageText.matches(Regex("\\d+")) && userIterationSettingState[chatId] == true -> {
                    service.handleIterationsSettingCallback(json, chatId, messageText, trainer)
                    userIterationSettingState[chatId] = false
                }

                else -> {
                    when (userStates[chatId]) {
                        AddWordState.AWAITING_ORIGINAL -> {
                            service.handleOriginalWord(json, chatId, messageText)
                        }

                        AddWordState.AWAITING_TRANSLATION -> {
                            service.handleTranslation(json, chatId, messageText)
                        }

                        else -> {
                            if (messageText.startsWith("/")) {
                                service.sendMessage(json, chatId, "Неизвестная команда. Используйте кнопки меню.")
                            }
                        }
                    }
                }
            }
        }
    }
}

const val FIRST_INDEX = 0
const val START_UPDATE_ID = 0L
const val SLEEP = 2000L
const val INCREMENT = 1

const val LEARN_WORDS = "learn_words"
const val ADD_WORD = "add_word"
const val STATS = "stats"
const val SETTINGS = "settings"

const val START = "/start"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
