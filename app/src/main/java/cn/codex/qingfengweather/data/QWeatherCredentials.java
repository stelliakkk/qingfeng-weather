package cn.codex.qingfengweather.data;

public final class QWeatherCredentials {
    public final String host;
    public final String apiKey;

    public QWeatherCredentials(String host, String apiKey) {
        this.host = host == null ? "" : host.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean isConfigured() {
        return !host.isEmpty() && !apiKey.isEmpty();
    }
}
