package com.yye.kamiweather.gson;

public class Hour_forecast {

    private String fxTime;
    private String text;
    private String temp;
    private String hum;

    public Hour_forecast(String fxTime, String text, String temp, String hum) {
        this.fxTime = fxTime;
        this.text = text;
        this.temp = temp;
        this.hum = hum;
    }

    public String getFxTime() {
            return fxTime;
        }

    public String getText() {
            return text;
        }

    public String getTemp() {
            return temp;
        }

    public String getHum() {
            return hum;
        }

}
