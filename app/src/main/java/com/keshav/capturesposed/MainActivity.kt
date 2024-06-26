package com.keshav.capturesposed

import android.app.Activity.ScreenCaptureCallback
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import com.keshav.capturesposed.ui.theme.APPTheme
import com.keshav.capturesposed.utils.PrefsUtils
import com.keshav.capturesposed.utils.XposedChecker

class MainActivity : ComponentActivity() {

    private var counter = mutableIntStateOf(0)
    private lateinit var isSwitchOn: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_APP)
        super.onCreate(savedInstanceState)
        counter.intValue = savedInstanceState?.getInt("counter") ?: 0
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
                    color = MaterialTheme.colorScheme.surfaceContainer
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("counter", counter.intValue)
    }

    override fun onStop() {
        super.onStop()
        unregisterScreenCaptureCallback(screenCaptureCallback)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppUI(counter: Int, modifier: Modifier = Modifier) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.xposed_name),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            modifier = modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxSize()
        ) { p ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(p)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                MainCard(isSwitchOn)
                TestCard()
            }
        }
    }

    @Composable
    fun MainCard(isSwitchOn: MutableState<Boolean>) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()){
            Column(modifier = Modifier.padding(16.dp)){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ){
                    if(XposedChecker.isEnabled() && isSwitchOn.value){
                        Icon(painterResource(R.drawable.okay_24), getString(R.string.running))
                    }else if (XposedChecker.isEnabled() && !isSwitchOn.value){
                        Icon(painterResource(R.drawable.disabled_24), getString(R.string.stopped))
                    }else{
                        Icon(painterResource(R.drawable.error_24), getString(R.string.error))
                    }
                    Text(getString(R.string.status_title), fontSize = 24.sp)
                }
                if (XposedChecker.isEnabled()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ){
                        if(isSwitchOn.value){
                            Text(
                                text = getString(R.string.status_running),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }else{
                            Text(
                                text = getString(R.string.status_stopped),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = isSwitchOn.value,
                            onCheckedChange = { PrefsUtils.toggleHookState() },
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Text(
                        text = getString(LocalContext.current, R.string.module_disabled),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun TestCard(){
        OutlinedCard(modifier = Modifier.fillMaxWidth()){
            Column(modifier = Modifier.padding(16.dp)){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ){
                    Icon(painterResource(R.drawable.checklist_24), getString(R.string.card_title_testing))
                    Text(getString(R.string.card_title_testing), fontSize = 24.sp)
                }
                Text(
                    text = "Counter: ${counter.intValue}",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}