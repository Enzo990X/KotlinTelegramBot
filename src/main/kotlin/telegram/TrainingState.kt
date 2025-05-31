package telegram

import trainer.model.Question

data class TrainingState(
    val chatId: String,
    val questionsRemaining: Int,
    val currentQuestion: Question? = null
)