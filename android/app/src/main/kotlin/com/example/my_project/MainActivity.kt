package com.mycompany.servicetest

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class ChannelHandler(private val context: Context, flutterEngine: FlutterEngine) {
    private val methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "channel-name")
    @RequiresApi(Build.VERSION_CODES.M)
    fun setMethodCallHandler() {
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "getNetworkStatus" -> {
                    val networkStatus = getNetworkStatus()
                    result.success(networkStatus)
                }
                else -> result.notImplemented()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ServiceCast")
    private fun getNetworkStatus(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return when {
            capabilities == null -> "No connection"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Connected to Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected to Cellular"
            else -> "Unknown connection"
        }
    }
}

class MainActivity: FlutterActivity() {
    val methodChannel = flutterEngine?.dartExecutor?.let { MethodChannel(it.binaryMessenger, "channel-name") }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val channelHandler = ChannelHandler(this, flutterEngine)
        channelHandler.setMethodCallHandler()


    }
}
