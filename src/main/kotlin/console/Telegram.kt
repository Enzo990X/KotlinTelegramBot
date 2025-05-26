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
        println(updates)

        val startUpdateId = updates.lastIndexOf("update_id")
        val endUpdateId = updates.indexOf(",\n\"message\"", startUpdateId)

        if (startUpdateId == WRONG_ID || endUpdateId == WRONG_ID) continue
        val updateIdString = updates.substring(startUpdateId + ADDED_SYMBOLS_TO_START_INDEX, endUpdateId)

        updateId = updateIdString.toInt() + INCREMENT
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
const val START_UPDATE_ID = 0
const val SLEEP = 2000L
const val WRONG_ID = -1
const val ADDED_SYMBOLS_TO_START_INDEX = 11
const val INCREMENT = 1
