package br.com.suamusica.rxmediaplayer.extensions

import java.util.*

fun <A, B> Queue<Pair<A, B>>.add(a: A, b: B) {
  this.add(a to b)
}
