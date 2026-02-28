package dev.cuza.FitSync.healthconnect

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.cuza.FitSync.MainActivity

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
        finish()
    }
}
