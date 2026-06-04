package com.gymmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gymmate.app.ui.GymmateApp
import com.gymmate.app.ui.theme.GymmateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymmateTheme {
                GymmateApp()
            }
        }
    }
}
