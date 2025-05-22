package trainer.model

data class Question(
    val learningWord: Word,
    val translationsToPick: List<Word>,
)