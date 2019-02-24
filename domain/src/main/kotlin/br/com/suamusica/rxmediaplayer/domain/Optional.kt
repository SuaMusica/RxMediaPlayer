package br.com.suamusica.rxmediaplayer.domain

import java.util.NoSuchElementException
import java.util.Objects

class Optional<T> {

  private var value: T? = null

  private constructor() {
    this.value = null
  }

  private constructor(value: T) {
    this.value = Objects.requireNonNull(value)
  }

  fun isPresent() = value != null

  fun get(): T = value?.let { it } ?: throw NoSuchElementException("No value present")

  companion object {

    fun <T> empty(): Optional<T> = Optional()

    fun <T> of(value: T): Optional<T> = Optional(value)

    fun <T> ofNullable(value: T?): Optional<T> = value?.let { Optional(it) } ?: Optional()
  }

}