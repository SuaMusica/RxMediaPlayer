package br.com.suamusica.rxmediaplayer.domain

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class RxMediaServiceImplTest {

  private val mediaPlayer: RxMediaPlayer = mock()
  private val testScheduler = TestScheduler()
  private val rxMediaService: RxMediaService = RxMediaService.create(mediaPlayer, testScheduler)

  @Test
  fun `add one media item to queue - should complete successfully and list media added`() {
    // GIVEN
    val mediaItem = fakeMediaItem()

    // WHEN
    val addObserver = rxMediaService.add(mediaItem).test()
    testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
    val queueObserver = rxMediaService.queue().test()
    testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

    // THEN
    addObserver.assertComplete()

    queueObserver.assertComplete()
    queueObserver.assertResult(mediaItem)
    queueObserver.assertValueCount(1)
  }

  @Test
  fun `add ten media items to queue - should complete successfully and list media added`() {
    // GIVEN
    val tenMediaItems = (1..10).map { fakeMediaItem() }

    // WHEN
    val addObserver = rxMediaService.add(tenMediaItems).test()
    testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
    val queueObserver = rxMediaService.queue().test()
    testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

    // THEN
    addObserver.assertComplete()

    queueObserver.assertComplete()
    queueObserver.assertResult(*tenMediaItems.toTypedArray())
    queueObserver.assertValueCount(10)
  }

  @Test
  fun remove() {
  }

  @Test
  fun removeAll() {
  }

  @Test
  fun reorder() {
  }

  @Test
  fun queue() {
  }

  @Test
  fun changeRandomState() {
  }

  @Test
  fun isRandomized() {
  }

  @Test
  fun play() {
  }

  @Test
  operator fun next() {
  }

  @Test
  fun previous() {
  }

  @Test
  fun pause() {
  }

  @Test
  fun stop() {
  }

  @Test
  fun status() {
  }
}