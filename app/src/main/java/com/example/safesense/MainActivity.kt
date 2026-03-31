package com.example.safesense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.safesense.data.preferences.UserPreferencesRepository
import com.example.safesense.ui.navigation.SafeSenseNavGraph
import com.example.safesense.ui.theme.SafeSenseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeSenseTheme {
                SafeSenseNavGraph(userPreferencesRepository = userPreferencesRepository)
            }
        }
    }
}
