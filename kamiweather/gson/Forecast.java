package com.yye.kamiweather.gson;



public class Forecast {
    private String fxDate;
    private String textDay;
    private String tempMax;
    private String tempMin;

    public Forecast(String fxDate, String textDay, String tempMax, String tempMin) {
        this.fxDate = fxDate;
        this.textDay = textDay;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
    }

    public String getFxDate() {
        return fxDate;
    }

    public String getTextDay() {
        return textDay;
    }

    public String getTempMax() {
        return tempMax;
    }

    public String getTempMin() {
        return tempMin;
    }
}
