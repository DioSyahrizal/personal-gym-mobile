package com.example.personalgym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.personalgym.ui.GymApp
import com.example.personalgym.ui.theme.PersonalGymTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RestTimerAlerts.createChannels(this)
        setContent {
            PersonalGymTheme { GymApp(vm) }
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.isForeground = true
    }

    override fun onStop() {
        super.onStop()
        AppVisibility.isForeground = false
    }
}
