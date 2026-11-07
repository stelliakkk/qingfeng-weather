package cn.codex.qingfengweather.data;

public interface WeatherProvider {
    String name();
    WeatherSnapshot fetch(City city) throws Exception;
}
