package br.com.suamusica.rxmediaplayer

import br.com.suamusica.rxmediaplayer.domain.MediaItem


class MediaRepository {
  companion object {
    val ITEMS = listOf(
        MediaItem(
            name = "Onde Há Fumaca Há Fogo",
            author = "Cleber e Cauan",
            coverUrl = "https:\\/\\/images.suamusica.com.br\\/BhBN-RC4-w8r6PuY0W_wbc3JPDs=\\/240x240\\/3988508\\/1997513\\/cd_cover.jpg",
            url = "http:\\/\\/www.kozco.com\\/tech\\/piano2-CoolEdit.mp3"
        ),
        MediaItem(
            name = "Debaixo do Tapete",
            author = "Cleber e Cauan",
            coverUrl = "https:\\/\\/images.suamusica.com.br\\/BhBN-RC4-w8r6PuY0W_wbc3JPDs=\\/240x240\\/3988508\\/1997513\\/cd_cover.jpg",
            url = "http:\\/\\/www.kozco.com\\/tech\\/piano2-Audacity1.2.5.mp3"
        ),
        MediaItem(
            name = "1 2 3",
            author = "Cleber e Cauan",
            coverUrl = "https:\\/\\/images.suamusica.com.br\\/BhBN-RC4-w8r6PuY0W_wbc3JPDs=\\/240x240\\/3988508\\/1997513\\/cd_cover.jpg",
            url = "http:\\/\\/www.kozco.com\\/tech\\/organfinale.mp3"
        ),
        MediaItem(
            name = "Quase - Sonho",
            author = "Cleber e Cauan",
            coverUrl = "https:\\/\\/images.suamusica.com.br\\/BhBN-RC4-w8r6PuY0W_wbc3JPDs=\\/240x240\\/3988508\\/1997513\\/cd_cover.jpg",
            url = "https:\\/\\/files.suamusica.com.br\\/3988508\\/1997513\\/04%20-%20Quase%20-%20Sonho.mp3?Expires=1513048967&Signature=H6uExKGlEtJDB-YD4kQ6Z5G0zAaO2lk9bGmysaTURphJ~xPcTQaxIfNf1Pqn-Ye6nDyZPDsLGeFbC5jlFJY4MoFqCqJVq17djY1cfWdu~dU5h9wFtr3rC5aUEzMKnTzrB27H4Wzl2pITZ7N9YSQVNs7KW1TfLK6db--giDUQd2S9NNU~70e~YHvgRQsVsyHw~VW~KjErOHX1P9MW9M1PlWPffn6QFerailqPh8QlGdm8kFyF7WrE0lmGPnwc0UKv4Hvd6g6mJX4vtHqTUqlBajK9qj7vqtdTbrOLzn28hpMmTPSX1NSzeqWT9CbbEAoVpAVb4v6ZPgT0cn5E-FLlbw__&Key-Pair-Id=APKAJXI72ADZRFKVIR2Q"
        ),
        MediaItem(
            name = "E Tudo Nosso",
            author = "Cleber e Cauan",
            coverUrl = "https:\\/\\/images.suamusica.com.br\\/BhBN-RC4-w8r6PuY0W_wbc3JPDs=\\/240x240\\/3988508\\/1997513\\/cd_cover.jpg",
            url = "https:\\/\\/files.suamusica.com.br\\/3988508\\/1997513\\/05%20-%20E%20Tudo%20Nosso.mp3?Expires=1513048967&Signature=B1DqKJc4U7A3QUSQzBlu1l20FzF8EzTi1CIpNCGyeyGd7xRCIs~vdj-t3bAAxI3ojr7OSZQqBmB0FiDTSGr~oR0641AlRYgeFfpgsagFdeubV3vIVqcFqSDjwbhrwfJ04ckMk6l1d6b8U0O4YYtAcTatbOrzU16mqavLjLcnspB52t46vk4Z1zHFLQIdYDKd5ww270DX37CgVlPfxygWdmUmrtJ070qjqRKY5Xxp3x9mST~lGAVq7B2r3fymgClcLAUkcyO3yIgpYLVI2C5GghR2AYsALoot5v1LTx1idFV1VF2YxBgRWF~z55yuWXGQS9kYa02iSIWp7CmETJBYHQ__&Key-Pair-Id=APKAJXI72ADZRFKVIR2Q"
        )
    )
  }
}