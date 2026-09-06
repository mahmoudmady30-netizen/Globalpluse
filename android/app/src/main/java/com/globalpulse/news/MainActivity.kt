package com.globalpulse.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.globalpulse.news.presentation.news.NewsScreen
import com.globalpulse.news.presentation.theme.GlobalPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlobalPulseTheme {
                NewsScreen()
            }
        }
    }
}
