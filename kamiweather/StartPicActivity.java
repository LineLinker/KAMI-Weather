package com.yye.kamiweather;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.TextView;

import java.util.Random;

public class StartPicActivity extends AppCompatActivity {

    //1sAPP开屏动画
    private final int SPLASH_DISPLAY_LENGHT = 1000;
    private Handler handler;
    //随机出现logo
    private int[] backgrounds = {R.drawable.logo1, R.drawable.logo2};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_start_pic);
        handler = new Handler();
        // 获取TextView组件
        TextView textView = findViewById(R.id.start_pic_tv);
        // 从backgrounds数组中随机选择一个作为背景
        Random random = new Random();
        int randomIndex = random.nextInt(backgrounds.length);
        int selectedBackground = backgrounds[randomIndex];
        // 设置TextView的背景
        textView.setBackgroundResource(selectedBackground);
        // 延迟SPLASH_DISPLAY_LENGHT时间然后跳转到MainActivity
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(StartPicActivity.this,
                        MainActivity.class);
                startActivity(intent);
                StartPicActivity.this.finish();
            }
        }, SPLASH_DISPLAY_LENGHT);
    }

    //禁止返回
    @Override
    public void onBackPressed() {

    }
}