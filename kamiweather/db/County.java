package com.yye.kamiweather.db;

import org.litepal.crud.DataSupport;

/*county类*/
public class County extends DataSupport {
    private int id;
    private String countyName;
    private String weatherId;
    private String cityName;

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id=id;
    }

    public String getCountyName(){
        return countyName;
    }

    public void setCountyName(String countyName){
        this.countyName=countyName;
    }

    public String getWeatherId(){
        return weatherId;
    }

    public void setWeatherId(String weatherId){
        this.weatherId=weatherId;
    }

    public String getCityName(){
        return cityName;
    }

    public void setCityName(String cityName){this.cityName=cityName;}

}

