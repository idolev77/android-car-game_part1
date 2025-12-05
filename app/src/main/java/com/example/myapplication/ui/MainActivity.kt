package com.example.myapplication.ui

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.logic.GameManager

class MainActivity : AppCompatActivity() {

    // ===== UI Variables =====
    private lateinit var mainLayout: RelativeLayout
    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var textScore: TextView

    // View arrays to control lanes easily
    private lateinit var carViews: Array<ImageView>
    private lateinit var obstacleViews: Array<ImageView>
    private lateinit var heartViews: Array<ImageView>

    // ===== Logic =====
    private lateinit var gameManager: GameManager
    private val handler = Handler(Looper.getMainLooper())
    private val frameRate: Long = 30 // Refresh rate (ms)

    // Obstacle data
    private var activeObstacleLane = 1 // Which obstacle is currently falling
    private var obstacleSpeed = 10 // Reduced speed to make obstacles fall slower
    private var collisionDetected = false // Prevent double collision

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Force LTR layout direction to prevent RTL mirroring on Hebrew devices
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR

        initViews()
        initGame()
    }

    // ===== View Initialization =====
    private fun initViews() {
        mainLayout = findViewById(R.id.main_layout)
        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
        textScore = findViewById(R.id.text_score)

        // Load animated GIF background with Glide
        val backgroundGif = findViewById<ImageView>(R.id.background_gif)
        Glide.with(this)
            .asGif()
            .load(R.drawable.tenor)
            .into(backgroundGif)

        carViews = arrayOf(
            findViewById(R.id.car_left),
            findViewById(R.id.car_center),
            findViewById(R.id.car_right)
        )

        obstacleViews = arrayOf(
            findViewById(R.id.obstacle_left),
            findViewById(R.id.obstacle_center),
            findViewById(R.id.obstacle_right)
        )

        heartViews = arrayOf(
            findViewById(R.id.heart1),
            findViewById(R.id.heart2),
            findViewById(R.id.heart3)
        )

        // Hide all obstacles initially
        for (obs in obstacleViews) {
            obs.visibility = View.INVISIBLE
        }
    }

    private fun initGame() {
        gameManager = GameManager()

        btnLeft.setOnClickListener {
            gameManager.moveCarLeft()
            updateCarUI()
        }

        btnRight.setOnClickListener {
            gameManager.moveCarRight()
            updateCarUI()
        }

        updateCarUI() // Show initial car position
        updateLivesUI() // Show initial lives
        updateScoreUI() // Show initial score
        startTimer()
    }

    // ===== Game Loop =====
    private val runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, frameRate)
            tick()
        }
    }

    private fun startTimer() {
        handler.post(runnable)
    }

    private fun stopTimer() {
        handler.removeCallbacks(runnable)
    }

    // ===== Constant Updates =====
    private fun tick() {
        if (!gameManager.isGameRunning) {
            stopTimer()
            openGameOverActivity()
            return
        }

        moveObstacle()
        checkCollision()
        updateLivesUI()
        updateScoreUI()
    }

    private fun moveObstacle() {
        // Select active obstacle and update its topMargin
        val obstacle = obstacleViews[activeObstacleLane]

        // Ensure obstacle is visible
        if (obstacle.visibility != View.VISIBLE) {
            obstacle.visibility = View.VISIBLE
            // Reset position to top
            val params = obstacle.layoutParams as RelativeLayout.LayoutParams
            params.topMargin = -200
            obstacle.layoutParams = params
        }

        // Change Margin (Simulating Y movement)
        val params = obstacle.layoutParams as RelativeLayout.LayoutParams
        params.topMargin += obstacleSpeed

        // If obstacle goes off screen
        if (params.topMargin > mainLayout.height) {
            // Only add score if player dodged successfully (no collision)
            if (!collisionDetected) {
                gameManager.incrementScore() // +10 points for successful dodge!
            }

            params.topMargin = -200
            obstacle.visibility = View.INVISIBLE
            // Pick a new random lane
            activeObstacleLane = (0..2).random()
            collisionDetected = false // Reset for new obstacle
        }

        obstacle.layoutParams = params
    }

    // ===== UI Updates =====
    private fun updateCarUI() {
        // Show only the car in the current lane
        for (i in carViews.indices) {
            if (i == gameManager.currentCarIndex) {
                carViews[i].visibility = View.VISIBLE
            } else {
                carViews[i].visibility = View.INVISIBLE
            }
        }
    }

    private fun updateLivesUI() {
        // Hide hearts based on life count
        for (i in heartViews.indices) {
            heartViews[i].visibility = if (i < gameManager.lives) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun updateScoreUI() {
        textScore.text = getString(R.string.score_label, gameManager.score)
    }

    // ===== Game Over =====
    private fun openGameOverActivity() {
        val intent = Intent(this, GameOverActivity::class.java)
        intent.putExtra("FINAL_SCORE", gameManager.score)
        startActivity(intent)
        finish()
    }

    // ===== Collision Detection =====
    private fun checkCollision() {
        if (collisionDetected) return // Already handled this obstacle

        val car = carViews[gameManager.currentCarIndex]
        val obstacle = obstacleViews[activeObstacleLane]

        // Only check if obstacle is visible
        if (obstacle.visibility != View.VISIBLE) return

        // Use global screen coordinates for accurate detection
        val carLocation = IntArray(2)
        val obstacleLocation = IntArray(2)

        car.getLocationOnScreen(carLocation)
        obstacle.getLocationOnScreen(obstacleLocation)

        val carRect = Rect(
            carLocation[0],
            carLocation[1],
            carLocation[0] + car.width,
            carLocation[1] + car.height
        )

        val obstacleRect = Rect(
            obstacleLocation[0],
            obstacleLocation[1],
            obstacleLocation[0] + obstacle.width,
            obstacleLocation[1] + obstacle.height
        )

        if (Rect.intersects(carRect, obstacleRect)) {
            // Collision detected - ONLY reduces hearts, NO scoring
            collisionDetected = true
            onCrash() // Show toast and vibrate
            gameManager.checkCollision(activeObstacleLane) // Reduces hearts only

            // Move obstacle away immediately to prevent double detection
            val params = obstacle.layoutParams as RelativeLayout.LayoutParams
            params.topMargin = mainLayout.height + 100
            obstacle.layoutParams = params
        }
    }

    // ===== Crash Handler =====
    private fun onCrash() {
        // Display Toast message
        Toast.makeText(this, "Crash!", Toast.LENGTH_SHORT).show()

        // Trigger vibration
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onResume() {
        super.onResume()
        startTimer()
    }
}