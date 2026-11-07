package cn.codex.qingfengweather.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WeatherSnapshot {
    public final double temperatureC;
    public final double feelsLikeC;
    public final int humidityPercent;
    public final double windKmh;
    public final int windDirection;
    public final double precipitationMm;
    public final String condition;
    public final int weatherCode;
    public final String updatedAt;
    public final String source;
    public final String attribution;
    public final List<DayForecast> days;
    public final List<HourForecast> hours;
    public final List<WeatherAlert> alerts;
    public final AirQuality airQuality;
    public final List<WeatherIndex> indices;

    public WeatherSnapshot(
            double temperatureC,
            double feelsLikeC,
            int humidityPercent,
            double windKmh,
            int windDirection,
            double precipitationMm,
            String condition,
            int weatherCode,
            String updatedAt,
            String source,
            String attribution,
            List<DayForecast> days) {
        this(temperatureC, feelsLikeC, humidityPercent, windKmh, windDirection, precipitationMm,
                condition, weatherCode, updatedAt, source, attribution, days,
                Collections.emptyList(), null, null, null);
    }

    public WeatherSnapshot(
            double temperatureC,
            double feelsLikeC,
            int humidityPercent,
            double windKmh,
            int windDirection,
            double precipitationMm,
            String condition,
            int weatherCode,
            String updatedAt,
            String source,
            String attribution,
            List<DayForecast> days,
            List<HourForecast> hours,
            List<WeatherAlert> alerts,
            AirQuality airQuality,
            List<WeatherIndex> indices) {
        this.temperatureC = temperatureC;
        this.feelsLikeC = feelsLikeC;
        this.humidityPercent = humidityPercent;
        this.windKmh = windKmh;
        this.windDirection = windDirection;
        this.precipitationMm = precipitationMm;
        this.condition = condition == null ? "" : condition;
        this.weatherCode = weatherCode;
        this.updatedAt = updatedAt == null ? "" : updatedAt;
        this.source = source == null ? "" : source;
        this.attribution = attribution == null ? "" : attribution;
        this.days = Collections.unmodifiableList(new ArrayList<>(days == null ? Collections.emptyList() : days));
        this.hours = Collections.unmodifiableList(new ArrayList<>(hours == null ? Collections.emptyList() : hours));
        this.alerts = alerts == null ? null : Collections.unmodifiableList(new ArrayList<>(alerts));
        this.airQuality = airQuality;
        this.indices = indices == null ? null : Collections.unmodifiableList(new ArrayList<>(indices));
    }

    public WeatherSnapshot withSource(String newSource, String newAttribution) {
        return new WeatherSnapshot(
                temperatureC, feelsLikeC, humidityPercent, windKmh, windDirection, precipitationMm,
                condition, weatherCode, updatedAt, newSource, newAttribution, days,
                hours, alerts, airQuality, indices);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("temperatureC", temperatureC);
        result.put("feelsLikeC", feelsLikeC);
        result.put("humidityPercent", humidityPercent);
        result.put("windKmh", windKmh);
        result.put("windDirection", windDirection);
        result.put("precipitationMm", precipitationMm);
        result.put("condition", condition);
        result.put("weatherCode", weatherCode);
        result.put("updatedAt", updatedAt);
        result.put("source", source);
        result.put("attribution", attribution);
        JSONArray dayArray = new JSONArray();
        for (DayForecast day : days) {
            JSONObject item = new JSONObject();
            item.put("date", day.date);
            item.put("condition", day.condition);
            item.put("weatherCode", day.weatherCode);
            item.put("highC", Double.isFinite(day.highC) ? day.highC : JSONObject.NULL);
            item.put("lowC", Double.isFinite(day.lowC) ? day.lowC : JSONObject.NULL);
            dayArray.put(item);
        }
        result.put("days", dayArray);
        JSONArray hourArray = new JSONArray();
        for (HourForecast hour : hours) {
            JSONObject item = new JSONObject();
            item.put("time", hour.time);
            item.put("temperatureC", hour.temperatureC);
            item.put("weatherCode", hour.weatherCode);
            item.put("condition", hour.condition);
            item.put("precipitationProbability", hour.precipitationProbability);
            hourArray.put(item);
        }
        result.put("hours", hourArray);
        if (alerts == null) {
            result.put("alerts", JSONObject.NULL);
        } else {
            JSONArray alertArray = new JSONArray();
            for (WeatherAlert alert : alerts) {
                JSONObject item = new JSONObject();
                item.put("headline", alert.headline);
                item.put("sender", alert.sender);
                item.put("severity", alert.severity);
                item.put("eventName", alert.eventName);
                item.put("description", alert.description);
                item.put("instruction", alert.instruction);
                item.put("effectiveTime", alert.effectiveTime);
                item.put("expireTime", alert.expireTime);
                item.put("color", alert.color);
                alertArray.put(item);
            }
            result.put("alerts", alertArray);
        }
        if (airQuality == null) {
            result.put("airQuality", JSONObject.NULL);
        } else {
            JSONObject air = new JSONObject();
            air.put("aqiDisplay", airQuality.aqiDisplay);
            air.put("category", airQuality.category);
            air.put("primaryPollutant", airQuality.primaryPollutant);
            air.put("pm25", airQuality.pm25);
            air.put("pm10", airQuality.pm10);
            air.put("advice", airQuality.advice);
            air.put("red", airQuality.red);
            air.put("green", airQuality.green);
            air.put("blue", airQuality.blue);
            result.put("airQuality", air);
        }
        if (indices == null) {
            result.put("indices", JSONObject.NULL);
        } else {
            JSONArray indexArray = new JSONArray();
            for (WeatherIndex index : indices) {
                JSONObject item = new JSONObject();
                item.put("name", index.name);
                item.put("category", index.category);
                item.put("text", index.text);
                indexArray.put(item);
            }
            result.put("indices", indexArray);
        }
        return result;
    }

    public static WeatherSnapshot fromJson(JSONObject json) throws JSONException {
        JSONArray dayArray = json.optJSONArray("days");
        List<DayForecast> days = new ArrayList<>();
        if (dayArray != null) {
            for (int i = 0; i < dayArray.length(); i++) {
                JSONObject item = dayArray.optJSONObject(i);
                if (item == null) continue;
                days.add(new DayForecast(
                        item.optString("date", ""),
                        item.optString("condition", ""),
                        item.optInt("weatherCode", -1),
                        item.optDouble("highC", Double.NaN),
                        item.optDouble("lowC", Double.NaN)));
            }
        }
        List<HourForecast> hours = new ArrayList<>();
        JSONArray hourArray = json.optJSONArray("hours");
        if (hourArray != null) {
            for (int i = 0; i < hourArray.length(); i++) {
                JSONObject item = hourArray.optJSONObject(i);
                if (item == null) continue;
                hours.add(new HourForecast(item.optString("time", ""),
                        item.optDouble("temperatureC", Double.NaN), item.optInt("weatherCode", -1),
                        item.optString("condition", ""), item.optInt("precipitationProbability", -1)));
            }
        }
        List<WeatherAlert> alerts = null;
        JSONArray alertArray = json.optJSONArray("alerts");
        if (alertArray != null) {
            alerts = new ArrayList<>();
            for (int i = 0; i < alertArray.length(); i++) {
                JSONObject item = alertArray.optJSONObject(i);
                if (item == null) continue;
                alerts.add(new WeatherAlert(item.optString("headline", ""), item.optString("sender", ""),
                        item.optString("severity", ""), item.optString("eventName", ""),
                        item.optString("description", ""), item.optString("instruction", ""),
                        item.optString("effectiveTime", ""), item.optString("expireTime", ""),
                        item.optString("color", "")));
            }
        }
        JSONObject airJson = json.optJSONObject("airQuality");
        AirQuality air = airJson == null ? null : new AirQuality(
                airJson.optString("aqiDisplay", ""), airJson.optString("category", ""),
                airJson.optString("primaryPollutant", ""), airJson.optString("pm25", ""),
                airJson.optString("pm10", ""), airJson.optString("advice", ""),
                airJson.optInt("red", 0), airJson.optInt("green", 0), airJson.optInt("blue", 0));
        List<WeatherIndex> indices = null;
        JSONArray indexArray = json.optJSONArray("indices");
        if (indexArray != null) {
            indices = new ArrayList<>();
            for (int i = 0; i < indexArray.length(); i++) {
                JSONObject item = indexArray.optJSONObject(i);
                if (item != null) indices.add(new WeatherIndex(item.optString("name", ""),
                        item.optString("category", ""), item.optString("text", "")));
            }
        }
        return new WeatherSnapshot(
                json.getDouble("temperatureC"),
                json.optDouble("feelsLikeC", json.getDouble("temperatureC")),
                json.optInt("humidityPercent", 0),
                json.optDouble("windKmh", 0),
                json.optInt("windDirection", -1),
                json.optDouble("precipitationMm", 0),
                json.optString("condition", ""),
                json.optInt("weatherCode", -1),
                json.optString("updatedAt", ""),
                json.optString("source", ""),
                json.optString("attribution", ""),
                days, hours, alerts, air, indices);
    }
}
