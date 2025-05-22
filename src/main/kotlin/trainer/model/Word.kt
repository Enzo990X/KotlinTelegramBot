package trainer.model

data class Word(
    val original: String,
    val translations: String,
    val type: String,
    var correctAnswersCount: Short = START_CORRECT_ANSWERS_COUNT,
    var usageCount: Int = START_USAGE_COUNT
)

const val START_CORRECT_ANSWERS_COUNT = 0.toShort()
const val START_USAGE_COUNT = 0
