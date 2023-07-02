package com.yye.kamiweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextDirectionHeuristic;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.yye.kamiweather.gson.AQI;
import com.yye.kamiweather.gson.Forecast;
import com.yye.kamiweather.gson.Hour_forecast;
import com.yye.kamiweather.gson.Weather;
import com.yye.kamiweather.service.AutoUpdateService;
import com.yye.kamiweather.util.HttpUtil;
import com.yye.kamiweather.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public SwipeRefreshLayout swipeRefresh;
    private final String APIkey = "&key="+"你的和风天气的密钥";//APIKEY
    private int API_Times=0;//api请求计数，防止过于频繁
    private String county_title;
    private ProgressDialog progressDialog;
    private ScrollView weatherLayout;
    private ImageView randomPic;
    //title.xml
    private TextView titleCity,titleUpdateTime;
    //now.xml
    private TextView degree,text,feelsLike,humidity,windScale,pressure;
    //forecast.xml
    private List<Forecast> forecastList = new ArrayList<>();
    //hour_forecast.xml
    private List<Hour_forecast> HourforecastList=new ArrayList<>();
    //aqi.xml
    private TextView aqiText,pm25Text;
    //website.xml     button  跳转设置界面
    private Button ToWebsite,Settings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        //初始化各控件 并检测是否第一次打开或者有新的id和地区名字
        InitAll();
        //对持久化的数据进行读取
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //读取设置
        boolean lp=prefs.getBoolean("sw_LoadPic",false);
        boolean ups=prefs.getBoolean("sw_UpdatePicService",true);
        boolean uds=prefs.getBoolean("sw_UpdateDataService",true);
        String forecast=prefs.getString("sw_7Forecast","3");
        boolean hf=prefs.getBoolean("sw_Hour_Forecast",true);
        //读取响应体
        String weatherResponse = prefs.getString("weather", null);
        String forecastResponse = prefs.getString("forecast", null);
        String hourForecastResponse=prefs.getString("hour_forecast",null);
        //读取存储的图片
        String randomPic_code = prefs.getString("randomPic_code", null);
        //额外赋值
        county_title = prefs.getString("county_Name", null);//读取被持久化的数据
        //据设置决定开启后台更新什么
        MyServiceStart(ups,uds);
        //据图片操作
        if (randomPic_code != null) {
            //据有无图片进行操作  存在  解码base64码
            byte[] imageData = Base64.decode(randomPic_code, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            randomPic.setImageBitmap(bitmap);
        }
        //是否定位而来，是的话，直接刷新,不读缓存
        if (getIntent().getBooleanExtra("LocateRefresh",false)){
            RefreshALL(lp,prefs,forecast,hf);//定位过来的
            Toast.makeText(WeatherActivity.this,"定位成功",Toast.LENGTH_SHORT).show();
        }else {
            //有缓存时直接解析天气的数据
            if (weatherResponse != null && forecastResponse != null && hourForecastResponse != null) {
                Weather weather = (Weather) Utility.handleNewAPIWeatherResponse(weatherResponse);
                forecastList = Utility.handleNewAPIForecastResponse(forecastResponse);
                HourforecastList = Utility.handleNewAPIHourForecastResponse(hourForecastResponse);
                //展示
                showInfo(weather);
                showInfoForecast();
                showInfoHourForecast(hf);
                showInfoAQI(prefs.getString("AQI_aqi", null), prefs.getString("AIQ_pm2p5", null));
            }
        }
        ToWebsite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //打开网址   有api的值的话打开，没有默认官网
                String url = prefs.getString("websiteURL", "https://www.qweather.com/");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    Toast.makeText(WeatherActivity.this, "正在前往和风天气",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // 如果没有应用程序可以处理该 Intent，您可以根据需求执行其他操作
                    Toast.makeText(WeatherActivity.this, "打开网站失败，请检查是否有可用浏览器",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        Settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(WeatherActivity.this,ChangeCity.class);
                startActivity(intent);
                WeatherActivity.this.finish();
            }
        });


        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                RefreshALL(lp,prefs,forecast,hf);
            }
        });
    }

    //单独方法，方便刷新，（由于定位机制的引入，从定位打开该页面需要进行一次刷新）
    private void RefreshALL(boolean lp,SharedPreferences prefs,String forecast,boolean hf){
        API_Times++;
        if (API_Times<=20&&lp){
            String id=prefs.getString("weather_Id",null);
            RequestAllData(id);
            RequestForecast(id,forecast);
            if (hf){RequestHourForecast(id);}
            if (API_Times%5==0){
                //每刷新5次更新图片一次
                Toast.makeText(WeatherActivity.this, "每刷新5次更新图片一次"
                        , Toast.LENGTH_SHORT).show();
                new FetchImageTask().execute();
            }
        }
        if (API_Times<=20&&!lp){
            //不更新图片
            String id=prefs.getString("weather_Id",null);
            RequestAllData(id);
            RequestForecast(id,forecast);
            if (hf){RequestHourForecast(id);}
        }
        if(API_Times>20){
            API_Times=0;//置0
            swipeRefresh.setRefreshing(false);
            Toast.makeText(WeatherActivity.this, "请勿短时间内重复刷新"
                    , Toast.LENGTH_SHORT).show();
        }
    }

    //开启后台服务  x为图片更新  y为数据更新
    private void MyServiceStart(boolean x,boolean y){
        Intent intent=new Intent(this, AutoUpdateService.class);
        intent.putExtra("pic",x);
        intent.putExtra("data",y);
        startService(intent);
    }

    private void InitAll(){
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        randomPic = (ImageView) findViewById(R.id.randomPic);
        swipeRefresh=(SwipeRefreshLayout)findViewById(R.id.RefreshLayout);
        //title.xml
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        //now.xml
        degree = (TextView) findViewById(R.id.degree);
        text = (TextView) findViewById(R.id.tips_text);
        feelsLike = (TextView) findViewById(R.id.t1);
        humidity = (TextView) findViewById(R.id.t2);
        windScale = (TextView) findViewById(R.id.t3);
        pressure = (TextView) findViewById(R.id.t4);
        //aqi.xml
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        //website.xml
        ToWebsite = findViewById(R.id.website_btn);
        //跳转设置界面
        Settings=(Button)findViewById(R.id.setting);

        //持久化存储  存取操作  先读
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
        String weather_Id = getIntent().getStringExtra("weather_Id");
        String county_Name = getIntent().getStringExtra("county_Name");
        if (weather_Id != null && county_Name != null) {
            //防止空指针异常  同时证明为第一次开启该Activity  初始化默认设置
            editor.putBoolean("sw_LoadPic", false);
            editor.putBoolean("sw_UpdatePicService", true);
            editor.putBoolean("sw_UpdateDataService", true);
            editor.putString("sw_7Forecast", "3");
            editor.putBoolean("sw_Hour_Forecast", true);
            editor.putString("weather_Id", weather_Id);
            editor.putString("county_Name", county_Name);
            //第一次开启，请求所有API，得到天气数据
            RequestAllData(weather_Id);
            //请求预测的信息  第一次加载默认3天数据  默认开启24小时
            RequestForecast(weather_Id,"3");
            RequestHourForecast(weather_Id);
            new FetchImageTask().execute();//请求一张图片
        } else if(weather_Id == null && county_Name != null){
            //部分数据更新
            editor.putString("county_Name",county_Name);
        }else if (weather_Id != null && county_Name == null){
            editor.putString("weather_Id",weather_Id);
        }else{
            editor.apply();
        }
        editor.apply();
    }
    //解析通过api获取的图片  异步任务
    private class FetchImageTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();//显示加载条
        }
        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                //必应每日一图的API
                InputStream inputStream = new URL("http://bing.ioliu.cn/v1/rand?h=1920&w=1080").openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                //将位图转换为字节数组
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] imageData = byteArrayOutputStream.toByteArray();
                //将字节数组转换为Base64编码字符串
                String imageBase64 = Base64.encodeToString(imageData, Base64.DEFAULT);
                //持久化存储
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("randomPic_code", imageBase64);
                editor.apply();
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            closeProgressDialog(); // 关闭加载条
            if (bitmap != null) {
                closeProgressDialog();
                randomPic.setImageBitmap(bitmap);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(WeatherActivity.this, "图片更新成功"
                        , Toast.LENGTH_SHORT).show();
            } else {
                // 获取图片失败
                Toast.makeText(WeatherActivity.this, "获取图片失败 请重试", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //api 24小时预测
    public boolean RequestHourForecast(String ID){
        String HourForecastURL="https://devapi.qweather.com/v7/weather/24h?location="+ID+APIkey;
        Thread API4=new Thread(){
            @Override
            public void run() {
                super.run();
                HttpUtil.sendOkHttpRequest(HourForecastURL, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WeatherActivity.this, "获取24小时天气预测失败"
                                        , Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        HourforecastList = Utility.handleNewAPIHourForecastResponse(responseText);
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                                WeatherActivity.this
                        ).edit();
                        editor.putString("hour_forecast", responseText);
                        editor.apply();
                        // 解析完成后调用展示方法
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showInfoHourForecast(true);//请求了必定要显示
                                swipeRefresh.setRefreshing(false);
                            }
                        });
                    }
                });
            }
        };
        API4.start();
        return HourforecastList != null && !HourforecastList.isEmpty();

    }

    //api3天预测 传入 ID 和 请求的天数
    private boolean RequestForecast(String ID,String day) {
        // 在这里开启线程进行网络请求，然后调用解析方法
        // 返回解析结果的布尔值，表示解析是否成功
        String ForecastURL="https://devapi.qweather.com/v7/weather/"+day+"d?location="+ID+APIkey;
        Thread API3=new Thread(){
            @Override
            public void run() {
                super.run();
                HttpUtil.sendOkHttpRequest(ForecastURL, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WeatherActivity.this, "获取天气预测失败"
                                        , Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        forecastList = Utility.handleNewAPIForecastResponse(responseText);
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                                WeatherActivity.this
                        ).edit();
                        editor.putString("forecast", responseText);
                        editor.apply();
                        // 解析完成后调用展示方法
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showInfoForecast();
                                swipeRefresh.setRefreshing(false);
                                Toast.makeText(WeatherActivity.this, "刷新天气成功"
                                        , Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        };
        API3.start();
        return forecastList != null && !forecastList.isEmpty();
    }

    //api 获取天气和AQI
    public void RequestAllData(String ID){
        //2个API,开2个线程
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WeatherActivity.this, "获取天气信息失败"
                                        , Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        final Weather weather = (Weather) Utility.handleNewAPIWeatherResponse(responseText);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (weather != null) {
                                    //UI线程操作，持久化存储
                                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                                            WeatherActivity.this
                                    ).edit();
                                    editor.putString("weather", responseText);
                                    editor.apply();
                                    showInfo(weather);
                                } else {
                                    Toast.makeText(WeatherActivity.this, "获取天气信息失败",
                                            Toast.LENGTH_SHORT).show();
                                }
                                swipeRefresh.setRefreshing(false);
                            }
                        });
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WeatherActivity.this, "获取天气AQI失败"
                                        , Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        AQI aqi = new AQI();
                        boolean success=Utility.handleNewAPIAQIResponse(responseText,aqi);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (success != false) {
                                    //UI线程操作，持久化存储
                                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                                            WeatherActivity.this
                                    ).edit();
                                    editor.putString("aqi", responseText);
                                    editor.putString("AQI_aqi", aqi.getAqi());
                                    editor.putString("AIQ_pm2p5", aqi.getPm2p5());
                                    editor.apply();
                                    showInfoAQI(aqi);
                                }
                                swipeRefresh.setRefreshing(false);
                            }
                        });
                    }
                });
            }
        };
        API2.start();
    }

    //由于API变更，各种各样的信息分为了多个API，故用此来处理其他部分
    private void showInfo(Weather weather) {
        //title.xml
        titleCity.setText(county_title);
        titleUpdateTime.setText("更新时间：" + weather.updateTime.substring(11, 16));
        //now.xml中的赋值
        degree.setText(weather.now.temp + "℃");
        feelsLike.setText("   " + weather.now.feelsLike);
        humidity.setText("   " + weather.now.humidity);
        text.setText(weather.now.text + "  ");
        windScale.setText(" "+weather.now.windDir + weather.now.windScale+"级");
        pressure.setText("   " + weather.now.pressure);
        //持久化存储website,方便访问
        SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(
                WeatherActivity.this
        ).edit();
        editor.putString("websiteURL",weather.fxLink);
        editor.apply();
    }

    //重载，据打开的次数来决定用那个
    private void showInfoAQI(AQI aqi1) {
        //aqi.xml赋值
        aqiText.setText(aqi1.getAqi());
        pm25Text.setText(aqi1.getPm2p5());
    }

    private void showInfoAQI(String x,String y) {
        //aqi.xml赋值
        aqiText.setText(x);
        pm25Text.setText(y);
    }

    private void showInfoForecast() {
        LinearLayout forecastLayout = findViewById(R.id.forecast_layout);
        LayoutInflater inflater = LayoutInflater.from(this);
        forecastLayout.removeAllViews();
        for (Forecast forecast : forecastList) {
            View view = inflater.inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);

            dateText.setText(forecast.getFxDate());
            infoText.setText(forecast.getTextDay());
            maxText.setText("最高："+forecast.getTempMax());
            minText.setText("最低："+forecast.getTempMin());

            forecastLayout.addView(view);
        }
    }

    private void showInfoHourForecast(boolean isShow){
        if (isShow) {
            LinearLayout hourForecastLayout = findViewById(R.id.hour_forecast_layout);
            LayoutInflater inflater = LayoutInflater.from(this);
            hourForecastLayout.removeAllViews();
            for (Hour_forecast hourForecast : HourforecastList) {
                View view = inflater.inflate(R.layout.hour_forecast_item, hourForecastLayout, false);
                TextView time = view.findViewById(R.id.h_time);
                TextView text = view.findViewById(R.id.h_text);
                TextView temp = view.findViewById(R.id.h_temp);
                TextView hum = view.findViewById(R.id.h_hum);

                String x = hourForecast.getFxTime();
                time.setText(x.substring(5, 10) + " " + x.substring(11, 16));
                text.setText(hourForecast.getText());
                temp.setText("温度：" + hourForecast.getTemp());
                hum.setText("湿度：" + hourForecast.getHum());

                hourForecastLayout.addView(view);
            }
        }else{
            TextView hf_Title=(TextView)findViewById(R.id.hf_title);//标题也隐藏了
            hf_Title.setVisibility(View.GONE);
        }
    }

    /*显示进度的对话框*/
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("获取背景图片中 请等待...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /*关闭进度对话框*/
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();/*关闭*/
        }
    }

}



