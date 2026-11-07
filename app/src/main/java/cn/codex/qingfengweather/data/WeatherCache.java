package cn.codex.qingfengweather.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public final class WeatherCache {
    private static final String PREFS = "weather_cache";
    private final SharedPreferences preferences;

    public WeatherCache(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveCity(City city) {
        preferences.edit()
                .putString("city_name", city.name)
                .putString("city_region", city.region)
                .putString("city_admin1", city.admin1)
                .putString("city_country", city.country)
                .putString("city_location_id", city.locationId)
                .putString("city_type", city.type)
                .putString("city_timezone", city.timezone)
                .putString("city_lat", Double.toString(city.latitude))
                .putString("city_lon", Double.toString(city.longitude))
                .apply();
    }

    public City getCity() {
        String name = preferences.getString("city_name", "");
        if (name == null || name.isEmpty()) return null;
        try {
            return new City(
                    name,
                    preferences.getString("city_region", ""),
                    preferences.getString("city_admin1", ""),
                    preferences.getString("city_country", ""),
                    Double.parseDouble(preferences.getString("city_lat", "")),
                    Double.parseDouble(preferences.getString("city_lon", "")),
                    preferences.getString("city_timezone", "auto"),
                    preferences.getString("city_location_id", ""),
                    preferences.getString("city_type", ""));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public void saveSnapshot(WeatherSnapshot snapshot) {
        try {
            preferences.edit()
                    .putString("snapshot", snapshot.toJson().toString())
                    .putLong("snapshot_saved_at", System.currentTimeMillis())
                    .apply();
        } catch (JSONException ignored) {
            // A cache write failure must never block a successful weather refresh.
        }
    }

    public WeatherSnapshot getSnapshot() {
        String value = preferences.getString("snapshot", "");
        if (value == null || value.isEmpty()) return null;
        try {
            return WeatherSnapshot.fromJson(new JSONObject(value));
        } catch (JSONException ignored) {
            return null;
        }
    }

    public void clearSnapshot() {
        preferences.edit().remove("snapshot").remove("snapshot_saved_at").apply();
    }

    public long getSnapshotSavedAt() {
        return preferences.getLong("snapshot_saved_at", 0);
    }

    public boolean isFresh(long maxAgeMillis) {
        long savedAt = getSnapshotSavedAt();
        return savedAt > 0 && System.currentTimeMillis() - savedAt < maxAgeMillis;
    }
}
