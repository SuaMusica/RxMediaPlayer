package br.com.suamusica.rxmediaplayer

import br.com.suamusica.rxmediaplayer.domain.MediaItem


class MediaRepository {
  companion object {
    val ITEMS = listOf(
        MediaItem(
            name = "Onde Há Fumaca Há Fogo",
            author = "Cleber e Cauan",
            coverUrl = "https:\\/\\/images.suamusica.com.br\\/BhBN-RC4-w8r6PuY0W_wbc3JPDs=\\/240x240\\/3988508\\/1997513\\/cd_cover.jpg",
            url = "https:\\/\\/files.suamusica.com.br\\/3988508\\/1997513\\/01%20-%20Onde%20H%C3%A1%20Fumaca%20Ha%20Fogo.mp3?Expires=1513048967&Signature=gZbvBUMec~MtSFjDTjCRlP~E7uVaJx3OXvdbxhx7-zxWYgUjfXODmZGtnh5FmhGZ2HWk7f5kO5M5r06KiTu3LAW1WxL7qNGkgYFYjMEvX8kfeZwDvk-xq-bcsd8o5By-5D--UZbw6eBlUiU~lVTP7uKlJCVvqnL5HEG~PPEfYaoqd~sjtGISoruFVuBmI-ghEBC2IDnFXjIlLUNecjzJc21BhWh7D7NnOpiAvUmwGoN6gckoB0uti~I8cnUa0IaFQ7Xn46omYuYN0Llje7xefJMHmKuC2DO5OKmJo1VMvCDdJSPxhcXYA8j0sYRRtor7KNNoqazB5feRychwubMAQA__&Key-Pair-Id=APKAJXI72ADZRFKVIR2Q"
        ),
        MediaItem(
            name = "Debaixo do Tapete",
            author = "Cleber e Cauan",
            coverUrl = "https:\\/\\/images.suamusica.com.br\\/BhBN-RC4-w8r6PuY0W_wbc3JPDs=\\/240x240\\/3988508\\/1997513\\/cd_cover.jpg",
            url = "https:\\/\\/files.suamusica.com.br\\/3988508\\/1997513\\/02%20-%20Debaixo%20do%20Tapete.mp3?Expires=1513048967&Signature=E82C3o0LmIdQ-vnA9pVyoB08ijWe59MvLU5zKTSAVPElF5KTuDC0i-8OQglXBECeELQAvdnc34nQPA49bcA0ImfMP8mABJD6qpb6falpgHwWU5G2Q7FvuIedJuOQwPuGQWw-vMxuD4FGhq7g9y9raLbowVjtn5nH09x2mCySFsxaHuEfgyWK1UB9EgOLdcvtvJaGxbk3QpNirMCUXHtAhuFoU8mYEaGz7M-bY7qrhxaSwwrWjyDvoWIJz2npRuYMcUOJ1-QMwddgA~ACx17e7GRf8N6~WpmEQ~P1q5o0Y5cHkNWWY9xYIwVtHeYKBWeLiTbiyisxU~Thik-b1~e1bA__&Key-Pair-Id=APKAJXI72ADZRFKVIR2Q"
        ),
        MediaItem(
            name = "1 2 3",
            author = "Cleber e Cauan",
            coverUrl = "https:\\/\\/images.suamusica.com.br\\/BhBN-RC4-w8r6PuY0W_wbc3JPDs=\\/240x240\\/3988508\\/1997513\\/cd_cover.jpg",
            url = "https:\\/\\/files.suamusica.com.br\\/3988508\\/1997513\\/03%20-%201%202%203.mp3?Expires=1513048967&Signature=AhAeqF5KFAesL-RDYf9Xi29r9KDIWJMHjPgUY4cAw~1X7fW30nzIRwRQ78Vbnw1mj5YWvui1K8hNXHN9CYdjE8ISAGeSGBUGgIepM7X8LHt312GShazGJgqjVOAvy7wz2GrfxvpRn3cZwiTJUUvf558R14VMkRZ4ruqdoo9YnsjcOveSs1iDC31dfWdVfuW4lHjNApL3KOns0MqXc5ijjkEqCA~LB0LeHd2OJRM99D5tiEOCgAwWd00LHUOk1WhspTt7jWI7HUGvcFYJzD7~DmGA6FQi8VOmV5oc6kwvdUZLt-cS2fNP8jZWkKwszfwPmxSka93tWD2Ka5IqK-KZhw__&Key-Pair-Id=APKAJXI72ADZRFKVIR2Q"
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