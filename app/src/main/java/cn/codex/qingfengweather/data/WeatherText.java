package cn.codex.qingfengweather.data;

public final class WeatherText {
    private WeatherText() {}

    public static String fromWmoCode(int code) {
        if (code == 0) return "晴";
        if (code == 1) return "大部晴朗";
        if (code == 2) return "局部多云";
        if (code == 3) return "阴";
        if (code == 45 || code == 48) return "有雾";
        if (code >= 51 && code <= 57) return "毛毛雨";
        if (code >= 61 && code <= 67) return "下雨";
        if (code >= 71 && code <= 77) return "下雪";
        if (code >= 80 && code <= 82) return "阵雨";
        if (code == 85 || code == 86) return "阵雪";
        if (code == 95 || code == 96 || code == 99) return "雷雨";
        return "天气";
    }

    public static String emoji(int code, String condition) {
        String text = condition == null ? "" : condition;
        if (text.contains("雷")) return "⛈";
        if (text.contains("雪")) return "❄";
        if (text.contains("雨") || text.contains("毛毛")) return "🌧";
        if (text.contains("雾") || text.contains("霾")) return "🌫";
        if (text.contains("阴") || text.contains("云")) return "☁";
        if (code == 0 || text.contains("晴")) return "☀";
        return "🌤";
    }

    public static String windDirection(int degrees) {
        if (degrees < 0) return "—";
        String[] directions = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        int index = ((degrees + 22) % 360) / 45;
        return directions[index];
    }

    public static String weekday(String date) {
        if (date == null || date.length() < 10) return "—";
        try {
            java.time.LocalDate parsed = java.time.LocalDate.parse(date.substring(0, 10));
            return parsed.equals(java.time.LocalDate.now()) ? "今天" : new String[]{"周一", "周二", "周三", "周四", "周五", "周六", "周日"}[parsed.getDayOfWeek().getValue() - 1];
        } catch (RuntimeException ignored) {
            return date.substring(5, Math.min(10, date.length()));
        }
    }
}
