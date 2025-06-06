package trainer.model

import kotlinx.serialization.Serializable

@Serializable
data class Word(
    var original: String,
    var translation: String,
    val type: String,
    var correctAnswersCount: Short = START_CORRECT_ANSWERS_COUNT,
    var usageCount: Short = START_USAGE_COUNT
)

const val START_CORRECT_ANSWERS_COUNT = 0.toShort()
const val START_USAGE_COUNT = 0.toShort()
