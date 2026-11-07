package cn.codex.qingfengweather.data;

public final class RefreshResult {
    public final WeatherSnapshot snapshot;
    public final boolean usedBackup;
    public final boolean primaryUnavailable;
    public final String errorMessage;

    private RefreshResult(WeatherSnapshot snapshot, boolean usedBackup, boolean primaryUnavailable, String errorMessage) {
        this.snapshot = snapshot;
        this.usedBackup = usedBackup;
        this.primaryUnavailable = primaryUnavailable;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static RefreshResult success(WeatherSnapshot snapshot, boolean usedBackup, boolean primaryUnavailable) {
        return new RefreshResult(snapshot, usedBackup, primaryUnavailable, "");
    }

    public static RefreshResult failure(String message) {
        return new RefreshResult(null, false, false, message);
    }
}
