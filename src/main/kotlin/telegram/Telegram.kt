package telegram

import trainer.LearnWordsTrainer
import trainer.model.Dictionary

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

    while (true) {
        Thread.sleep(SLEEP)
        val updates = service.getUpdates(updateId)

        val updateIdMatch = updateIdRegex.find(updates)
        val updateIdString = updateIdMatch?.groupValues?.get(SECOND_INDEX)?.toIntOrNull() ?: continue

        updateId = updateIdString + INCREMENT

        val messageText = messageTextRegex.find(updates)?.groupValues?.get(SECOND_INDEX)

        val chatIdMatch = chatIdRegex.find(updates)
        val chatId = chatIdMatch?.groupValues?.get(SECOND_INDEX) ?: continue
        val data = dataRegex.find(updates)?.groupValues?.get(SECOND_INDEX)?.lowercase()

        when {
            data == LEARN_WORDS -> service.sendMessage(chatId, "Learn words")
            data == ADD_WORD -> service.sendMessage(chatId, "Add word")
            data == STATS -> service.showStats(chatId, trainer.getStatistics())
            data == SETTINGS -> service.sendMessage(chatId, "Settings")
            data == START || messageText == START -> service.sendMenu(chatId)
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
