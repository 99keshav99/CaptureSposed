package com.keshav.capturesposed

import android.app.Activity.ScreenCaptureCallback
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.keshav.capturesposed.ui.theme.APPTheme

class MainActivity : ComponentActivity() {
    private var counter = mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_APP)
        super.onCreate(savedInstanceState)
        setContent {
            APPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AppUI(counter.intValue)
                }
            }
        }
    }

    private val screenCaptureCallback = ScreenCaptureCallback {
        counter.intValue++
    }

    override fun onStart() {
        super.onStart()
        registerScreenCaptureCallback(mainExecutor, screenCaptureCallback)
    }

    override fun onStop() {
        super.onStop()
        unregisterScreenCaptureCallback(screenCaptureCallback)
    }
}

@Composable
fun AppUI(counter: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "APP!",
            fontSize = 160.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Counter: $counter",
            fontSize = 50.sp,
            textAlign = TextAlign.Center
        )
    }
}