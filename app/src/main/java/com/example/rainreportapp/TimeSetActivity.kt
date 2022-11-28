package com.example.rainreportapp


import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.example.rainreportapp.databinding.ActivityTimeSetBinding
import com.example.rainreportapp.work.NotificationWorker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class TimeSetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimeSetBinding
    private val setTimer = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeSetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val latitude = intent.getDoubleExtra("latitude",0.0)
        val longitude = intent.getDoubleExtra("longitude",0.0)
        binding.timeSetButton.setOnClickListener {
            //ダイアログの起動
            showDialog()
        }

        //ワーカーの起動
        binding.sendButton.setOnClickListener {
            val current = Calendar.getInstance()
            //現在時刻から指定時間までの時間を計算
            val delay = setTimer.timeInMillis - current.timeInMillis
            val data = Data.Builder().apply {
                putInt("hour",setTimer.get(Calendar.HOUR_OF_DAY))
                putInt("min",setTimer.get(Calendar.MINUTE))
                putDouble("latitude",latitude)
                putDouble("longitude",longitude)
            }.build()
            //ワーカーをセット
            current.add(Calendar.MILLISECOND,delay.toInt())
            println(current.time)
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("weather-work")
                .setInputData(data)
                .build()

            val manager = WorkManager.getInstance(this)
            manager.enqueue(request)
            println("Worker起動")
        }

        //ワーカーの停止
        binding.workStopButton.setOnClickListener{
            WorkManager.getInstance(this).cancelAllWorkByTag("weather-work")
        }
    }


    @SuppressLint("SimpleDateFormat")
    fun showDialog(){
        //リスナーの設定
        val timeSetListener = TimePickerDialog.OnTimeSetListener{timePicker, hour, min ->
            setTimer.set(Calendar.HOUR_OF_DAY, hour)
            setTimer.set(Calendar.MINUTE, min)
            setTimer.set(Calendar.SECOND, 0)
            //EditTextに選択された時間を設定
            binding.timeText.text = (SimpleDateFormat("HH:mm").format(setTimer.time))
        }
        TimePickerDialog(this, timeSetListener, setTimer.get(Calendar.HOUR_OF_DAY),
            setTimer.get(Calendar.MINUTE), true).show()

    }
}