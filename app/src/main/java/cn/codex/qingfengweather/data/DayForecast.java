package cn.codex.qingfengweather.data;

public final class DayForecast {
    public final String date;
    public final String condition;
    public final int weatherCode;
    public final double highC;
    public final double lowC;

    public DayForecast(String date, String condition, int weatherCode, double highC, double lowC) {
        this.date = date == null ? "" : date;
        this.condition = condition == null ? "" : condition;
        this.weatherCode = weatherCode;
        this.highC = highC;
        this.lowC = lowC;
    }
}
