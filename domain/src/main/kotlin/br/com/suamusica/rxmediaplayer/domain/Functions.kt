package br.com.suamusica.rxmediaplayer.domain

import java.util.*

tailrec fun <T> LinkedList<T>.getRandomElement(ignore: T? = null): T? {
  if (this.isEmpty()) return null

  if (this.size == 1)
    return if (this.peek() != ignore) this.peek() else null

  val randomIndex = Random().nextInt(this.size)

  val element = this[randomIndex]

  return when (ignore) {
    null -> element
    element -> getRandomElement(ignore = element)
    else -> element
  }
}

fun <T> List<T>.getMappingReferenceIndex(referenceItem: T, mapReferenceIndex: (Int) -> Int): T? {
  val currentIndex = indexOf(referenceItem)
  val index = mapReferenceIndex(currentIndex)
  return when (index) {
    in 0 until size -> get(index)
    else -> null
  }
}