//书上原有的
//下面全部弃用
    //据天气id查询天气信息
    /*public void requestAPI(String weatherId, String type){
        String URL="";
        Log.d(TAG, weatherId);
        if (type.equals("weather")){
            URL="https://devapi.qweather.com/v7/weather/now?location="+weatherId+APIkey;
            Log.d(TAG, URL);
            HttpUtil.sendOkHttpRequest(URL, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WeatherActivity.this,"获取天气失败"
                                    ,Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseText=response.body().string();
                    final Weather weather=(Weather) Utility.handleNewAPIResponse(responseText,"weather");
                    Log.d(TAG, "onResponse: weather持久化前");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (weather!=null){
                                //UI线程操作，持久化存储
                                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(
                                        WeatherActivity.this
                                ).edit();
                                editor.putString("weather",responseText);
                                editor.apply();
                                showInfo(weather);
                            }else{
                                Toast.makeText(WeatherActivity.this,"获取天气信息失败",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        }/*else if (type.equals("aqi")){
            URL="https://devapi.qweather.com/v7/air/now?location="+weatherId+APIkey;
            Log.d(TAG, URL);
            HttpUtil.sendOkHttpRequest(URL, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WeatherActivity.this,"获取AQI失败onFailure"
                                    ,Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                        final String responseText=response.body().string();
                        Log.d(TAG,responseText);
                        //final AQI aqi=(AQI) Utility.handleNewAPIResponse(responseText,"aqi");
                        Log.d(TAG,"handleNewAPIAQIResponse调用");
                        boolean aqi=Utility.handleNewAPIAQIResponse(responseText);
                        Log.d(TAG, String.valueOf(aqi));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (aqi!=false){
                                    //UI线程操作，持久化存储
                                    SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(
                                            WeatherActivity.this
                                    ).edit();
                                    editor.putString("aqi",responseText);
                                    editor.apply();
                                    showInfo(new AQI());
                                }else{
                                    Toast.makeText(WeatherActivity.this,"获取AQI信息失败onResponse",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
            });
        }*/
    //}






//处理并展示weather中的实体类
    /*private void showWeatherInfo(Weather weather){




        //String cityName=weather.basic.cityName;
        //String degree=weather.now.temp+"℃";
        //String weatherInfo=weather.now.more.info;
        //titleCity.setText(cityName);

        //degreeText.setText(degree);
        //weatherInfoText.setText(weatherInfo);

        //原预测天气处理方式
        /*

        /*
        /*
        if (weather.aqi!=null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort="舒适度："+ weather.website.xml.comfort.info;
        String carWash="洗车指数"+weather.website.xml.carWash.info;
        String sport="运动建议："+weather.website.xml.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }*/