package cn.codex.qingfengweather.data;

public final class WeatherAlert {
    public final String headline;
    public final String sender;
    public final String severity;
    public final String eventName;
    public final String description;
    public final String instruction;
    public final String effectiveTime;
    public final String expireTime;
    public final String color;

    public WeatherAlert(String headline, String sender, String severity, String eventName,
                        String description, String instruction, String effectiveTime,
                        String expireTime, String color) {
        this.headline = safe(headline);
        this.sender = safe(sender);
        this.severity = safe(severity);
        this.eventName = safe(eventName);
        this.description = safe(description);
        this.instruction = safe(instruction);
        this.effectiveTime = safe(effectiveTime);
        this.expireTime = safe(expireTime);
        this.color = safe(color);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
