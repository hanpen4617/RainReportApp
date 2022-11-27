package com.example.rainreportapp


import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.example.rainreportapp.databinding.ActivityTimeSetBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class IntentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimeSetBinding
    private val setTimer = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeSetBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.timeSetButton.setOnClickListener {
            //ダイアログの起動
            showDialog()
        }

        //ワーカーの起動
        binding.send.setOnClickListener {
            val current = Calendar.getInstance()
            //現在時刻から指定時間までの時間を計算
            val timeDiff = setTimer.timeInMillis - current.timeInMillis
            val data = Data.Builder().apply {
                putInt("hour",setTimer.get(Calendar.HOUR_OF_DAY))
                putInt("min",setTimer.get(Calendar.MINUTE))
            }.build()
            //ワーカーをセット
            current.add(Calendar.MILLISECOND,timeDiff.toInt())
            println(current.time)
            val request = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .addTag("weatherwork")
                .setInputData(data)
                .build()

            val manager = WorkManager.getInstance(this)
            manager.enqueue(request)
            println("Woker起動")
        }

        //ワーカーの停止
        binding.stop.setOnClickListener{
            WorkManager.getInstance(this).cancelAllWorkByTag("weatherwork")
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
            binding.timeText.setText(SimpleDateFormat("HH:mm").format(setTimer.time))
        }
        TimePickerDialog(this, timeSetListener, setTimer.get(Calendar.HOUR_OF_DAY),
            setTimer.get(Calendar.MINUTE), true).show()

    }
}