package Telegram

import ktb.trainer.LearnWordsTrainer
import trainer.model.Dictionary

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        println("Usage: java Telegram <bot_token>")
        return
    }

    val updateIdRegex = Regex("update_id\":(\\d+)")
    val messageTextRegex = Regex("\"text\":\"([^\"]+)\"")
    val chatIdRegex = Regex("chat\":\\{\"id\":(\\d+)")
    val dataRegex = Regex("\"data\":\"([^\"]+)\"")

    val botToken = args[FIRST_INDEX]
    var updateId = START_UPDATE_ID

    val service = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer(Dictionary())

    while (true) {
        Thread.sleep(SLEEP)
        val updates = service.getUpdates(botToken, updateId)

        val updateIdMatch = updateIdRegex.find(updates)
        val updateIdString = updateIdMatch?.groupValues?.get(SECOND_INDEX)

        val messageText = messageTextRegex.find(updates)?.groupValues?.get(SECOND_INDEX)

        val chatIdMatch = chatIdRegex.find(updates)
        val chatId = chatIdMatch?.groupValues?.get(SECOND_INDEX) ?: continue
        val data = dataRegex.find(updates)?.groupValues?.get(SECOND_INDEX)

        when {
            data != null -> when (data.lowercase()) {
                LEARN_WORDS -> service.sendMessage(botToken, chatId, "Learn words")
                ADD_WORD -> service.sendMessage(botToken, chatId, "Add word")
                STATS -> service.sendMessage(botToken, chatId, "Stats")
                SETTINGS -> service.sendMessage(botToken, chatId, "Settings")
                else -> Unit
            }
            messageText != null -> when (messageText.lowercase()) {
                HELLO.lowercase() -> service.sendMessage(botToken, chatId, "Hello!")
                START.lowercase() -> service.sendMenu(botToken, chatId)
                else -> Unit
            }
        }

        if (updateIdString != null) {
            updateId = updateIdString.toInt() + INCREMENT
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

const val HELLO = "Hello"
const val START = "/start"
