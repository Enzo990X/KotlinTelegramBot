package telegram

import trainer.model.Question

data class TrainState(
    val chatId: Long?,
    var questionsRemaining: Int,
    var currentQuestion: Question? = null,
    var isWaitingForAnswer: Boolean = false,
    var onComplete: (() -> Unit)? = null
) {
    fun startNewQuestion(question: Question, onComplete: () -> Unit) {
        currentQuestion = question
        isWaitingForAnswer = true
        this.onComplete = onComplete
    }

    fun handleAnswerProcessed() {
        isWaitingForAnswer = false
        questionsRemaining--
    }

    fun completeTraining() {
        onComplete?.invoke()
        onComplete = null
    }
}
