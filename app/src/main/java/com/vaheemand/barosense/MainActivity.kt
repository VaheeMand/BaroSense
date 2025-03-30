package com.vaheemand.barosense

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.vaheemand.barosense.databinding.ActivityMainBinding
import kotlin.math.pow

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private var currentPressure = 0f
    private var lastUpdateTime = 0L
    private var updateInterval = 1000L
    private val pressureHistory = mutableListOf<Float>()
    private val maxHistorySize = 100
    private val handler = Handler(Looper.getMainLooper())
    private var currentUnit = "hPa"
    private var currentAngle = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "BaroSense"

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        loadSettings()
        setupGraph()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        updateInterval = prefs.getString("update_interval", "1000")?.toLongOrNull() ?: 1000L
        currentUnit = prefs.getString("pressure_unit", "hPa") ?: "hPa"
    }

    private fun setupGraph() {
        handler.post(object : Runnable {
            override fun run() {
                if (pressureHistory.isNotEmpty()) {
                    binding.graphView.addDataPoint(pressureHistory.last())
                    binding.graphView.invalidate()
                }
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        loadSettings()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PRESSURE) {
            val now = System.currentTimeMillis()
            if (now - lastUpdateTime >= updateInterval) {
                lastUpdateTime = now
                currentPressure = event.values[0]
                updateUI()
                updateHistory()
            }
        }
    }

    private fun updateUI() {
        val (value, unit) = convertPressure(currentPressure)
        binding.pressureValue.text = "%.1f".format(value)
        binding.pressureUnit.text = unit
        animateNeedle(calculateNeedleAngle(value))
    }

    private fun convertPressure(pressure: Float): Pair<Float, String> = when (currentUnit) {
        "mmHg" -> pressure * 0.750062f to "mmHg"
        "bar" -> pressure * 0.001f to "bar"
        "atm" -> pressure * 0.000986923f to "atm"
        "m" -> altitudeFromPressure(pressure) to "m"
        else -> pressure to "hPa"
    }

    private fun altitudeFromPressure(pressure: Float): Float {
        return (44330 * (1 - (pressure / 1013.25).pow(0.190284))).toFloat()
    }    

    private fun calculateNeedleAngle(value: Float): Float = when (currentUnit) {
        "hPa" -> (value - 950) * 0.9f
        "mmHg" -> (value - 700) * 1.2f
        "bar" -> (value - 0.9f) * 1000
        "atm" -> (value - 0.9f) * 1000
        "m" -> value * 2
        else -> value
    }

    private fun animateNeedle(newAngle: Float) {
        RotateAnimation(
            currentAngle, newAngle,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 500
            fillAfter = true
            binding.needle.startAnimation(this)
        }
        currentAngle = newAngle
    }

    private fun updateHistory() {
        pressureHistory.add(currentPressure)
        if (pressureHistory.size > maxHistorySize) {
            pressureHistory.removeAt(0)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}