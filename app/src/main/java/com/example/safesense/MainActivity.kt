package com.example.safesense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.safesense.ui.navigation.SafeSenseNavGraph
import com.example.safesense.ui.theme.SafeSenseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeSenseTheme {
                SafeSenseNavGraph()
            }
        }
    }
}