package ktb

import java.io.File

fun main() {

    val wordsFile: File = File("words.txt")
    wordsFile.createNewFile()

    updateDictionary(wordsFile)

    for (line in wordsFile.readLines()) {
        println(line)
    }
}

fun updateDictionary(dictionary: File) {
    println("Хотите пополнить Ваш словарь?")
    val enterAnswer = readln()

    if (enterAnswer.equals("да", ignoreCase = true)) addWord(dictionary)
}

fun addWord(wordsFile: File) {

    do {
        println("\nВведите новое слово: ")
        val newWord = getNewWord()
        wordsFile.appendText("$newWord\n", Charsets.UTF_8)

        println("\nХотите ввести ещё одно слово?")
    } while (readln().equals("да", ignoreCase = true))
}

fun getNewWord(): String = readln().lowercase()
