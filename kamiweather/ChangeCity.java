package com.yye.kamiweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.animation.PropertyValuesHolder;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.yye.kamiweather.service.AutoUpdateService;
import com.yye.kamiweather.util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class ChangeCity extends AppCompatActivity {
    private Button cc_Location,cc_Back,cc_Select;
    private Switch sw_LoadPic,sw_UpdatePicService,sw_UpdateDataService,sw_7Forecast,sw_Hour_Forecast;
    private final String LocateAPIKEY="&key="+"你的高德地图的密钥";//高德API
    private final String WeatherAPIKEY="&key="+"你的和风天气的密钥";//和风API

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_city);
        InitAll();
        //返回上一个界面
        cc_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BackToWeather();
            }
        });
        //定位
        cc_Location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    SaveData();
                    LocateByIP();//通过IP获取坐标,成功了就打开weather
                }
            });
        //进行选择
        cc_Select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //选择的时候直接对设置进行保存，进入碎片可回退
                SaveData();
                LinearLayout ll=(LinearLayout)findViewById(R.id.cc_ll);
                // 高度设置为0，将由权重控制 权重设置为1，表示占据比例为1
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,0);
                ll.setLayoutParams(layoutParams);
                // 创建 ChooseAreaFragment 实例
                ChooseAreaFragment fragment = new ChooseAreaFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.cc_container, fragment)
                        .commit();
            }
        });
    }

    //高德地图API  使用的是 IP定位
    private void LocateByIP(){
        final String requestURL="https://restapi.amap.com/v3/ip?"+LocateAPIKEY;
        Thread LocateAPI=new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUtil.sendOkHttpRequest(requestURL, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Toast.makeText(ChangeCity.this,"定位失败，请重试",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();//得到响应体
                        handleLocateAPIResponse(responseText);
                    }
                });
            }
        });
        LocateAPI.start();
    }
    //在当前activity直接解析
    private boolean handleLocateAPIResponse(String response){
        String x="",y="",province="",city=" ";
        try {
            JSONObject jsonObject = new JSONObject(response);
            province = jsonObject.getString("province");
            city = jsonObject.getString("city");
            String rectangle = jsonObject.getString("rectangle");
            // 保留两位小数  并对坐标数据进行处理
            String[] rectangleCoordinates = rectangle.split(";");
            String topLeftCoordinate = rectangleCoordinates[0];
            String bottomRightCoordinate = rectangleCoordinates[1];
            String[] topLeft = topLeftCoordinate.split(",");
            double topLeftLatitude = Double.parseDouble(topLeft[1]);
            double topLeftLongitude = Double.parseDouble(topLeft[0]);
            String[] bottomRight = bottomRightCoordinate.split(",");
            double bottomRightLatitude = Double.parseDouble(bottomRight[1]);
            double bottomRightLongitude = Double.parseDouble(bottomRight[0]);
            // 计算平均值   用于请求该点天气
            double averageLatitude = (topLeftLatitude + bottomRightLatitude) / 2;
            double averageLongitude = (topLeftLongitude + bottomRightLongitude) / 2;
            // 格式化为保留两位小数
            x = String.format("%.2f", averageLongitude);
            y = String.format("%.2f", averageLatitude);
        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ChangeCity.this,"处理返回数据异常",Toast.LENGTH_SHORT);
                }
            });
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ChangeCity.this).edit();
        //更新主UI上的城市显示
        if (city!=null){
            editor.putString("county_Name",city);
        }else {
            if (province != null) {
                editor.putString("county_Name", province);
            }else{
                return false;//城市和省份都没有 错误
            }
        }
        editor.apply();
        //转换坐标到id
        if (x!=null&&y!=null){
            RequestLocateID(x,y);
        }else {
            return false;//没有坐标 错误
        }
        return true;
    }
    //通过x,y坐标直接解析对应区域的weather_ID
    private void RequestLocateID(String x,String y){
        String RequestLocateIDURL="https://geoapi.qweather.com/v2/city/lookup?location="+x+","+y+WeatherAPIKEY;
        Thread LocateAPI2=new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUtil.sendOkHttpRequest(RequestLocateIDURL, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();//得到响应体
                        //解析并存储weather_id
                        try {
                            String weather_id=" ";
                            JSONObject jsonObject = new JSONObject(responseText);
                            JSONArray locationArray = jsonObject.getJSONArray("location");
                            if (locationArray.length() > 0) {
                                JSONObject locationObject = locationArray.getJSONObject(0);
                                weather_id= locationObject.getString("id");
                                if (weather_id!=null){//天气id不为空
                                    SharedPreferences.Editor editor =
                                            PreferenceManager.getDefaultSharedPreferences(ChangeCity.this).edit();
                                    editor.putString("weather_Id",weather_id);
                                    editor.apply();
                                    //写入完成
                                    OpenWeather();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ChangeCity.this,"解析异常",Toast.LENGTH_SHORT);
                                }
                            });
                        }
                    }
                });
            }
        });
        LocateAPI2.start();
    }

    private void OpenWeather(){
        // 创建启动 WeatherActivity 的 Intent
        Intent intent = new Intent(ChangeCity.this, WeatherActivity.class);
        //开启的同时直接触发weather的刷新，以更新数据
        intent.putExtra("LocateRefresh",true);
        // 启动 WeatherActivity
        startActivity(intent);
    }

    //返回通用
    private void BackToWeather(){
        // 退出前保存设置的数据保存数据
        SaveData();
        Intent intent=new Intent(ChangeCity.this,WeatherActivity.class);
        startActivity(intent);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ChangeCity.this);
        intent.putExtra("weather_Id", prefs.getString("weather_Id",null));
        intent.putExtra("county_Name", prefs.getString("county_Name",null));
        ChangeCity.this.finish();
    }

    //手机自带返回键重写，调用返回
    @Override
    public void onBackPressed() {
        BackToWeather();
    }

    //绑定所有所需组件  并设置初始值
    private void InitAll() {
        cc_Select=(Button) findViewById(R.id.cc_select);
        cc_Back=(Button) findViewById(R.id.cc_back);
        cc_Location=(Button) findViewById(R.id.cc_location);
        sw_LoadPic=(Switch) findViewById(R.id.sw_loadPic);
        sw_UpdatePicService=(Switch) findViewById(R.id.sw_updatePicService);
        sw_UpdateDataService=(Switch) findViewById(R.id.sw_updateDataService);
        sw_7Forecast=(Switch) findViewById(R.id.sw_7forecast);
        sw_Hour_Forecast=(Switch) findViewById(R.id.sw_hour_forecast);
        //读取数据并设置状态
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ChangeCity.this);
        sw_LoadPic.setChecked(prefs.getBoolean("sw_LoadPic",false));
        sw_UpdatePicService.setChecked(prefs.getBoolean("sw_UpdatePicService",true));
        sw_UpdateDataService.setChecked(prefs.getBoolean("sw_UpdateDataService",true));
        if ("3".equals(prefs.getString("sw_7Forecast","3"))){
            sw_7Forecast.setChecked(false);
        }else{
            sw_7Forecast.setChecked(true);
        }
        sw_Hour_Forecast.setChecked(prefs.getBoolean("sw_Hour_Forecast",true));
    }

    private void SaveData(){
        boolean lp=sw_LoadPic.isChecked();
        boolean ups=sw_UpdatePicService.isChecked();
        boolean uds=sw_UpdateDataService.isChecked();
        boolean forecast=sw_7Forecast.isChecked();
        boolean hf=sw_Hour_Forecast.isChecked();
        //保存设置的值
        SharedPreferences.Editor editor= PreferenceManager.getDefaultSharedPreferences(ChangeCity.this).edit();
        editor.putBoolean("sw_LoadPic", lp);
        editor.putBoolean("sw_UpdatePicService", ups);
        editor.putBoolean("sw_UpdateDataService", uds);
        editor.putBoolean("isFirstStartFragment",false);//除了首次使用时，其余时间chooseArea都不是第一次打开
        if (forecast){
            editor.putString("sw_7Forecast", "7");
        }else{
            editor.putString("sw_7Forecast", "3");
        }
        editor.putBoolean("sw_Hour_Forecast", hf);
        editor.apply();
    }
}