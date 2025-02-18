package com.example.digitaltablet.util

import android.os.Build
import androidx.annotation.RawRes
import com.example.digitaltablet.BuildConfig
import com.example.digitaltablet.R

object Constants {

    object Mqtt {

        const val BROKER_URL = "tcp://mqtt1.rcsl.online:1883"

        const val BROKER_NAME = "NTNU-MQTT-Server-1"

        const val USERNAME = "rcsl"

        const val PASSWORD = "rcslmqtt"

        object Topic {

            const val TTS = "$BROKER_NAME/mqtt/TTS/{{deviceId}}"

            const val STT = "$BROKER_NAME/mqtt/STT/{{deviceId}}"

            const val IMAGE = "$BROKER_NAME/mqtt/image/{{deviceId}}"

            const val ARGV = "$BROKER_NAME/mqtt/argv/{{deviceId}}"

            const val TEXT_INPUT = "$BROKER_NAME/mqtt/TextInput/{{deviceId}}"

            const val RESPONSE = "$BROKER_NAME/mqtt/response/{{deviceId}}"

            const val GET_CATEGORY = "{{deviceId}}/getRobotCategory/response"

            const val API_KEY = "$BROKER_NAME/mqtt/Apikey/{{deviceId}}"

            const val ASST_ID = "$BROKER_NAME/mqtt/AsstId/{{deviceId}}"

            const val SEND_IMAGE = "$BROKER_NAME/mqtt/SendImage/{{deviceId}}"

            const val SEND_FILE = "$BROKER_NAME/mqtt/SendFile/{{deviceId}}"

            const val TABLET_QR = "$BROKER_NAME/mqtt/tabletqr/{{deviceId}}"

            const val TABLET = "{{deviceId}}-tablet"

            const val ROBOT = "{{deviceId}}"

            const val ROBOT_TOAST = "$BROKER_NAME/mqtt/RobotToast/{{deviceId}}"

        }

    }

    object LanguageModel {

        const val BASE_URL = "https://api.openai.com/v1/"

        val PROJECTS = mapOf(
            "AI-based Research" to "",
            "Chinese Language" to "",
            "Creativity with Chinese Language" to "",
            "Kindergarten" to "",
            "Making prompts invisible" to "",
            "Order people care" to "",
            "Robot Storyteller" to "",
            "Social emotional learning" to "",
            "STEM Education" to "",
            "Testing Agents" to ""
        )
    }

    object Rcsl {

        const val BASE_URL = "https://api.rcsl.online/"

    }

}