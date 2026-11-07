package cn.codex.qingfengweather.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public final class OpenMeteoProvider implements WeatherProvider {
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";

    @Override
    public String name() {
        return "Open-Meteo 备用";
    }

    @Override
    public WeatherSnapshot fetch(City city) throws IOException, JSONException {
        String query = String.format(Locale.US,
                "latitude=%.5f&longitude=%.5f&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,precipitation_probability,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=7&wind_speed_unit=kmh",
                city.latitude, city.longitude);
        JSONObject root = getJson(FORECAST_URL + "?" + query);
        return parseForecast(root);
    }

    static WeatherSnapshot parseForecast(JSONObject root) throws JSONException {
        JSONObject current = root.getJSONObject("current");
        JSONObject daily = root.getJSONObject("daily");
        JSONObject hourly = root.optJSONObject("hourly");

        int humidity = current.optInt("relative_humidity_2m", 0);
        int code = current.optInt("weather_code", -1);
        String condition = WeatherText.fromWmoCode(code);
        List<DayForecast> days = new ArrayList<>();
        JSONArray dates = daily.optJSONArray("time");
        JSONArray codes = daily.optJSONArray("weather_code");
        JSONArray highs = daily.optJSONArray("temperature_2m_max");
        JSONArray lows = daily.optJSONArray("temperature_2m_min");
        if (dates != null) {
            for (int i = 0; i < dates.length(); i++) {
                int dayCode = codes == null ? -1 : codes.optInt(i, -1);
                days.add(new DayForecast(
                        dates.optString(i, ""),
                        WeatherText.fromWmoCode(dayCode),
                        dayCode,
                        numberAt(highs, i, Double.NaN),
                        numberAt(lows, i, Double.NaN)));
            }
        }
        List<HourForecast> hours = parseHours(hourly, current.optString("time", ""));
        return new WeatherSnapshot(
                current.getDouble("temperature_2m"),
                current.optDouble("apparent_temperature", current.getDouble("temperature_2m")),
                humidity,
                current.optDouble("wind_speed_10m", 0),
                current.optInt("wind_direction_10m", -1),
                current.optDouble("precipitation", 0),
                condition,
                code,
                current.optString("time", ""),
                "Open-Meteo 备用",
                "Open-Meteo · https://open-meteo.com · CC BY 4.0",
                days,
                hours,
                null,
                null,
                null);
    }

    static List<HourForecast> parseHours(JSONObject hourly, String currentTime) {
        List<HourForecast> hours = new ArrayList<>();
        if (hourly == null) return hours;
        JSONArray times = hourly.optJSONArray("time");
        JSONArray temperatures = hourly.optJSONArray("temperature_2m");
        JSONArray codes = hourly.optJSONArray("weather_code");
        JSONArray probabilities = hourly.optJSONArray("precipitation_probability");
        if (times == null || temperatures == null) return hours;
        int start = 0;
        if (currentTime != null && currentTime.length() >= 13) {
            String currentHour = currentTime.substring(0, 13);
            for (int i = 0; i < times.length(); i++) {
                if (times.optString(i, "").startsWith(currentHour)) { start = i; break; }
            }
        }
        for (int i = start; i < Math.min(times.length(), start + 24); i++) {
            int code = codes == null ? -1 : codes.optInt(i, -1);
            hours.add(new HourForecast(times.optString(i, ""),
                    numberAt(temperatures, i, Double.NaN), code,
                    WeatherText.fromWmoCode(code), probabilities == null ? -1 : probabilities.optInt(i, -1)));
        }
        return hours;
    }

    public List<City> searchCities(String query) throws IOException, JSONException {
        String url = GEOCODING_URL
                + "?name=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                + "&count=8&language=zh&format=json";
        JSONObject root = getJson(url);
        JSONArray results = root.optJSONArray("results");
        List<City> cities = new ArrayList<>();
        Set<String> seenCoordinates = new HashSet<>();
        Set<String> seenNames = new HashSet<>();
        if (results == null) return cities;
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.optJSONObject(i);
            if (item == null || !item.has("latitude") || !item.has("longitude")) continue;
            double latitude = item.getDouble("latitude");
            double longitude = item.getDouble("longitude");
            String coordinateKey = String.format(Locale.US, "%.4f,%.4f", latitude, longitude);
            if (!seenCoordinates.add(coordinateKey)) continue;
            String admin1 = item.optString("admin1", "");
            String admin2 = item.optString("admin2", "");
            City city = new City(
                    item.optString("name", ""),
                    admin2.isEmpty() ? admin1 : admin2,
                    admin2.isEmpty() ? "" : admin1,
                    item.optString("country", ""),
                    latitude,
                    longitude,
                    item.optString("timezone", "auto"),
                    "",
                    item.optString("feature_code", ""));
            if (seenNames.add(city.displayName())) cities.add(city);
        }
        return cities;
    }

    static JSONObject getJson(String url) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("User-Agent", "QingFengWeather/1.0");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Weather service returned HTTP " + status);
            }
            InputStream stream = connection.getInputStream();
            if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                stream = new GZIPInputStream(stream);
            }
            StringBuilder text = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) text.append(line);
            }
            if (text.length() == 0 || text.length() > 2_000_000) {
                throw new IOException("Weather service returned an empty or oversized response");
            }
            return new JSONObject(text.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static double numberAt(JSONArray array, int index, double fallback) {
        if (array == null || index >= array.length() || array.isNull(index)) return fallback;
        return array.optDouble(index, fallback);
    }
}
