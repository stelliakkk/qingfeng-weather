package cn.codex.qingfengweather.data;

public final class WeatherRepository {
    private final WeatherProvider primary;
    private final WeatherProvider backup;

    public WeatherRepository(WeatherProvider primary, WeatherProvider backup) {
        this.primary = primary;
        this.backup = backup;
    }

    public RefreshResult refresh(City city) {
        boolean primaryUnavailable = false;
        if (primary != null) {
            try {
                WeatherSnapshot result = primary.fetch(city);
                return RefreshResult.success(result.withSource(primary.name(), result.attribution), false, false);
            } catch (Exception ignored) {
                primaryUnavailable = true;
            }
        }

        if (backup != null) {
            try {
                WeatherSnapshot result = backup.fetch(city);
                return RefreshResult.success(result.withSource(backup.name(), result.attribution), true, primaryUnavailable);
            } catch (Exception ignored) {
                // The caller keeps showing the last cached snapshot, if any.
            }
        }

        if (primaryUnavailable) {
            return RefreshResult.failure("和风天气和 Open-Meteo 暂时都无法连接");
        }
        return RefreshResult.failure("Open-Meteo 暂时无法连接");
    }
}
