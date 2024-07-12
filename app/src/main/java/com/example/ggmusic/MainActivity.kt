package com.example.ggmusic


import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.benchmark.perfetto.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ggmusic.ui.theme.GGMusicTheme

class MainActivity : ComponentActivity() {
    // 请求码，用于在回调中识别是哪个权限请求的结果
    private val PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1
    private var audioFiles = mutableStateOf<List<AudioFile>>(listOf())
    private var player: Player? by mutableStateOf(null)

    private val playerViewModel: PlayerViewModel by viewModels()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 显示音频文件列表
            AudioList(audioFiles.value, viewModel = playerViewModel)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_READ_EXTERNAL_STORAGE
            )
        } else {
            // 权限已授予，可以直接获取音频文件列表
            audioFiles.value = getAudioFiles(this)

        }
    }

    override fun onStart() {
        super.onStart()
        playerViewModel.bindToMusicService(baseContext)
    }

    override fun onStop() {
        playerViewModel.unbindToMusicService(baseContext)
        super.onStop()
    }

    override fun onDestroy() {
        player?.stop()
        super.onDestroy()
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_READ_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 用户已授权读取外部存储，现在可以获取音频文件列表
                    audioFiles.value = getAudioFiles(this)
                } else {
                    // 用户拒绝了权限，根据应用需求处理此情况，例如显示提示信息
                    Toast.makeText(
                        this,
                        "未授权，获取音频失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            else -> {
                return
            }
        }
    }

    companion object {
        const val URL = "URL"
        const val TITLE = "TITLE"
        const val ARTIST = "ARTIST"
    }
}






@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioList(audioFiles: List<AudioFile>, player: Player = Player(),viewModel: PlayerViewModel) {
    var sliderPosition by remember { mutableStateOf(0f) }
    var currentMusic by remember { mutableStateOf(AudioFile()) }
    val context = LocalContext.current
    Scaffold(
        topBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0, 133, 119),
                    contentColor = Color(0, 133, 119)
                ),
                shape = MaterialTheme.shapes.small.copy(CornerSize(0))
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(text = "GGMusic", fontSize = 28.sp, color = Color.White)
                }
            }
        },
        bottomBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(10.dp),
            ) {
                Column {

                    viewModel.mService?.getCurrentPosition()?.let {
                        Slider(
                            value = it.toFloat(),
                            onValueChange = { value ->
                                viewModel.mService!!.seekTo(value.toInt())
                            },
                            modifier = Modifier.height(12.dp),
                            valueRange = 0f..currentMusic.duration.toFloat()
                        )
                    }
//                    Slider(
//                        value = player.currentPosition.toFloat(),
//                        onValueChange = {
//                            player.seekTo(it.toInt())
//                        },
//                        modifier = Modifier.height(12.dp),
//                        valueRange = 0f..currentMusic.duration.toFloat()
//                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.baseline_music_note_24),
                                contentDescription = null
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = currentMusic.title,
                                    fontSize = 20.sp,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(180.dp)
                                )
                                Text(
                                    text = currentMusic.artist,
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(150.dp)
                                )

                            }
                        }

                        IconButton(onClick = {
                            if (viewModel.mService?.isPlaying() == true) {
                                viewModel.mService?.pause()
                            } else {
                                viewModel.mService?.resume()
                             }
                        }) {
                            Image(
                                painter = painterResource(id = if (viewModel.mService?.isPlaying() == true) R.drawable.baseline_pause_circle_outline_24 else R.drawable.baseline_play_circle_outline_24),
                                contentDescription = null
                            )
                        }

                    }
                }
            }
        },

        ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            itemsIndexed(audioFiles) { index, audio ->
                AudioItem(index,
                    audio.title,
                    audio.artist,
                    audio.filePath,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = true),
                        onClick = {
                            currentMusic = audio
                            // player.play(currentMusic.filePath)

                            val serviceIntent = Intent(context, MusicService::class.java)
                            serviceIntent.putExtra(MainActivity.URL, currentMusic.filePath)
                            serviceIntent.putExtra(MainActivity.TITLE, currentMusic.title)
                            serviceIntent.putExtra(MainActivity.ARTIST, currentMusic.artist)
                            context.startService(serviceIntent)
                        }




                    )
                )
            }
        }
    }
}




