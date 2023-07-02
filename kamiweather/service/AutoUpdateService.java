package com.yye.kamiweather.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.yye.kamiweather.gson.AQI;
import com.yye.kamiweather.util.HttpUtil;
import com.yye.kamiweather.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    private static final long WEATHER_UPDATE_INTERVAL = 8 * 60 * 60 * 1000; // 8小时毫秒数
    private static final long PIC_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // 24小时毫秒数
    private Handler mHandler;
    private Runnable mWeatherUpdateRunnable;
    private Runnable mPicUpdateRunnable;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mWeatherUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateWeather();
                mHandler.postDelayed(this, WEATHER_UPDATE_INTERVAL);
            }
        };
        mPicUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updatePic();
                mHandler.postDelayed(this, PIC_UPDATE_INTERVAL);
            }
        };
    }


    public int OnStartCommand(Intent intent,int flags,int startId){
        if (intent != null) {
            if (intent.getBooleanExtra("pic",true)) {
                updatePic();
            }
            if(intent.getBooleanExtra("data",true)){
                updateWeather();
            }
        }
        // 启动定时任务
        mHandler.postDelayed(mWeatherUpdateRunnable, WEATHER_UPDATE_INTERVAL);
        mHandler.postDelayed(mPicUpdateRunnable, PIC_UPDATE_INTERVAL);

        return super.onStartCommand(intent,flags,startId);
    }

    //天气信息更新   此处需自己填写APIKEY
    private void updateWeather(){
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String forecast=prefs.getString("sw_7Forecast","3");
        String id=prefs.getString("weather_Id",null);
        String key="&key="+"你的和风天气的密钥";
        RequestHourForecastS(id,key);
        RequestForecastS(id,forecast,key);
        RequestAllDataS(id,key);
    }

    //api 24小时预测
    public void RequestHourForecastS(String ID,String APIkey){
        String HourForecastURL="https://devapi.qweather.com/v7/weather/24h?location="+ID+APIkey;
        Thread API4=new Thread(){
            @Override
            public void run() {
                super.run();
                HttpUtil.sendOkHttpRequest(HourForecastURL, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("hour_forecast", responseText);
                        editor.apply();
                    }
                });
            }
        };
        API4.start();
    }

    //api3天预测
    private void RequestForecastS(String ID,String day,String APIkey) {
        // 在这里开启线程进行网络请求，然后调用解析方法
        // 返回解析结果的布尔值，表示解析是否成功
        String ForecastURL="https://devapi.qweather.com/v7/weather/+"+day+"d?location="+ID+APIkey;
        Thread API3=new Thread(){
            @Override
            public void run() {
                super.run();
                HttpUtil.sendOkHttpRequest(ForecastURL, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                       e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                                AutoUpdateService.this
                        ).edit();
                        editor.putString("forecast", responseText);
                        editor.apply();
                    }
                });
            }
        };
        API3.start();
    }

    //api 获取天气和AQI
    public void RequestAllDataS(String ID,String APIkey){
        //几个API,开几个线程
        String WeatherURL="https://devapi.qweather.com/v7/weather/now?location="+ID+APIkey;
        String AQIURL="https://devapi.qweather.com/v7/air/now?location="+ID+APIkey;
        Thread API1=new Thread() {
            @Override
            public void run() {
                super.run();
                HttpUtil.sendOkHttpRequest(WeatherURL, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                                AutoUpdateService.this
                        ).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }
                });
            }
        };
        API1.start();
        Thread API2=new Thread() {
            @Override
            public void run() {
                super.run();
                HttpUtil.sendOkHttpRequest(AQIURL, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        AQI aqi = new AQI();
                        boolean success=Utility.handleNewAPIAQIResponse(responseText,aqi);
                        if (success){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                                            AutoUpdateService.this
                            ).edit();
                            editor.putString("aqi", responseText);
                            editor.putString("AQI_aqi", aqi.getAqi());
                            editor.putString("AIQ_pm2p5", aqi.getPm2p5());
                            editor.apply();
                        }
                    }
                });
            }
        };
        API2.start();
    }

    //自动更新图片
    private void updatePic(){
        try {
            //必应每日一图
            InputStream inputStream = new URL("http://bing.ioliu.cn/v1/rand?h=1920&w=1080").openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            //将位图转换为字节数组
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] imageData = byteArrayOutputStream.toByteArray();
            //将字节数组转换为Base64编码字符串
            String imageBase64 = Base64.encodeToString(imageData, Base64.DEFAULT);
            //持久化存储
            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString("randomPic_code", imageBase64);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
            updatePic();//遇到错误再次尝试获取
        }
    }
}