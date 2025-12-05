package com.example.myapplication.logic

/**
 * Manages game logic: lives, collisions, and car state
 */
class GameManager(private val lifeCount: Int = 3) {

    var score: Int = 0
        private set

    var lives: Int = lifeCount
        private set

    var currentCarIndex: Int = 1 // 0=Left, 1=Center, 2=Right
        private set

    var isGameRunning: Boolean = false
        private set

    // ===== Initialization =====
    init {
        resetGame()
    }

    fun resetGame() {
        score = 0
        lives = lifeCount
        currentCarIndex = 1
        isGameRunning = true
    }

    // ===== Car Movement =====
    fun moveCarLeft() {
        if (currentCarIndex > 0) {
            currentCarIndex--
        }
    }

    fun moveCarRight() {
        if (currentCarIndex < 2) {
            currentCarIndex++
        }
    }

    // ===== Score Management =====
    fun incrementScore() {
        score += 10
    }

    // ===== Lives & Collision =====
    fun checkCollision(obstacleLane: Int) {
        // If obstacle is in the same lane as the car
        if (obstacleLane == currentCarIndex) {
            handleCollision()
        }
    }

    private fun handleCollision() {
        lives--

        // Toast is shown in MainActivity.onCrash() to avoid duplicate messages

        if (lives <= 0) {
            isGameRunning = false
            // Game Over message removed - GameOverActivity will show instead
        }
    }
}