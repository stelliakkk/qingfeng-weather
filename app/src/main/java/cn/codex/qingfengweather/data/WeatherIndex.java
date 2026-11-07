package cn.codex.qingfengweather.data;

public final class WeatherIndex {
    public final String name;
    public final String category;
    public final String text;

    public WeatherIndex(String name, String category, String text) {
        this.name = name == null ? "" : name;
        this.category = category == null ? "" : category;
        this.text = text == null ? "" : text;
    }
}
