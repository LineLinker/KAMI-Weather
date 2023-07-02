package com.yye.kamiweather.db;

import org.litepal.crud.DataSupport;
/*city*/
public class City extends DataSupport {
    private int id;
    private String cityName;
    private String cityCode;
    private String provinceName;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getCityName() {
        return cityName;
    }
    public void setCityName(String cityName) {
        this.cityName= cityName;
    }
    public String getCityCode(){
        return cityCode;
    }
    public void setCityCode(String cityCode){
        this.cityCode=cityCode;
    }
    public String getProvinceName(){
        return provinceName;
    }
    public void setProvinceName(String provinceName){
        this.provinceName=provinceName;
    }
}

