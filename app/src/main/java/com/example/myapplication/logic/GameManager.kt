package com.example.myapplication.logic

import android.content.Context
import android.widget.Toast

/**
 * Manages game logic: lives, collisions, and car state
 */
class GameManager(private val context: Context, private val lifeCount: Int = 3) {

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

    // ===== Lives & Collision =====
    fun checkCollision(obstacleLane: Int) {
        // If obstacle is in the same lane as the car
        if (obstacleLane == currentCarIndex) {
            handleCollision()
        }
    }

    private fun handleCollision() {
        lives--

        // No vibration here anymore

        // User feedback
        Toast.makeText(context, "BOOM! Collision!", Toast.LENGTH_SHORT).show()

        if (lives <= 0) {
            isGameRunning = false
            Toast.makeText(context, "Game Over!", Toast.LENGTH_LONG).show()
        }
    }
}