package com.meteors.android.launchertest

import android.app.Service
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import kotlin.concurrent.thread


class MyService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        thread {
            try {
                val progress = Runtime.getRuntime().exec(arrayOf("logcat", "-d"))
                val reader = BufferedReader(InputStreamReader(progress.inputStream))

                val log = StringBuilder()
                val separator = System.getProperty("line.separator")
                var line = reader.readLine()
                while(line != null){
                    Log.d("Test1", line)
                    //log.append(line);
                    //log.append(separator)
                    line = reader.readLine()
                }
                var w = log.toString()
                Toast.makeText(applicationContext, w, Toast.LENGTH_SHORT).show()
            }catch (e:Exception){
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }


        return super.onStartCommand(intent, flags, startId)
    }

}