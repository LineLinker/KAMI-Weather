package com.yye.kamiweather.util;

import com.yye.kamiweather.R;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Callback;/*额外加的，与书上不同*/


public class HttpUtil {
    public static void sendOkHttpRequest(String address, Callback callback){
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
