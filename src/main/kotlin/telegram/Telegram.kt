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

    val service = TgBotMainService(args[FIRST_INDEX])
    val dictionaryService = TgBotDictionaryService(args[FIRST_INDEX])
    val activeTrain = mutableMapOf<Long, Pair<LearnWordsTrainer, TrainState>>()
    val userIterationSettingState: MutableMap<Long?, Boolean> = mutableMapOf()

    while (true) {
        Thread.sleep(SLEEP)
        val firstUpdate = json.decodeFromString<Response>(service.getUpdates(lastUpdateId))
            .result.firstOrNull() ?: continue
        lastUpdateId = firstUpdate.updateId + INCREMENT

        handleUpdates(firstUpdate, service, dictionaryService, activeTrain, userIterationSettingState)
    }
}

fun completeTraining(
    chatId: Long,
    activeTrain: MutableMap<Long, Pair<LearnWordsTrainer, TrainState>>,
    service: TgBotMainService
) {
    activeTrain.remove(chatId)
    service.sendMessage(chatId, "Тренировка завершена!")
    service.sendMenu(chatId)
}

fun train(
    chatId: Long,
    trainer: LearnWordsTrainer,
    activeTrain: MutableMap<Long, Pair<LearnWordsTrainer, TrainState>>,
    service: TgBotMainService,
    dictionaryService: TgBotDictionaryService,
) {
    val numberOfWordsToTrain = trainer.settings.numberOfIterations
    trainer.resetUsage()

    val trainState = TrainState(chatId, numberOfWordsToTrain)
    activeTrain[chatId] = trainer to trainState

    val onComplete = { completeTraining(chatId, activeTrain, service) }
    dictionaryService.checkNextQuestionAndSend(chatId, trainer, trainState, onComplete)
}

fun addWordToFileWithBot(
    chatId: Long,
    service: TgBotMainService,
    dictionaryService: TgBotDictionaryService,
) {

    val wordsFile = File(WORDS_FILE)

    if (!wordsFile.exists()) {
        wordsFile.createNewFile()
    }

    service.sendMessage(chatId, "Пополнение словаря")
    dictionaryService.sendTypeOfWordMenu(chatId)
}

fun handleUpdates(
    update: Update,
    service: TgBotMainService,
    dictionaryService: TgBotDictionaryService,
    activeTrain: MutableMap<Long, Pair<LearnWordsTrainer, TrainState>>,
    userIterationSettingState: MutableMap<Long?, Boolean>,
) {

    val messageText = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return

    val data = update.callbackQuery?.data

    if (data != null) {

        when {
            data == LOAD_DICTIONARY -> service.handleLoadDictionary(chatId)
            data == EMPTY_DICTIONARY -> service.handleEmptyDictionary(chatId)

            data == MENU -> service.sendMenu(chatId)

            data == LEARN_WORDS -> train(chatId, LearnWordsTrainer(chatId), activeTrain, service, dictionaryService)
            data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                val (trainer, trainState) = activeTrain[chatId] ?: run {
                    service.sendMessage(chatId, "Тренировка не начата. Начните новую тренировку.")
                    return
                }

                dictionaryService.handleAnswer(chatId, data, trainer, trainState)
                { completeTraining(chatId, activeTrain, service) }
            }

            data == ADD_WORD -> addWordToFileWithBot(chatId, service, dictionaryService)
            data == TYPE_WORD || data == TYPE_WORD_PAIR || data == TYPE_EXPRESSION || data == TYPE_ALL ->
                dictionaryService.handleWordTypeSelection(chatId, data)

            data == STATS -> {
                val statistics = LearnWordsTrainer(chatId).getStatistics()
                dictionaryService.showStats(chatId, statistics)
            }
            data == SETTINGS -> {
                userIterationSettingState[chatId] = false
                service.sendSettingsMenu(chatId)
            }

            data == CHANGE_NUMBER_OF_ITERATIONS -> {
                userIterationSettingState[chatId] = true
                dictionaryService.sendIterationsSettingMenu(chatId, LearnWordsTrainer(chatId))
            }

            data == CHANGE_TYPE_OF_TRAIN -> dictionaryService.sendFilterSettingMenu(chatId, LearnWordsTrainer(chatId))
            data == FILTER_WORD || data == FILTER_WORD_PAIR || data == FILTER_EXPRESSION || data == FILTER_ALL ->
                dictionaryService.handleFilterSettingCallback(chatId, data, LearnWordsTrainer(chatId))
            data == RESET_PROGRESS -> dictionaryService.handleResetProgress(chatId)
        }
    } else if (messageText != null) {

        when {
            messageText == START -> service.sendGreeting(chatId)
            messageText == MENU -> service.sendMenu(chatId)
            messageText.matches(Regex("\\d+")) && userIterationSettingState[chatId] == true -> {
                dictionaryService.handleIterationsSettingCallback(chatId, messageText, LearnWordsTrainer(chatId))
                userIterationSettingState[chatId] = false
            }

            else -> {
                when (userStates[chatId]) {
                    AddWordState.AWAITING_ORIGINAL -> {
                        val dictionary = Dictionary(chatId)
                        dictionaryService.handleOriginalWord(chatId, messageText, dictionary)
                    }

                    AddWordState.AWAITING_TRANSLATION -> {
                        val dictionary = Dictionary(chatId)
                        dictionaryService.handleTranslation(chatId, messageText, dictionary)
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
const val RESET_PROGRESS = "reset_progress"


const val START = "/start"
const val MENU = "/menu"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
