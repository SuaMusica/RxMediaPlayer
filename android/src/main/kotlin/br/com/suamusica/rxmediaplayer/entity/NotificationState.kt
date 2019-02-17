package br.com.suamusica.rxmediaplayer.entity

import android.app.Notification
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState

data class NotificationState(val notification: Notification, val mediaState: MediaBoundState)