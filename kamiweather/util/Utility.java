package com.yye.kamiweather.util;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.yye.kamiweather.db.City;
import com.yye.kamiweather.db.County;
import com.yye.kamiweather.db.Province;
import com.yye.kamiweather.gson.AQI;
import com.yye.kamiweather.gson.Forecast;
import com.yye.kamiweather.gson.Hour_forecast;
import com.yye.kamiweather.gson.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/*解析处理json工具类*/
public class Utility {
    //新的省级数据处理
    public static boolean handleNewProvinceResponse(String response) {
        //对json数据解析，并将获取到的省级名称存入数据库
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    Province province = new Province();
                    //对数据库的省级名称存入
                    province.setProvinceName(provinceObject.getString("name"));
                    province.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //新的市级数据处理
    public static boolean handleNewCityResponse(String response, String provinceName) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONObject json = new JSONObject(response);
                JSONArray allCities = json.getJSONArray("location");
                for (int i = 0; i < allCities.length(); i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    //存入城市的id和名字
                    city.setCityCode(cityObject.getString("id"));
                    city.setCityName(cityObject.getString("name"));
                    city.setProvinceName(provinceName);
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //新的县级数据处理
    public static boolean handleNewCountyResponse(String response, String cityName) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONObject json = new JSONObject(response);
                JSONArray allCounties = json.getJSONArray("location");
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    //存入县级的name和查询天气要用的id
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("id"));
                    county.setCityName(cityName);
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //新的实体类处理方法    第一个是响应的字符串
    public static Object handleNewAPIWeatherResponse(String response) {
        try {
            Gson gson = new Gson();
            Weather weather = gson.fromJson(response, Weather.class);
            return weather;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //由于混合解析会有空指针异常现拆分
    //解析aqi（空气质量）数据
    public static boolean handleNewAPIAQIResponse(String response, AQI aqi) {
        try {
            JSONObject json = new JSONObject(response);
            JSONObject now = json.getJSONObject("now");
            aqi.setAqi(now.getString("aqi"));
            aqi.setPm2p5(now.getString("pm2p5"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    //解析forecast数据
    public static List<Forecast> handleNewAPIForecastResponse(String response) {
        List<Forecast> forecastList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray dailyArray = jsonObject.getJSONArray("daily");
            for (int i = 0; i < dailyArray.length(); i++) {
                JSONObject dailyObject = dailyArray.getJSONObject(i);
                String fxDate = dailyObject.getString("fxDate");
                String textDay = dailyObject.getString("textDay");
                String tempMax = dailyObject.getString("tempMax");
                String tempMin = dailyObject.getString("tempMin");
                Forecast forecast = new Forecast(fxDate, textDay, tempMax, tempMin);
                forecastList.add(forecast);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return forecastList;
    }
    //解析hour forecast数据
    public static List<Hour_forecast> handleNewAPIHourForecastResponse(String response) {
        List<Hour_forecast> hourForecastList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray dailyArray = jsonObject.getJSONArray("hourly");
            for (int i = 0; i < dailyArray.length(); i++) {
                JSONObject dailyObject = dailyArray.getJSONObject(i);
                String fxTime = dailyObject.getString("fxTime");
                String text = dailyObject.getString("text");
                String temp = dailyObject.getString("temp");
                String hum = dailyObject.getString("humidity");
                Hour_forecast hourForecast = new Hour_forecast(fxTime, text, temp, hum);
                hourForecastList.add(hourForecast);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hourForecastList;
    }
}




//弃用，仅供备份参考
//由于和风天气API更新，导致各种各样的数据及详情需要多次分开请求

            /*if (type.equals("weather")) {
                Log.d(TAG, "handleNewAPIResponse: weatherjsonjiexi");
                Weather weather = gson.fromJson(response, Weather.class);
                return weather;
            } else if (type.equals("aqi")) {
                Log.d(TAG, "handleNewAPIResponse: aqijsonjiexi");
                JSONObject json = new JSONObject(response);
                JSONArray aqinow = json.getJSONArray("now");
                for (int i = 0; i < aqinow.length(); i++) {
                    JSONObject countyObject = aqinow.getJSONObject(i);
                    AQI aqi= new AQI();
                    aqi.aqi=countyObject.getString("aqi");
                    Log.d(TAG, aqi.aqi);
                    aqi.pm2p5=countyObject.getString("pm2p5");
                    Log.d(TAG, aqi.pm2p5);
                }
                /*JSONObject jsonObject = new JSONObject(json);
                JSONObject nowObject = jsonObject.getJSONObject("now");
                aqi = nowObject.getString("aqi");
                pm2p5 = nowObject.getString("pm2p5");
                AQI aqi = gson.fromJson(response, AQI.class);
                Log.d(TAG, aqi.pm2p5);
                return aqi;*/
    /*将返回的json数据解析为weather实体类*/
    /*public static Weather handleWeatherResponse(String response) {
        try {

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent, Weather.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }*/






    /*省级数据  弃用
   /* public static boolean handleProvinceResponse(String response){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allProvinces=new JSONArray(response);
                for (int i=0;i<allProvinces.length();i++){
                    JSONObject provinceObject=allProvinces.getJSONObject(i);
                    Province province=new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }*/

    /*市级数据*/
    /*public static boolean handleCityResponse(String response,int provinceId){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allCities=new JSONArray(response);
                for (int i=0;i<allCities.length();i++){
                    JSONObject cityObject=allCities.getJSONObject(i);
                    City city=new City();
                    city.setCityCode(cityObject.getInt("id"));
                    city.setCityName(cityObject.getString("name"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }*/

    /*县级数据*/
    /*public static boolean handleCountyResponse(String response,int cityId){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allCounties=new JSONArray(response);
                for (int i=0;i<allCounties.length();i++){
                    JSONObject countyObject=allCounties.getJSONObject(i);
                    County county=new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }*/

    /*locationID处理 不需要了
    public static boolean handleLocationIDResponse(String response,String belongs) {
        //判断是属于哪里的ID
        if (belongs.equals("province")){
            try {
                JSONObject jsonObject = new JSONObject(response);  // jsonString 是您提供的 JSON 数据
                JSONArray locationArray = jsonObject.getJSONArray("location");
                for (int i = 0; i < locationArray.length(); i++) {
                    JSONObject locationObject = locationArray.getJSONObject(i);
                    String id = locationObject.getString("id");
                    Log.d(TAG, "handleLocationIDResponse: "+id);
                    System.out.println("ID: " + id);
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }*/
