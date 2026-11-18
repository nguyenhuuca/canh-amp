package com.canhamp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.canhamp.ui.AppNavGraph
import com.canhamp.ui.theme.CanhAmpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        (application as CanhAmpApp).container.playerConnection.connect()
        setContent { CanhAmpTheme { AppNavGraph() } }
    }
}
