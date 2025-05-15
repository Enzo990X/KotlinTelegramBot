package ktb

import java.io.File

data class Statistics(
    val learnedWords: Int,
    val wordsInFile: Int,
    val progressPercentage: Int,
)

data class Question(
    val learningWord: Word,
    val translationsToPick: List<Word>,
)

class LearnWordsTrainer {

    var question: Question? = null
    private val dictionary = Dictionary().loadDictionary()

    fun getStatistics(): Statistics {

        val learnedWords = (dictionary.filter { it.correctAnswersCount >= NUMBER_OF_CORRECT_ANSWERS }).size
        val wordsInFile = dictionary.size
        val progressPercentage = (learnedWords.toFloat() / wordsInFile * PERCENTAGE).toInt()

        return Statistics(learnedWords, wordsInFile, progressPercentage)
    }

    fun getNextQuestion(): Question? {

        val notLearnedList = dictionary.filter { it.correctAnswersCount < NUMBER_OF_CORRECT_ANSWERS }
        if (notLearnedList.isEmpty()) return null

        val learningWord = notLearnedList.minByOrNull { it.usageCount } ?: return null
        learningWord.usageCount++

        val incorrectTranslations = notLearnedList
            .filter { it != learningWord }
            .shuffled()
            .take(NUMBER_OF_INCORRECT_ANSWERS)

        return Question(
            learningWord = learningWord,
            translationsToPick = (listOf(learningWord) + incorrectTranslations).shuffled()
        )
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {

        return question?.let {
            val correctAnswerIndex = it.translationsToPick.indexOf(it.learningWord)

            if (correctAnswerIndex == userAnswerIndex) {
                it.learningWord.correctAnswersCount++
                Dictionary().saveDictionary(dictionary)
                true
            } else {
                false
            }
        } ?: false
    }

    fun changeSettings() {

        val settings = File(SETTINGS_FILE)

        println("\nСколько раз показывать слова для тренировки (по умолчанию $WORDS_TO_LEARN)?")
        val newNumberOfIterations = readInput().toInt()

        settings.writeText("numberOfIterations=$newNumberOfIterations")
    }

    fun getSettings(): Int {

        val settingsFile = File(SETTINGS_FILE)

        if (!settingsFile.exists() || settingsFile.length() == 0L) return WORDS_TO_LEARN

        val settings = settingsFile.readLines().firstOrNull { it.startsWith("numberOfIterations=") }
            ?.split("=")?.get(SECOND_INDEX)?.toIntOrNull()

        return settings ?: WORDS_TO_LEARN
    }
}

class Dictionary {

    fun addWordToDictionary() {

        do {
            val newWord = createNewWord()
            val wordsFile = File(WORDS_FILE)
            wordsFile.appendText(
                "${newWord.original}|${newWord.translations}|${newWord.correctAnswersCount}\n",
                Charsets.UTF_8
            )

            println("\nХотите ввести ещё одно слово?")
        } while (readln().equals("да", ignoreCase = true))
    }

    private fun createNewWord(): Word {

        val input = readWordInput()
        val separation = input.split("|")

        val originalInput = separation[FIRST_INDEX]
        val translatedInput = separation[SECOND_INDEX]

        return Word(originalInput, translatedInput, BASE_CORRECT_ANSWERS_COUNT)
    }

    private fun readWordInput(): String {

        println("\nВведите новое слово или фразу на иностранном языке: ")
        var latinInput = readln().lowercase()

        while (latinInput.isEmpty()) {
            println("Ошибка: пустая строка. Повторите ввод.")
            latinInput = readln().lowercase()
        }

        while (!checkLatinWords(latinInput)) {
            println("Ошибка. Используйте только латинские символы. Повторите ввод.")
            latinInput = readln().lowercase()
            checkLatinWords(latinInput)
        }

        println("\nВведите перевод: ")
        var cyrillicInput = readln().lowercase()

        while (cyrillicInput.isEmpty()) {
            println("Ошибка: пустая строка. Повторите ввод.")
            cyrillicInput = readln().lowercase()
        }

        while (!checkCyrillicWords(cyrillicInput)) {
            println("Ошибка. Используйте только кириллические символы. Повторите ввод.")
            cyrillicInput = readln().lowercase()
            checkCyrillicWords(cyrillicInput)
        }

        val input = "$latinInput|$cyrillicInput"
        return input
    }

    private fun addWordToDictionaryFromFile(dictionary: MutableList<Word>) {

        val wordsFile = File(WORDS_FILE)
        for (line in wordsFile.readLines()) {
            val splitLine = line.split("|")
            val correctAnswersCount =
                splitLine[CORRECT_ANSWERS_COUNT_INDEX].toShortOrNull() ?: BASE_CORRECT_ANSWERS_COUNT
            val word = Word(splitLine[FIRST_INDEX], splitLine[SECOND_INDEX], correctAnswersCount)
            dictionary.add(word)
        }
    }

    private fun checkLatinWords(latinInput: String): Boolean {

        val inputSplit = latinInput.split(" ")
        return inputSplit[FIRST_INDEX].all { char ->
            (char in FIRST_EN_SMALL_CHAR..LAST_EN_SMALL_CHAR) ||
                    (char in FIRST_EN_BIG_CHAR..LAST_EN_BIG_CHAR)
        }
    }

    private fun checkCyrillicWords(cyrillicInput: String): Boolean {

        val inputSplit = cyrillicInput.split(" ")
        return inputSplit[FIRST_INDEX].all { char ->
            (char in FIRST_RU_SMALL_CHAR..LAST_RU_SMALL_CHAR) ||
                    (char in FIRST_RU_BIG_CHAR..LAST_RU_BIG_CHAR)
        }
    }

    fun saveDictionary(dictionary: List<Word>) {

        val wordsFile = File(WORDS_FILE)
        wordsFile.writeText("")
        dictionary.forEach { word ->
            wordsFile.appendText("${word.original}|${word.translations}|${word.correctAnswersCount}\n")
        }
    }

    fun loadDictionary(): List<Word> {

        val dictionary = mutableListOf<Word>()
        addWordToDictionaryFromFile(dictionary)
        return dictionary.toList()
    }
}

const val WORDS_TO_LEARN = 4

const val FIRST_EN_SMALL_CHAR = 'a'
const val LAST_EN_SMALL_CHAR = 'z'
const val FIRST_EN_BIG_CHAR = 'A'
const val LAST_EN_BIG_CHAR = 'Z'

const val FIRST_RU_SMALL_CHAR = 'а'
const val LAST_RU_SMALL_CHAR = 'я'
const val FIRST_RU_BIG_CHAR = 'А'
const val LAST_RU_BIG_CHAR = 'Я'

const val FIRST_INDEX = 0
const val SECOND_INDEX = 1
const val CORRECT_ANSWERS_COUNT_INDEX = 2
const val PERCENTAGE = 100
const val NUMBER_OF_CORRECT_ANSWERS = 3.toShort()
const val NUMBER_OF_INCORRECT_ANSWERS = 3

const val WORDS_FILE = "words.txt"
const val SETTINGS_FILE = "settings.txt"
