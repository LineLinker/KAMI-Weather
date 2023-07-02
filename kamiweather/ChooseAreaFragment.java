package com.yye.kamiweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;



import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.yye.kamiweather.db.City;
import com.yye.kamiweather.db.County;
import com.yye.kamiweather.db.Province;
import com.yye.kamiweather.util.HttpUtil;
import com.yye.kamiweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/*碎片的代码*/
public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private final String APIkey = "&key="+"你的和风天气的密钥";
    private ProgressDialog progressDialog;
    private TextView titletext;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /*省列表*/
    private List<Province> provinceList;
    /*市列表*/
    private List<City> cityList;
    /*县列表*/
    private List<County> countyList;
    /*被选中的省*/
    private Province selectedProvince;
    /*被选中的表*/
    private City selectedCity;
    /*当前选中的级别*/
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_layout, container, false);
        titletext = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        queryNewProvince();//先进行一次查询
        ReadSet();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //当前是省级的列表时
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    //获取所选省级的名称    //查询市，传入省级名称
                    queryNewCities(selectedProvince.getProvinceName());
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryNewCounties(selectedCity.getCityName());
                } else if (currentLevel == LEVEL_COUNTY) {
                    //该部分用于从Fragment跳转到weatherActivity
                    String weatherId = countyList.get(position).getWeatherId();
                    String countyName=countyList.get(position).getCountyName();
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_Id", weatherId);
                    intent.putExtra("county_Name", countyName);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });
        Toast.makeText(getContext(),"请选择地区",Toast.LENGTH_LONG).show();
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    //对比书上，采取整体结构重写，思路为下：
    //首先打开该碎片时，直接调用书上的API获取全部省级城市，不获取ID和locationID，将省级名称存入数据库
    //进行省份选择，选择完成后据所选择的省级的名称使用新API查询其下面的城市，并获取到对应的locationID
    //县级同市级处理方案  将市级和县级的写入数据库  依此思路重写
    private void queryNewProvince() {
        titletext.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            //数据库无省级名称数据时直接查询
            showProgressDialog();//loading提示
            String address = "http://guolin.tech/api/china/";
            HttpUtil.sendOkHttpRequest(address, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    //回到主线程处理逻辑
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            Toast.makeText(getContext(), "获取省级数据失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();//接收到的服务器返回的数据
                    boolean result = false;
                    result = Utility.handleNewProvinceResponse(responseText);//交由utility处理
                    if (result) {
                        //utility处理成功
                        //更新至UI
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeProgressDialog();
                                queryNewProvince();//调用自己走if,更新fragment的列表显示
                            }
                        });
                    }
                }
            });
        }
    }

    //参数x为对应的省级名称  即为上一级
    private void queryNewCities(String x) {
        titletext.setText(x);
        backButton.setVisibility(View.VISIBLE);
        //查询city中满足该省级（属于该省级）的城市加入列表中 无法使用ID，因为在导入省级名称时，没有导入ID
        cityList = DataSupport.where("provinceName = ?", x).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            queryFromNewAPI(x, "city");
        }
    }

    //同市级处理方案 x为上一级
    private void queryNewCounties(String x) {
        titletext.setText(x);
        backButton.setVisibility(View.VISIBLE);
        //查询county中满足该city级的县级加入列表中 简化代码使用名称查询
        countyList = DataSupport.where("cityName = ?",x).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            queryFromNewAPI(x, "county");
        }
    }

    //从新API查询其他信息，据传入参数判断   第一个参数 被查询的上一级 第二个参数 被查询的本级
    private void queryFromNewAPI(String UpLevel, String NowLevel) {
        showProgressDialog();
        String address = "https://geoapi.qweather.com/v2/city/lookup?location=" + UpLevel + APIkey;
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                //回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "从服务器查询失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();//接收到的服务器返回的数据
                boolean result = false;
                if ("city".equals(NowLevel)) {
                    result=Utility.handleNewCityResponse(responseText,selectedProvince.getProvinceName());
                } else if ("county".equals(NowLevel)) {
                    result=Utility.handleNewCountyResponse(responseText,selectedCity.getCityName());
                }
                //处理成功进行UI同步
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("city".equals(NowLevel)) {
                                queryNewCities(UpLevel);
                            } else if ("county".equals(NowLevel)) {
                                queryNewCounties(UpLevel);
                            }
                        }
                    });
                }
            }
        });
    }

    /*显示进度的对话框*/
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Now Loading...");
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

    private void showAreaSelectionMessage() {
        Toast.makeText(getContext(), "请选择地区", Toast.LENGTH_SHORT).show();
    }
    private boolean ReadSet(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean("isFirstStartFragment",true);//第一次创建时，其值为空，默认为是第一次使用;
    }

    //检测手机自带返回键操作
    public void onBackPressed() {
        if (currentLevel == LEVEL_COUNTY) {
            queryNewCities(selectedProvince.getProvinceName());
        } else if (currentLevel == LEVEL_CITY) {
            queryNewProvince();
        } else {
            if (ReadSet()){
                //是第一次
                showAreaSelectionMessage();//提示选择
            }
        }
    }
}

//下面的是书上原有的，因和风天气API变动
    //已更新  下面均以弃用  留做备份
    //由于和风天气API更新，但是当前省、市、县级列表查询仍可用，
    //故采用原来的方法查询城市列表，其对应ID将采用新api获取（to do）
    /*查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器查询*/
    /*private void queryProvince(){
        titletext.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();
            for (Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address="http://guolin.tech/api/china/";
            queryFromServer(address,"province");
            String NewAPIAddress="https://geoapi.qweather.com/v2/city/lookup?location=北京"+APIkey;
            queryIDFromNewAPI(NewAPIAddress,"province");//从新API查询locationID
            Log.d(TAG, "queryProvince: suc1");
        }
    }*/
    /*查询省内的所有的市，优先从数据库查询，如果没有查询到再去服务器查询*/
   /* private void queryCities(){
        titletext.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",
                String.valueOf(selectedProvince.getLocationID())).find(City.class);
        if (cityList.size()>0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
            Log.d(TAG, "queryCities: suc2");
        }
    }*/
    /*查询市内的所有的县，优先从数据库查询，如果没有查询到再去服务器查询*/
    /*private void queryCounties(){
        titletext.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        Log.d(TAG, "queryCounties: 1111");
        countyList= DataSupport.where("cityid = ?",
                String.valueOf(selectedCity.getId())).find(County.class);
        Log.d(TAG, "queryCounties: 2222");
        if (countyList.size()>0){
            dataList.clear();
            for (County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/" +
                    cityCode;
            queryFromServer(address,"county");
            Log.d(TAG, "queryCounties: 33333");
        }
    }*/

    /*根据传来的地址和类型从服务器上查询*/
   /* private void queryFromServer(String address,final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();//接收到的服务器返回的数据
                boolean result=false;
                //据type类型调用utility的不同方法来处理
                Log.d(TAG, "onResponse: 1111");
                if ("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if ("county".equals(type)){
                    result=Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                //处理成功进行同步
                if (result){
                    Log.d(TAG, "onResponse: 4444");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvince();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: start track");
                e.printStackTrace();
                Log.d(TAG, "onFailure: finish track");
                //回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }*/

