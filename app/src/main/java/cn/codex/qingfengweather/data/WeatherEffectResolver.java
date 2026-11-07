package cn.codex.qingfengweather.data;

import java.util.Locale;

/** Maps provider condition labels to a small, provider-independent set of visuals. */
public final class WeatherEffectResolver {
    private WeatherEffectResolver() {}

    public static WeatherEffect resolve(String condition) {
        if (condition == null || condition.trim().isEmpty()) return WeatherEffect.UNKNOWN;
        String value = condition.trim().toLowerCase(Locale.ROOT);

        if (containsAny(value, "雷", "thunder", "雷暴")) return WeatherEffect.THUNDERSTORM;
        if (containsAny(value, "雪", "snow", "sleet", "雨夹雪", "冻雨")) return WeatherEffect.SNOW;
        if (containsAny(value, "雨", "rain", "drizzle", "shower", "毛毛")) return WeatherEffect.RAIN;
        if (containsAny(value, "雾", "霾", "fog", "haze", "mist", "dust", "smoke")) return WeatherEffect.FOG;

        boolean hasCloud = containsAny(value, "云", "cloud", "overcast");
        boolean hasSun = containsAny(value, "晴", "sun", "clear");
        if (hasCloud && hasSun || containsAny(value,
                "局部多云", "少云", "大部晴朗", "晴间多云", " partly", "mostly sunny", "partly sunny")) {
            return WeatherEffect.PARTLY_CLOUDY;
        }
        if (containsAny(value, "阴", "多云", "云", "cloud", "overcast")) return WeatherEffect.CLOUDY;
        if (hasSun) return WeatherEffect.SUNNY;
        return WeatherEffect.UNKNOWN;
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) return true;
        }
        return false;
    }
}
