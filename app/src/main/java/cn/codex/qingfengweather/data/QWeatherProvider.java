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
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public final class QWeatherProvider implements WeatherProvider {
    private static final Pattern VALID_HOST = Pattern.compile("[A-Za-z0-9.-]+");
    private final QWeatherCredentials credentials;

    public QWeatherProvider(QWeatherCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public String name() {
        return "和风天气";
    }

    @Override
    public WeatherSnapshot fetch(City city) throws IOException, JSONException {
        if (credentials == null || !credentials.isConfigured()) {
            throw new IOException("和风天气尚未配置");
        }
        String host = normalizeHost(credentials.host);
        String coordinates = String.format(Locale.US, "%.4f/%.4f", city.latitude, city.longitude);
        String common = "?lang=zh&localTime=true";
        JSONObject currentRoot = getJson(
                "https://" + host + "/weather/v1/current/" + coordinates + common,
                credentials.apiKey);
        JSONObject dailyRoot = getJson(
                "https://" + host + "/weather/v1/daily/" + coordinates + common + "&days=7",
                credentials.apiKey);
        JSONObject hourlyRoot = getOptionalJson(
                "https://" + host + "/weather/v1/hourly/" + coordinates + common + "&hours=24",
                credentials.apiKey);
        JSONObject alertRoot = getOptionalJson(
                "https://" + host + "/weatheralert/v1/current/" + coordinates + common,
                credentials.apiKey);
        JSONObject airRoot = getOptionalJson(
                "https://" + host + "/airquality/v1/current/" + coordinates + "?lang=zh",
                credentials.apiKey);
        String encodedCoordinates = URLEncoder.encode(
                String.format(Locale.US, "%.2f,%.2f", city.longitude, city.latitude), StandardCharsets.UTF_8.name());
        JSONObject indicesRoot = getOptionalJson(
                "https://" + host + "/v7/indices/1d?type=0&location=" + encodedCoordinates + "&lang=zh",
                credentials.apiKey);
        return parseSnapshot(currentRoot, dailyRoot, hourlyRoot, alertRoot, airRoot, indicesRoot);
    }

    static WeatherSnapshot parseSnapshot(JSONObject current, JSONObject dailyRoot) throws JSONException {
        return parseSnapshot(current, dailyRoot, null, null, null, null);
    }

    static WeatherSnapshot parseSnapshot(JSONObject current, JSONObject dailyRoot, JSONObject hourlyRoot,
                                         JSONObject alertRoot, JSONObject airRoot, JSONObject indicesRoot) throws JSONException {
        List<DayForecast> days = parseDays(dailyRoot);
        List<HourForecast> hours = parseHours(hourlyRoot);
        List<WeatherAlert> alerts = alertRoot == null ? null : parseAlerts(alertRoot);
        AirQuality airQuality = airRoot == null ? null : parseAirQuality(airRoot);
        List<WeatherIndex> indices = indicesRoot == null ? null : parseIndices(indicesRoot);
        JSONObject condition = current.getJSONObject("condition");
        JSONObject temperature = current.getJSONObject("temperature");
        JSONObject feelsLike = current.optJSONObject("feelsLike");
        JSONObject wind = current.optJSONObject("wind");
        JSONObject windSpeed = wind == null ? null : wind.optJSONObject("speed");
        JSONObject windDirection = wind == null ? null : wind.optJSONObject("direction");
        JSONObject precipitation = current.optJSONObject("precipitation");
        JSONObject precipitationAmount = precipitation == null ? null : precipitation.optJSONObject("amount");
        JSONObject metadata = current.optJSONObject("metadata");
        String attribution = parseAttribution(metadata);
        if (attribution.isEmpty()) attribution = "和风天气数据";

        double temperatureC = temperature.getDouble("value");
        double feelsLikeC = feelsLike == null ? temperatureC : feelsLike.optDouble("value", temperatureC);
        int humidity = (int) Math.round(current.optDouble("humidity", 0) * 100);
        double windMps = windSpeed == null ? 0 : windSpeed.optDouble("value", 0);
        int direction = windDirection == null ? -1 : windDirection.optInt("degree", -1);
        double rain = precipitationAmount == null ? 0 : precipitationAmount.optDouble("value", 0);
        int code = safeInt(condition.optString("code", ""));
        return new WeatherSnapshot(
                temperatureC,
                feelsLikeC,
                humidity,
                windMps * 3.6,
                direction,
                rain,
                condition.optString("text", ""),
                code,
                current.optString("updateTime", metadata == null ? "" : metadata.optString("timestamp", "")),
                "和风天气",
                attribution,
                days,
                hours,
                alerts,
                airQuality,
                indices);
    }

    public List<City> searchCities(String query) throws IOException, JSONException {
        if (credentials == null || !credentials.isConfigured()) throw new IOException("GeoAPI 尚未配置");
        String url = "https://" + normalizeHost(credentials.host) + "/geo/v2/city/lookup?location="
                + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                + "&range=cn&number=20&lang=zh";
        return parseCities(getJson(url, credentials.apiKey));
    }

    static List<City> parseCities(JSONObject root) throws JSONException {
        JSONArray locations = root.optJSONArray("location");
        List<City> cities = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Set<String> seenNames = new HashSet<>();
        if (locations == null) return cities;
        for (int i = 0; i < locations.length(); i++) {
            JSONObject item = locations.optJSONObject(i);
            if (item == null || !item.has("lat") || !item.has("lon")) continue;
            String id = item.optString("id", "");
            if (!id.isEmpty() && !seenIds.add(id)) continue;
            try {
                City city = new City(
                        item.optString("name", ""),
                        item.optString("adm2", ""),
                        item.optString("adm1", ""),
                        item.optString("country", "中国"),
                        Double.parseDouble(item.getString("lat")),
                        Double.parseDouble(item.getString("lon")),
                        item.optString("tz", "Asia/Shanghai"),
                        id,
                        item.optString("type", ""));
                // Keep the most useful match when a provider repeats the same administrative path.
                if (seenNames.add(city.displayName())) cities.add(city);
            } catch (NumberFormatException ignored) {
                // Ignore malformed coordinates without failing the whole search.
            }
        }
        return cities;
    }

    public static List<DayForecast> parseDays(JSONObject root) throws JSONException {
        JSONArray items = root.optJSONArray("days");
        List<DayForecast> days = new ArrayList<>();
        if (items == null) return days;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            JSONObject daytime = item.optJSONObject("daytime");
            JSONObject daytimeCondition = daytime == null ? null : daytime.optJSONObject("condition");
            JSONObject high = item.optJSONObject("temperatureMax");
            JSONObject low = item.optJSONObject("temperatureMin");
            String time = daytime == null
                    ? item.optString("forecastStartTime", "")
                    : daytime.optString("forecastStartTime", item.optString("forecastStartTime", ""));
            if (time.length() >= 10) time = time.substring(0, 10);
            days.add(new DayForecast(
                    time,
                    daytimeCondition == null ? "" : daytimeCondition.optString("text", ""),
                    daytimeCondition == null ? -1 : safeInt(daytimeCondition.optString("code", "")),
                    high == null ? Double.NaN : high.optDouble("value", Double.NaN),
                    low == null ? Double.NaN : low.optDouble("value", Double.NaN)));
        }
        return days;
    }

    static List<HourForecast> parseHours(JSONObject root) throws JSONException {
        List<HourForecast> hours = new ArrayList<>();
        if (root == null) return hours;
        JSONArray items = root.optJSONArray("hours");
        if (items == null) return hours;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            JSONObject condition = item.optJSONObject("condition");
            JSONObject temperature = item.optJSONObject("temperature");
            JSONObject precipitation = item.optJSONObject("precipitation");
            int probability = precipitation == null ? -1
                    : (int) Math.round(precipitation.optDouble("probability", 0) * 100);
            hours.add(new HourForecast(
                    item.optString("forecastTime", ""),
                    temperature == null ? Double.NaN : temperature.optDouble("value", Double.NaN),
                    condition == null ? -1 : safeInt(condition.optString("code", "")),
                    condition == null ? "" : condition.optString("text", ""),
                    probability));
        }
        return hours;
    }

    static List<WeatherAlert> parseAlerts(JSONObject root) throws JSONException {
        JSONArray items = root.optJSONArray("alerts");
        List<WeatherAlert> alerts = new ArrayList<>();
        if (items == null) return alerts;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            JSONObject event = item.optJSONObject("eventType");
            JSONObject color = item.optJSONObject("color");
            alerts.add(new WeatherAlert(
                    item.optString("headline", "天气预警"),
                    item.optString("senderName", ""),
                    item.optString("severity", ""),
                    event == null ? "" : event.optString("name", ""),
                    item.optString("description", ""),
                    item.optString("instruction", ""),
                    item.optString("effectiveTime", ""),
                    item.optString("expireTime", ""),
                    color == null ? "" : color.optString("code", "")));
        }
        return alerts;
    }

    static AirQuality parseAirQuality(JSONObject root) throws JSONException {
        JSONArray indexes = root.optJSONArray("indexes");
        if (indexes == null || indexes.length() == 0) return null;
        JSONObject chosen = null;
        JSONObject fallback = null;
        for (int i = 0; i < indexes.length(); i++) {
            JSONObject item = indexes.optJSONObject(i);
            if (item == null) continue;
            String code = item.optString("code", "");
            if ("cn-mee-1h".equals(code)) { chosen = item; break; }
            if ("cn-mee".equals(code)) chosen = item;
            else if (!"qaqi".equals(code) && fallback == null) fallback = item;
        }
        if (chosen == null) chosen = fallback;
        if (chosen == null) return null;
        JSONObject pollutant = chosen.optJSONObject("primaryPollutant");
        JSONArray pollutants = root.optJSONArray("pollutants");
        String pm25 = findPollutant(pollutants, "pm2p5");
        String pm10 = findPollutant(pollutants, "pm10");
        JSONObject color = chosen.optJSONObject("color");
        JSONObject health = chosen.optJSONObject("health");
        JSONObject advice = health == null ? null : health.optJSONObject("advice");
        String generalAdvice = advice == null ? "" : advice.optString("generalPopulation", "");
        return new AirQuality(
                chosen.optString("aqiDisplay", chosen.optString("aqi", "—")),
                chosen.optString("category", ""),
                pollutant == null ? "" : pollutant.optString("name", ""),
                pm25, pm10, generalAdvice,
                color == null ? 80 : color.optInt("red", 80),
                color == null ? 180 : color.optInt("green", 180),
                color == null ? 120 : color.optInt("blue", 120));
    }

    private static String findPollutant(JSONArray pollutants, String code) {
        if (pollutants == null) return "";
        for (int i = 0; i < pollutants.length(); i++) {
            JSONObject item = pollutants.optJSONObject(i);
            if (item == null || !code.equals(item.optString("code", ""))) continue;
            JSONObject concentration = item.optJSONObject("concentration");
            if (concentration == null) return "";
            return String.format(Locale.getDefault(), "%.0f %s",
                    concentration.optDouble("value", Double.NaN), concentration.optString("unit", ""));
        }
        return "";
    }

    static List<WeatherIndex> parseIndices(JSONObject root) throws JSONException {
        JSONArray items = root.optJSONArray("daily");
        List<WeatherIndex> indices = new ArrayList<>();
        if (items == null) return indices;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            indices.add(new WeatherIndex(item.optString("name", "天气指数"),
                    item.optString("category", ""), item.optString("text", "")));
        }
        return indices;
    }

    private static JSONObject getOptionalJson(String url, String key) {
        try {
            return getJson(url, key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JSONObject getJson(String url, String apiKey) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(4500);
            connection.setReadTimeout(5500);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("X-QW-Api-Key", apiKey);
            connection.setRequestProperty("User-Agent", "QingFengWeather/1.0");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("和风天气返回 HTTP " + status);
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
                throw new IOException("和风天气返回了空或过大的数据");
            }
            return new JSONObject(text.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String normalizeHost(String value) throws IOException {
        String host = value == null ? "" : value.trim();
        host = host.replaceFirst("(?i)^https?://", "");
        while (host.endsWith("/")) host = host.substring(0, host.length() - 1);
        if (!VALID_HOST.matcher(host).matches() || host.contains("..")) {
            throw new IOException("和风天气 API Host 格式无效");
        }
        return host.toLowerCase(Locale.ROOT);
    }

    private static String parseAttribution(JSONObject metadata) {
        if (metadata == null) return "";
        JSONArray values = metadata.optJSONArray("attributions");
        if (values == null || values.length() == 0) return "";
        List<String> shortValues = new ArrayList<>();
        for (int i = 0; i < values.length(); i++) {
            String value = values.optString(i, "").trim();
            if (!value.isEmpty()) shortValues.add(value);
        }
        return String.join(" · ", shortValues);
    }

    private static int safeInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
