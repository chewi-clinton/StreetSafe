package com.example.safesense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint must be on every Activity that participates in Hilt injection.
// Without it, any ViewModel annotated with @HiltViewModel in this Activity will crash.

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Temporary — replaced in Phase 4 with the full NavGraph
            Text("SafeSense is alive ✅")
        }
    }
}