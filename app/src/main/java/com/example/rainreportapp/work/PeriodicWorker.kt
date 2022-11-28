package com.example.rainreportapp.work

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.*
import com.example.rainreportapp.WeatherData
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PeriodicWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    private lateinit var current: Calendar
    private var latitude = 0.0
    private var longitude = 0.0

    override fun doWork(): Result {
        Realm.init(applicationContext)
        latitude = inputData.getDouble("latitude",0.0)
        longitude = inputData.getDouble("longitude",0.0)
        weatherGetter()

        //現在ミリ秒取得
        current = Calendar.getInstance()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.add(Calendar.HOUR_OF_DAY, 24)

        //(現時刻 ー 予約時刻)
        val nextTime = calendar.timeInMillis - current.timeInMillis

        val data = Data.Builder().apply {
            putDouble("latitude",latitude)
            putDouble("longitude",longitude)
        }.build()

        //次回の起動時間をセット
        val request = OneTimeWorkRequestBuilder<PeriodicWorker>()
            .setInitialDelay(nextTime, TimeUnit.MILLISECONDS)
            .addTag("weather-work")
            .setInputData(data)
            .build()

        //Worker送信
        WorkManager.getInstance(applicationContext).enqueue(request)

        return Result.success()
    }

    @SuppressLint("SimpleDateFormat")
    @OptIn(DelicateCoroutinesApi::class)
    fun weatherGetter(): Job = GlobalScope.launch {
        val key = "9c5b8afab877ebe11f361113ac477602"
        val url = URL("https://api.openweathermap.org/data/2.5/onecall?lat=$latitude&lon=$longitude&units=metric&lang=ja&APPID=$key")


        //APIから情報を取得する.
        val br = BufferedReader(InputStreamReader(withContext(Dispatchers.IO) {
            url.openStream()
        }))
        //取得した情報を文字列に変換
        val str = br.readText()
        //json形式のデータとして識別
        val json = JSONObject(str)
        //時間別の配列を取得
        val daily = json.getJSONArray("daily")
        //一週間分の天気を取得
        for (i in 0..6) {
            //Jsonファイルから要素抽出↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
            val firstObject = daily.getJSONObject(i)
            val weather = firstObject.getJSONArray("weather").getJSONObject(0)
            val temp = firstObject.getJSONObject("temp")

            val dt = firstObject.getLong("dt")
            val date = Date(dt * 1000)
            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
            //↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑

            //Text変換
            val dateText = dateFormat.format(date)
            val descriptionText = weather.getString("description")
            val tempText = temp.getString("day")

            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                //Realm内にすでに同じ日付が保存されているか
                if (0 == realm.where<WeatherData>().equalTo("dt",dateText).findAll().size) {
                    //新規保存
                    val id = realm.where<WeatherData>().max("id")
                    val nextId = (id?.toLong() ?: 0) + 1
                    val realmObject = realm.createObject<WeatherData>(nextId)
                    realmObject.dt = dateText
                    realmObject.weather = descriptionText
                    realmObject.temp = tempText
                    realm.insert(realmObject)
                } else {
                    //上書き保存
                   val realmObject = realm.where<WeatherData>().equalTo("dt",dateText).findFirst()
                    realmObject?.weather = descriptionText
                    realmObject?.temp = tempText
                }

            }

        }
    }
}
