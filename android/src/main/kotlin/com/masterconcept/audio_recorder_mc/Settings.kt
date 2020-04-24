package com.masterconcept.audio_recorder_mc

import android.media.AudioFormat

object Settings {
    object Microfone {
        val taxaAmostragemReal = 44100
        val taxaAmostragemEfetiva = 8820
        val canais = AudioFormat.CHANNEL_IN_MONO
        val codificacao = AudioFormat.ENCODING_PCM_FLOAT
    }
}