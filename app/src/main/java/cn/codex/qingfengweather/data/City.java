package cn.codex.qingfengweather.data;

import java.util.Locale;

public final class City {
    public final String name;
    public final String region;
    public final String admin1;
    public final String country;
    public final String locationId;
    public final String type;
    public final double latitude;
    public final double longitude;
    public final String timezone;

    public City(String name, String region, String country, double latitude, double longitude, String timezone) {
        this(name, region, "", country, latitude, longitude, timezone, "", "");
    }

    public City(String name, String region, String admin1, String country,
                double latitude, double longitude, String timezone, String locationId, String type) {
        this.name = name == null ? "" : name;
        this.region = region == null ? "" : region;
        this.admin1 = admin1 == null ? "" : admin1;
        this.country = country == null ? "" : country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone == null ? "auto" : timezone;
        this.locationId = locationId == null ? "" : locationId;
        this.type = type == null ? "" : type;
    }

    public String displayName() {
        StringBuilder result = new StringBuilder();
        appendDistinct(result, name);
        appendDistinct(result, region);
        appendDistinct(result, admin1);
        if (!country.isEmpty() && !country.equals("中国")) appendDistinct(result, country);
        return result.toString();
    }

    private static void appendDistinct(StringBuilder result, String value) {
        if (value == null || value.trim().isEmpty()) return;
        String normalized = simplify(value.trim());
        if (result.length() > 0) {
            for (String part : result.toString().split(" · ")) {
                if (simplify(part).equals(normalized)) return;
            }
            result.append(" · ");
        }
        result.append(value.trim());
    }

    private static String simplify(String value) {
        return value.replaceAll("(特别行政区|自治区|自治州|省|市|区|县)$", "");
    }

    public String coordinates() {
        return String.format(Locale.US, "%.4f, %.4f", latitude, longitude);
    }
}
