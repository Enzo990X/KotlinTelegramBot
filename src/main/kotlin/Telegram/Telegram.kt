package Telegram

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        println("Usage: java Telegram <bot_token>")
        return
    }

    val updateIdRegex = Regex("update_id\":(\\d+)")
    val messageTextRegex = Regex("\"text\":\"([^\"]+)\"")
    val chatIdRegex = Regex("chat\":\\{\"id\":(\\d+)")

    val botToken = args[FIRST_INDEX]
    var updateId = START_UPDATE_ID

    val service = TelegramBotService(botToken)

    while (true) {
        Thread.sleep(SLEEP)
        val updates = service.getUpdates(botToken, updateId)

        val updateIdMatch = updateIdRegex.find(updates)
        val updateIdString = updateIdMatch?.groupValues?.get(SECOND_INDEX)

        val messageText = messageTextRegex.find(updates)?.groupValues?.get(SECOND_INDEX)

        val chatIdMatch = chatIdRegex.find(updates)
        val chatId = chatIdMatch?.groupValues?.get(SECOND_INDEX)


        if (messageText?.equals("Hello", ignoreCase = true) == true && chatId != null) {
            service.sendMessage(botToken, chatId, "Hello!")
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
