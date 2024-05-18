package com.keshav.capturesposed

import android.app.Activity.ScreenCaptureCallback
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keshav.capturesposed.ui.theme.APPTheme
import com.keshav.capturesposed.utils.PrefsUtils
import com.keshav.capturesposed.utils.XposedChecker

class MainActivity : ComponentActivity() {

    private var counter = mutableIntStateOf(0)
    private lateinit var isSwitchOn: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_APP)
        super.onCreate(savedInstanceState)
        PrefsUtils.loadPrefs()
        isSwitchOn = mutableStateOf(PrefsUtils.isHookOn())
        PrefsUtils.getHookActiveAsLiveData().observe(this) { isActive ->
            isActive?.let {
                isSwitchOn.value = it
            }
        }

        setContent {
            APPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
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

    @Composable
    fun AppUI(counter: Int, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CaptureSposed",
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Switch(
                    enabled = XposedChecker.isEnabled(),
                    checked = isSwitchOn.value,
                    onCheckedChange = {
                        PrefsUtils.toggleHookState()
                    },
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = "Counter: $counter",
                fontSize = 40.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(50.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}