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
        val chatId = chatIdMatch?.groupValues?.get(SECOND_INDEX)
        val data = dataRegex.find(updates)?.groupValues?.get(SECOND_INDEX)

        if (data?.equals("learn_words", ignoreCase = true) == true && chatId != null) {
            service.sendMessage(botToken, chatId, "Learn words")
        }

        if (data?.equals("add_word", ignoreCase = true) == true && chatId != null) {
            service.sendMessage(botToken, chatId, "Add word")
        }

        if (data?.equals("stats", ignoreCase = true) == true && chatId != null) {
            service.sendMessage(botToken, chatId, "Stats")
        }

        if (data?.equals("settings", ignoreCase = true) == true && chatId != null) {
            service.sendMessage(botToken, chatId, "Settings")
        }

        if (messageText?.equals("Hello", ignoreCase = true) == true && chatId != null) {
            service.sendMessage(botToken, chatId, "Hello!")
        }

        if (messageText?.equals("/start", ignoreCase = true) == true && chatId != null) {
            service.sendMenu(botToken, chatId)
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
