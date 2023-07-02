package com.yye.kamiweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.SavedStateHandle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //从 SharedPreferences 读取缓存数据，不为null则证明已经有数据了
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getString("weather",null)!=null){
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);  // 启动WeatherActivity
            finish();
        }
    }

    //返回键
    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.choose_fragment);

        if (currentFragment instanceof ChooseAreaFragment) {
            // 如果当前碎片是 ChooseAreaFragment，则调用该碎片的 onBackPressed() 方法处理返回事件
            ChooseAreaFragment chooseAreaFragment = (ChooseAreaFragment) currentFragment;
            chooseAreaFragment.onBackPressed();
        } else {
            // 如果当前碎片不是 ChooseAreaFragment，则让父级活动处理返回事件
            super.onBackPressed();
        }
    }

}