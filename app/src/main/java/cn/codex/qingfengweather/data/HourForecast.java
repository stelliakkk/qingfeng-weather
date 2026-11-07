package cn.codex.qingfengweather.data;

public final class HourForecast {
    public final String time;
    public final double temperatureC;
    public final int weatherCode;
    public final String condition;
    public final int precipitationProbability;

    public HourForecast(String time, double temperatureC, int weatherCode, String condition, int precipitationProbability) {
        this.time = time == null ? "" : time;
        this.temperatureC = temperatureC;
        this.weatherCode = weatherCode;
        this.condition = condition == null ? "" : condition;
        this.precipitationProbability = precipitationProbability;
    }
}
