package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class GameOverActivity : AppCompatActivity() {

    private lateinit var textFinalScore: TextView
    private lateinit var btnPlayAgain: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        // Force LTR layout direction
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR

        initViews()
        displayScore()
    }

    private fun initViews() {
        textFinalScore = findViewById(R.id.text_final_score)
        btnPlayAgain = findViewById(R.id.btn_play_again)

        btnPlayAgain.setOnClickListener {
            playAgain()
        }
    }

    private fun displayScore() {
        val finalScore = intent.getIntExtra("FINAL_SCORE", 0)
        textFinalScore.text = getString(R.string.final_score_label, finalScore)
    }

    private fun playAgain() {
        // Return to MainActivity and reset the game
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}

