package ktb.trainer

import trainer.model.Dictionary
import trainer.model.Question
import trainer.model.Settings
import trainer.model.Statistics
import trainer.model.Word

class LearnWordsTrainer(private val dictionary: Dictionary) {

    var settings = Settings().apply { loadSettings() }
    var question: Question? = null
    private var currentDictionary: List<Word> = dictionary.loadDictionary()

    fun getStatistics(): Statistics {

        refreshDictionary()

        val learnedWords = (currentDictionary.filter { it.correctAnswersCount >= NUMBER_OF_CORRECT_ANSWERS }).size
        val wordsInFile = currentDictionary.size
        val progressPercentage = (learnedWords.toFloat() / wordsInFile * PERCENTAGE).toInt()

        return Statistics(learnedWords, wordsInFile, progressPercentage)
    }

    fun resetUsage() {

        currentDictionary.forEach { word ->
            word.usageCount = 0
        }

        dictionary.saveDictionary(currentDictionary)
    }

    fun getNextQuestion(): Question? {

        refreshDictionary()

        val notLearnedList = currentDictionary.filter { it.correctAnswersCount < NUMBER_OF_CORRECT_ANSWERS }

        val filter = settings.filter.takeIf { it in listOf("слова", "словосочетания", "выражения", "всё") } ?: "всё"
        val filteredNotLearnedList = notLearnedList.filter { word ->
            when (filter) {
                "слова" -> word.original.split(" ").size == WORD_SIZE
                "словосочетания" -> word.original.split(" ").size == COLLOCATION_SIZE
                "выражения" -> word.original.split(" ").size >= EXPRESSION_SIZE
                "всё" -> true
                else -> false
            }
        }

        if (filteredNotLearnedList.isEmpty()) return null

        val minUsageCount = filteredNotLearnedList.minOf { it.usageCount }
        val leastUsedWords = filteredNotLearnedList.filter { it.usageCount == minUsageCount }
        val learningWord = leastUsedWords.random()

        learningWord.usageCount++
        dictionary.saveDictionary(currentDictionary)

        val incorrectTranslations = currentDictionary
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
                dictionary.saveDictionary(currentDictionary)
                true
            } else {
                false
            }
        } ?: false
    }

    private fun refreshDictionary() {

        currentDictionary = dictionary.loadDictionary()
    }
}

const val DEFAULT_NUMBER_OF_TRAINS = 4
const val DEFAULT_FILTER = "всё"

const val PERCENTAGE = 100
const val NUMBER_OF_CORRECT_ANSWERS = 3.toShort()
const val NUMBER_OF_INCORRECT_ANSWERS = 3

const val WORD_SIZE = 1
const val COLLOCATION_SIZE = 2
const val EXPRESSION_SIZE = 3
