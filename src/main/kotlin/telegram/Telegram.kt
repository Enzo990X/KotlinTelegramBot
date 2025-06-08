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

    var lastUpdateId = START_UPDATE_ID

    val service = TelegramBotService(args[FIRST_INDEX])
    val trainer = LearnWordsTrainer(Dictionary())
    val activeTrain: MutableMap<Long?, TrainState> = mutableMapOf()
    val userIterationSettingState: MutableMap<Long?, Boolean> = mutableMapOf()

    while (true) {
        Thread.sleep(SLEEP)
        val firstUpdate = json.decodeFromString<Response>(service.getUpdates(lastUpdateId))
            .result.firstOrNull() ?: continue
        lastUpdateId = firstUpdate.updateId + INCREMENT

        handleUpdates(firstUpdate, service, trainer, activeTrain, userIterationSettingState)
    }
}

fun completeTraining(
    chatId: Long,
    activeTrain: MutableMap<Long?, TrainState>,
    service: TelegramBotService,
) {

    activeTrain.remove(chatId)
    service.sendMessage(chatId, "Тренировка завершена!")
    service.sendMenu(chatId)
}

fun train(
    chatId: Long,
    trainer: LearnWordsTrainer,
    activeTrain: MutableMap<Long?, TrainState>,
    service: TelegramBotService
) {

    val numberOfWordsToTrain = trainer.settings.numberOfIterations
    trainer.resetUsage()

    val trainState = TrainState(chatId, numberOfWordsToTrain)
    activeTrain[chatId] = trainState

    val context = TrainContext(trainer, trainState) { completeTraining(chatId, activeTrain, service) }

    service.checkNextQuestionAndSend(chatId, context)
}

fun addWordToFileWithBot(chatId: Long, service: TelegramBotService) {

    val wordsFile = File(WORDS_FILE)

    if (!wordsFile.exists()) {
        wordsFile.createNewFile()
    }

    service.sendMessage(chatId, "Пополнение словаря")
    service.sendTypeOfWordMenu(chatId)
}

fun handleUpdates(
    update: Update,
    service: TelegramBotService,
    trainer: LearnWordsTrainer,
    activeTrain: MutableMap<Long?, TrainState>,
    userIterationSettingState: MutableMap<Long?, Boolean>
) {

    val messageText = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id

    if (chatId == null) {
        println("Warning: Received update with no chat ID")
        return
    }

    val data = update.callbackQuery?.data

    if (data != null) {

        when {
            data == START -> service.sendMenu(chatId)
            data == LEARN_WORDS -> train(chatId, trainer, activeTrain, service)
            data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                activeTrain[chatId]?.let { trainState ->
                    val context = TrainContext(
                        trainer, trainState,
                        { completeTraining(chatId, activeTrain, service) })
                    service.handleAnswer(chatId, data, context)
                } ?: service.sendMessage(chatId, "Тренировка не начата. Начните новую тренировку.")
            }

            data == ADD_WORD -> addWordToFileWithBot(chatId, service)
            data == TYPE_WORD || data == TYPE_WORD_PAIR || data == TYPE_EXPRESSION || data == TYPE_ALL ->
                service.handleWordTypeSelection(chatId, data)

            data == STATS -> service.showStats(chatId, trainer.getStatistics())
            data == SETTINGS -> {
                userIterationSettingState[chatId] = false
                service.sendSettingsMenu(chatId)
            }

            data == CHANGE_NUMBER_OF_ITERATIONS -> {
                userIterationSettingState[chatId] = true
                service.sendIterationsSettingMenu(chatId, trainer)
            }

            data == CHANGE_TYPE_OF_TRAIN -> service.sendFilterSettingMenu(chatId, trainer)
            data == FILTER_WORD || data == FILTER_WORD_PAIR || data == FILTER_EXPRESSION || data == FILTER_ALL ->
                service.handleFilterSettingCallback(chatId, data, trainer)
        }
    } else if (messageText != null) {

        when {
            messageText == START -> service.sendMenu(chatId)
            messageText.matches(Regex("\\d+")) && userIterationSettingState[chatId] == true -> {
                service.handleIterationsSettingCallback(chatId, messageText, trainer)
                userIterationSettingState[chatId] = false
            }

            else -> {
                when (userStates[chatId]) {
                    AddWordState.AWAITING_ORIGINAL -> {
                        service.handleOriginalWord(chatId, messageText)
                    }

                    AddWordState.AWAITING_TRANSLATION -> {
                        service.handleTranslation(chatId, messageText)
                    }

                    else -> {
                        if (messageText.startsWith("/")) {
                            service.sendMessage(chatId, "Неизвестная команда. Используйте кнопки меню.")
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
