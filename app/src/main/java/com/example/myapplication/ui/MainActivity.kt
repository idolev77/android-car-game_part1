package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.logic.GameManager

class MainActivity : AppCompatActivity() {

    // ===== Grid Configuration =====
    private val GRID_ROWS = 14  // 14 rows - bear takes MUCH longer to reach bottom
    private val GRID_COLS = 3
    private val PLAYER_ROW = GRID_ROWS - 2

    // ===== UI Variables =====
    private lateinit var gameGrid: TableLayout
    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var textScore: TextView
    private lateinit var heartViews: Array<ImageView>

    // ===== Grid Cells =====
    private val gridCells = Array(GRID_ROWS) { row ->
        Array<ImageView?>(GRID_COLS) { null }
    }

    // ===== Logic =====
    private lateinit var gameManager: GameManager
    private val handler = Handler(Looper.getMainLooper())
    private val frameRate: Long = 600 // Move every 0.6 seconds - faster gameplay

    // Obstacle tracking
    private var obstacleRow = -1
    private var obstacleCol = 1
    private var collisionDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Force LTR
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR

        initViews()
        buildGrid()
        initGame()
    }

    private fun initViews() {
        gameGrid = findViewById(R.id.game_grid)
        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
        textScore = findViewById(R.id.text_score)

        // Load GIF background
        val backgroundGif = findViewById<ImageView>(R.id.background_gif)
        Glide.with(this)
            .asGif()
            .load(R.drawable.tenor)
            .into(backgroundGif)

        heartViews = arrayOf(
            findViewById(R.id.heart1),
            findViewById(R.id.heart2),
            findViewById(R.id.heart3)
        )
    }

    private fun buildGrid() {
        gameGrid.removeAllViews()

        // Use TableLayout's built-in equal distribution
        // No need to calculate cell height - let TableLayout handle it!

        for (row in 0 until GRID_ROWS) {
            val tableRow = TableRow(this).apply {
                layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    0,  // Height 0 with weight
                    1f  // Equal weight for all rows
                )
                gravity = android.view.Gravity.CENTER
            }

            for (col in 0 until GRID_COLS) {
                val cell = ImageView(this).apply {
                    layoutParams = TableRow.LayoutParams(
                        0,  // Width 0 with weight
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f  // Equal weight for all columns
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(24, 24, 24, 24)  // More padding = smaller icons
                    adjustViewBounds = true
                }

                gridCells[row][col] = cell
                tableRow.addView(cell)
            }

            gameGrid.addView(tableRow)
        }
    }

    private fun initGame() {
        gameManager = GameManager()

        btnLeft.setOnClickListener {
            gameManager.moveCarLeft()
            updatePlayerPosition()
        }

        btnRight.setOnClickListener {
            gameManager.moveCarRight()
            updatePlayerPosition()
        }

        updatePlayerPosition()
        updateLivesUI()
        updateScoreUI()
        spawnNewObstacle()
        startTimer()
    }

    // ===== Game Loop =====
    private val runnable = object : Runnable {
        override fun run() {
            if (gameManager.isGameRunning) {
                tick()  // Execute game logic FIRST
                handler.postDelayed(this, frameRate)  // Then schedule next run
            }
        }
    }

    private fun startTimer() {
        handler.postDelayed(runnable, frameRate)
    }

    private fun stopTimer() {
        handler.removeCallbacks(runnable)
    }

    private fun tick() {
        android.util.Log.d("MainActivity", "tick() - isGameRunning=${gameManager.isGameRunning}, lives=${gameManager.lives}")

        if (!gameManager.isGameRunning) {
            android.util.Log.d("MainActivity", "Game Over! Opening GameOverActivity")
            stopTimer()
            openGameOverActivity()
            return
        }

        moveObstacle()
        updateScoreUI()
        updateLivesUI()
    }

    private fun spawnNewObstacle() {
        obstacleRow = 0
        obstacleCol = (0 until GRID_COLS).random()
        collisionDetected = false

        // Place obstacle in grid
        gridCells[obstacleRow][obstacleCol]?.setImageResource(R.drawable.bear)
    }

    private fun moveObstacle() {
        if (obstacleRow < 0) return

        // Clear current position
        if (obstacleRow < GRID_ROWS) {
            gridCells[obstacleRow][obstacleCol]?.setImageDrawable(null)
        }

        // Move down one row
        obstacleRow++

        // Check if reached bottom
        if (obstacleRow >= GRID_ROWS) {
            // Obstacle passed successfully
            if (!collisionDetected) {
                gameManager.incrementScore()
            }
            spawnNewObstacle()
            return
        }

        // Check collision BEFORE placing
        if (obstacleRow == PLAYER_ROW && obstacleCol == gameManager.currentCarIndex) {
            // Collision!
            android.util.Log.d("MainActivity", "COLLISION DETECTED! Lives before: ${gameManager.lives}")
            collisionDetected = true
            onCrash()

            // Reduce lives directly
            gameManager.handleCollision()
            android.util.Log.d("MainActivity", "Lives after collision: ${gameManager.lives}, isGameRunning: ${gameManager.isGameRunning}")

            // Check for game over IMMEDIATELY after collision
            if (!gameManager.isGameRunning) {
                android.util.Log.d("MainActivity", "Game Over immediately after collision!")
                stopTimer()
                openGameOverActivity()
                return
            }

            // Skip placing obstacle (it crashed)
            spawnNewObstacle()
            return
        }

        // Place obstacle in new position
        gridCells[obstacleRow][obstacleCol]?.setImageResource(R.drawable.bear)
    }

    private fun updatePlayerPosition() {
        // Clear all player positions
        for (col in 0 until GRID_COLS) {
            gridCells[PLAYER_ROW][col]?.setImageDrawable(null)
        }

        // Place player in current lane
        val playerCol = gameManager.currentCarIndex
        gridCells[PLAYER_ROW][playerCol]?.setImageResource(R.drawable.bitcoin)
    }

    private fun updateLivesUI() {
        for (i in heartViews.indices) {
            heartViews[i].visibility = if (i < gameManager.lives) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun updateScoreUI() {
        textScore.text = getString(R.string.score_label, gameManager.score)
    }

    private fun onCrash() {
        Toast.makeText(this, "Crash!", Toast.LENGTH_SHORT).show()

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun openGameOverActivity() {
        val intent = Intent(this, GameOverActivity::class.java)
        intent.putExtra("FINAL_SCORE", gameManager.score)
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "onResume() - isGameRunning=${if (::gameManager.isInitialized) gameManager.isGameRunning else "not initialized"}")
        if (::gameManager.isInitialized && gameManager.isGameRunning) {
            android.util.Log.d("MainActivity", "Restarting timer in onResume")
            startTimer()
        }
    }
}

