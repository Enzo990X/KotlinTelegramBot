package console

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken = args[FIRST_INDEX]
    var updateId = START_UPDATE_ID

    while (true) {
        Thread.sleep(SLEEP)
        val updates = getUpdates(botToken, updateId)

        val updateIdRegex = Regex("update_id\":(\\d+)")
        val updateIdMatch = updateIdRegex.find(updates)
        val updateIdString = updateIdMatch?.groupValues?.get(SECOND_INDEX)

        if (updateIdString != null) {
            updateId = updateIdString.toInt() + INCREMENT
        }

        val messageTextRegex = Regex("\"text\":\"([^\"]+)\"")
        val messageText = messageTextRegex.find(updates)?.groupValues?.get(SECOND_INDEX)
        println("text: $messageText")
    }
}

fun getUpdates(botToken: String, updateId: Int): String {

    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client = HttpClient.newBuilder().build()
    val requestUpdate = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val responseUpdate = client.send(requestUpdate, HttpResponse.BodyHandlers.ofString())

    return responseUpdate.body()
}

const val FIRST_INDEX = 0
const val SECOND_INDEX = 1
const val START_UPDATE_ID = 0
const val SLEEP = 2000L
const val INCREMENT = 1
