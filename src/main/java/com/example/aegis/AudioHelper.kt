package com.example.aegis

import android.content.Context
import android.media.MediaRecorder
import android.widget.Toast

object AudioHelper {

    private var recorder: MediaRecorder? = null

    fun startRecording(context: Context) {
        try {
            val fileName = "recording_${System.currentTimeMillis()}.3gp"
            val filePath = context.filesDir.absolutePath + "/" + fileName

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(filePath)
                prepare()
                start()
            }

            Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
            Toast.makeText(context, "Saved at: $filePath", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}