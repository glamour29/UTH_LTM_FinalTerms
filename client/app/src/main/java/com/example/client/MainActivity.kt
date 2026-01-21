package com.example.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.client.view.navigation.AppNavigation
import com.example.client.view.theme.ClientTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClientTheme {
                AppNavigation()
            }
        }
    }
}