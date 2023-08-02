package com.devgeekssolutions.ktorclient

import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.devgeekssolutions.ktorclient.ui.theme.KtorClientTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    private lateinit var client: HttpClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client = provideKtorClient()

        lifecycleScope.launch {
            downloadImage(
                "background",
                "https://console-uploads-staging.s3.eu-west-1.amazonaws.com/images/174010_1612439179_92a68673-80c8-4379-a652-d2a710265908.png"
            )
        }

        setContent {
            KtorClientTheme {
                // A surface container using the 'background' color from the theme
                val painter = rememberAsyncImagePainter(
                    model = File(
                        applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path + "/background"
                    )
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                applicationContext.getExternalFilesDir(
                                    Environment.DIRECTORY_PICTURES
                                )?.path + "/background"
                            )
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private suspend fun downloadImage(imageName: String, imgUrl: String) {
        val file = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path + "/$imageName"
        )
        downloadBackgroundImage(
            imgUrl
        ).copyAndClose(
            file.writeChannel()
        )
    }

    suspend fun downloadBackgroundImage(imageUrl: String) = client.get(imageUrl) {
        headers.remove(HttpHeaders.Authorization)
        onDownload { bytesSentTotal, contentLength ->
            Log.d(
                "BackgroundImageDownload",
                "Received $bytesSentTotal bytes from $contentLength"
            )
        }
    }.bodyAsChannel()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KtorClientTheme {
        Greeting("Android")
    }
}

internal fun provideKtorClient(): HttpClient {
    return HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )

            install(Logging) {
                level = LogLevel.ALL
            }
        }
    }
}
