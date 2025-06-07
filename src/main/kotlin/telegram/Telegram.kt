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

    val instances = HashMap<Long, LearnWordsTrainer>()
    val trainerManager = TrainerManager()

    var lastUpdateId = START_UPDATE_ID

    val service = TelegramBotService(args[FIRST_INDEX])
    val trainer = LearnWordsTrainer(dictionary)
    val activeTrain = mutableMapOf<Long?, TrainState>()
    val userIterationSettingState = mutableMapOf<Long?, Boolean>()

    while (true) {

        Thread.sleep(SLEEP)

        val response = json.decodeFromString<Response>(service.getUpdates(lastUpdateId))
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { update ->
            val dependencies = Triple(update, activeTrain, userIterationSettingState)
            handleUpdate(
                json,
                instances,
                service,
                dependencies
            ) }
        lastUpdateId = sortedUpdates.last().updateId + INCREMENT
    }
}

fun ensureDataDirectoryExists() {
    val dataDir = File("data")
    if (!dataDir.exists()) {
        dataDir.mkdirs()
    }
}

fun completeTraining(
    json: Json,
    chatId: Long,
    activeTrain: MutableMap<Long?, TrainState>,
    service: TelegramBotService
) {
    activeTrain.remove(chatId)
    service.sendMessage(json, chatId, "Тренировка завершена!")
    service.sendMenu(json, chatId)
}

fun train(
    json: Json,
    trainer: LearnWordsTrainer,
    activeTrain: MutableMap<Long?, TrainState> = mutableMapOf(),
    chatId: Long,
    service: TelegramBotService
) {
    val numberOfWordsToTrain = trainer.settings.numberOfIterations
    trainer.resetUsage()

    val trainState = TrainState(chatId, numberOfWordsToTrain)
    activeTrain[chatId] = trainState

    val context = TrainContext(json, trainer, trainState
    ) { completeTraining(json, chatId, activeTrain, service) }

    service.checkNextQuestionAndSend(json, chatId, context)
}

    fun addWordToFileWithBot(json: Json, chatId: Long, service: TelegramBotService) {

    val wordsFile = File(WORDS_FILE)

    if (!wordsFile.exists()) {
        wordsFile.createNewFile()
    }

    service.sendMessage(json, chatId, "Пополнение словаря")
    service.sendTypeOfWordMenu(json, chatId)
}

fun handleUpdate(
    json: Json,
    instances: HashMap<Long, LearnWordsTrainer>,
    service: TelegramBotService,
    trainerManager: TrainerManager,
    dependencies: Triple<Update, MutableMap<Long?, TrainState>, MutableMap<Long?, Boolean>>
) {
    val (update, activeTrain, userIterationSettingState) = dependencies

    val messageText = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id
    if (chatId == null) {
        println("Warning: Received update with no chat ID")
        return
    }

    val data = update.callbackQuery?.data

    val trainer = trainerManager.getTrainerForUser(chatId)


    if (data != null) {

        when {
            update.message?.text == "/start" -> {
                service.sendMenu(json, chatId)
            }
            update.callbackQuery?.data == STATS -> {
                val stats = trainer.getStatistics()
                service.showStats(json, chatId, stats)
            }
            update.callbackQuery?.data == SETTINGS -> {
                service.sendSettingsMenu(json, chatId)
            }
            update.callbackQuery?.data == ADD_WORD -> {
                service.sendTypeOfWordMenu(json, chatId)
            }
            update.callbackQuery?.data == LEARN_WORDS -> {
                // Start a new training session
                val context = TrainContext(
                    trainer = trainer,
                    trainingState = TrainingState(trainer.settings.numberOfIterations),
                    onComplete = { completeTraining(json, chatId, it) }
                )
                service.checkNextQuestionAndSend(json, chatId, context)
            }
            update.callbackQuery?.data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true -> {
                // Handle answer to a question
                val context = TrainContext(
                    trainer = trainer,
                    trainingState = TrainingState.getForChat(chatId) ?: return,
                    onComplete = { completeTraining(json, chatId, it) }
                )
                service.handleAnswer(chatId, update.callbackQuery.data, context)
            }
            update.callbackQuery?.data == CHANGE_NUMBER_OF_ITERATIONS -> {
                service.sendIterationsSettingMenu(json, chatId, trainer)
            }
            update.callbackQuery?.data == CHANGE_TYPE_OF_TRAIN -> {
                service.sendFilterSettingMenu(json, chatId, trainer)
            }
            update.callbackQuery?.data in listOf(FILTER_WORD, FILTER_WORD_PAIR, FILTER_EXPRESSION, FILTER_ALL) -> {
                service.handleFilterSettingCallback(json, chatId, update.callbackQuery.data, trainer)
            }
            userStates[chatId] == AddWordState.AWAITING_ORIGINAL -> {
                service.handleOriginalWord(json, chatId, update.message?.text ?: return)
            }
            userStates[chatId] == AddWordState.AWAITING_TRANSLATION -> {
                service.handleTranslation(json, chatId, update.message?.text ?: return)
            }
            update.message?.text?.toIntOrNull() != null &&
                    TrainingState.getForChat(chatId)?.isWaitingForIterationsSetting == true -> {
                service.handleIterationsSettingCallback(json, chatId, update.message.text, trainer)
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
