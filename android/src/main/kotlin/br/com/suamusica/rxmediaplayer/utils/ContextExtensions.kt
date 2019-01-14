package br.com.suamusica.rxmediaplayer.utils

import android.content.Context
import android.net.ConnectivityManager

fun Context.isConnected(): Boolean {
  return (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
      .activeNetworkInfo?.isConnected ?: false
}