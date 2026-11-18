package com.canhamp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource

private val Phosphor = Color(0xFF00FF41)
private val Panel = Color(0xFF101417)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Splash() }
    }
}

@Composable
private fun Splash() {
    Box(
        modifier = Modifier.fillMaxSize().background(Panel),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.app_tagline), color = Phosphor)
    }
}
