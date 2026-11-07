package cn.codex.qingfengweather.data;

public final class AirQuality {
    public final String aqiDisplay;
    public final String category;
    public final String primaryPollutant;
    public final String pm25;
    public final String pm10;
    public final String advice;
    public final int red;
    public final int green;
    public final int blue;

    public AirQuality(String aqiDisplay, String category, String primaryPollutant,
                      String pm25, String pm10, String advice, int red, int green, int blue) {
        this.aqiDisplay = safe(aqiDisplay);
        this.category = safe(category);
        this.primaryPollutant = safe(primaryPollutant);
        this.pm25 = safe(pm25);
        this.pm10 = safe(pm10);
        this.advice = safe(advice);
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
