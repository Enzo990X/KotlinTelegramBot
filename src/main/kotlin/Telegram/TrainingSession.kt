package Telegram

import ktb.trainer.LearnWordsTrainer
import trainer.model.Question

class TrainingSession(
    val trainer: LearnWordsTrainer,
    val numberOfWordsToTrain: Int = trainer.settings.numberOfIterations,
    var currentQuestion: Question? = null,
    var questionsAsked: Int = 0
) {
    val isComplete: Boolean
        get() = questionsAsked >= numberOfWordsToTrain

    fun getNextQuestion(): Question? {
        if (isComplete) return null

        val question = trainer.getNextQuestion()
        currentQuestion = question
        return question
    }

    fun recordAnswer(selectedIndex: Int): Boolean {
        val isCorrect = trainer.checkAnswer(selectedIndex)
        questionsAsked++
        return isCorrect
    }
}