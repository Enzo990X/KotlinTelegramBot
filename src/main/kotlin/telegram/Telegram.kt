package telegram

import console.WORDS_FILE
import trainer.LearnWordsTrainer
import trainer.model.Dictionary
import java.io.File

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        println("Usage: java Telegram <bot_token>")
        return
    }

    val dictionary = Dictionary()

    val updateIdRegex = Regex("update_id\":(\\d+)")
    val messageTextRegex = Regex("\"text\":\"([^\"]+)\"")
    val chatIdRegex = Regex("chat\":\\{\"id\":(\\d+)")
    val dataRegex = Regex("\"data\":\"([^\"]+)\"")

    var updateId = START_UPDATE_ID

    val service = TelegramBotService(args[FIRST_INDEX])
    val trainer = LearnWordsTrainer(dictionary)
    val activeTrain = mutableMapOf<String, TrainState>()
    val userIterationSettingState = mutableMapOf<String, Boolean>()

    fun completeTraining(chatId: String) {
        activeTrain.remove(chatId)
        service.sendMessage(chatId, "Тренировка завершена!")
        service.sendMenu(chatId)
    }

    fun train(chatId: String) {
        val numberOfWordsToTrain = trainer.settings.numberOfIterations
        trainer.resetUsage()

        val trainState = TrainState(chatId, numberOfWordsToTrain)
        activeTrain[chatId] = trainState

        service.checkNextQuestionAndSend(trainer, chatId, trainState) { completeTraining(chatId) }
    }

    fun addWordToFileWithBot(chatId: String) {

        val wordsFile = File(WORDS_FILE)

        if (!wordsFile.exists()) {
            wordsFile.createNewFile()
        }

        service.sendMessage(chatId, "Пополнение словаря")
        service.sendTypeOfWordMenu(chatId)
    }

    while (true) {
        Thread.sleep(SLEEP)
        val updates = service.getUpdates(updateId)

        val updateIdMatch = updateIdRegex.find(updates)
        val updateIdString = updateIdMatch?.groupValues?.get(SECOND_INDEX)?.toIntOrNull() ?: continue

        updateId = updateIdString + INCREMENT

        val messageText = messageTextRegex.find(updates)?.groupValues?.get(SECOND_INDEX) ?: ""

        val chatIdMatch = chatIdRegex.find(updates)
        val chatId = chatIdMatch?.groupValues?.get(SECOND_INDEX) ?: continue
        val data = dataRegex.find(updates)?.groupValues?.get(SECOND_INDEX)?.lowercase()

        activeTrain[chatId]?.let updateId@{ trainState ->
            service.checkNextQuestionAndSend(trainer, chatId, trainState) { completeTraining(chatId) }
            return@updateId
        }

        when {
            data == START || messageText == START -> service.sendMenu(chatId)

            data == LEARN_WORDS -> train(chatId)
            data == ADD_WORD -> addWordToFileWithBot(chatId)
            data == TYPE_WORD || data == TYPE_WORD_PAIR || data == TYPE_EXPRESSION || data == TYPE_ALL -> {
                service.handleWordTypeSelection(chatId, data)
            }

            data == STATS -> service.showStats(chatId, trainer.getStatistics())
            data == SETTINGS -> {
                userIterationSettingState[chatId] = false
                service.sendSettingsMenu(chatId)
            }

            data == CHANGE_NUMBER_OF_ITERATIONS -> {
                userIterationSettingState[chatId] = true
                service.sendIterationsSettingMenu(chatId, trainer)
            }

            messageText.matches(Regex("\\d+")) && userIterationSettingState[chatId] == true -> {
                service.handleIterationsSettingCallback(chatId, messageText, trainer)
                userIterationSettingState[chatId] = false  // Reset the state after handling
            }

            data == CHANGE_TYPE_OF_TRAIN -> service.sendFilterSettingMenu(chatId, trainer)
            data == FILTER_WORD || data == FILTER_WORD_PAIR || data == FILTER_EXPRESSION || data == FILTER_ALL ->
                service.handleFilterSettingCallback(chatId, data, trainer)

            messageText.isNotBlank() -> {
                when (userStates[chatId]) {
                    AddWordState.AWAITING_ORIGINAL -> {
                        service.handleOriginalWord(chatId, messageText)
                    }
                    AddWordState.AWAITING_TRANSLATION -> {
                        service.handleTranslation(chatId, messageText)
                    }
                    else -> {
                        // Handle other commands or unknown input
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
const val SECOND_INDEX = 1
const val START_UPDATE_ID = 0
const val SLEEP = 2000L
const val INCREMENT = 1

const val LEARN_WORDS = "learn_words"
const val ADD_WORD = "add_word"
const val STATS = "stats"
const val SETTINGS = "settings"

const val START = "/start"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
