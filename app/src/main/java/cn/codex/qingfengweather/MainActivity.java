package cn.codex.qingfengweather;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import cn.codex.qingfengweather.data.City;
import cn.codex.qingfengweather.data.CredentialStore;
import cn.codex.qingfengweather.data.DayForecast;
import cn.codex.qingfengweather.data.HourForecast;
import cn.codex.qingfengweather.data.OpenMeteoProvider;
import cn.codex.qingfengweather.data.AirQuality;
import cn.codex.qingfengweather.data.QWeatherCredentials;
import cn.codex.qingfengweather.data.QWeatherProvider;
import cn.codex.qingfengweather.data.RefreshResult;
import cn.codex.qingfengweather.data.WeatherAlert;
import cn.codex.qingfengweather.data.WeatherCache;
import cn.codex.qingfengweather.data.WeatherIndex;
import cn.codex.qingfengweather.data.WeatherRepository;
import cn.codex.qingfengweather.data.WeatherSnapshot;
import cn.codex.qingfengweather.data.WeatherText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lightweight, ad-free weather screen. Cached data is rendered before network refresh starts. */
public final class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(39, 99, 173);
    private static final int INK = Color.rgb(31, 48, 70);
    private static final int MUTED = Color.rgb(111, 128, 148);
    private static final long REFRESH_INTERVAL_MILLIS = 30 * 60 * 1000L;
    private static final long CACHE_FRESH_MILLIS = REFRESH_INTERVAL_MILLIS;
    private static final String[] ENCOURAGEMENTS = {
            "慢一点也没关系，正在前进就很好。",
            "把今天过好，就是在向未来靠近。",
            "愿你心里有光，脚下有路。",
            "今天也有值得期待的小事。",
            "不必着急，属于你的风景正在路上。"
    };

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable scheduledRefresh = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing()) {
                refreshWeather(true);
                mainHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS);
            }
        }
    };
    private WeatherCache cache;
    private City city;
    private WeatherSnapshot snapshot;
    private LinearLayout page;
    private TextView cityLabel;
    private TextView temperatureLabel;
    private TextView conditionLabel;
    private TextView updatedLabel;
    private TextView sourceLabel;
    private TextView detailsLabel;
    private TextView quoteLabel;
    private LinearLayout forecastRows;
    private TextView messageLabel;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout alertCard;
    private TextView alertSummary;
    private LinearLayout airCard;
    private TextView airValue;
    private TextView airSummary;
    private LinearLayout indicesCard;
    private LinearLayout indicesRows;
    private LinearLayout hourlyRows;
    private boolean requestInFlight;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        cache = new WeatherCache(this);
        city = cache.getCity();
        if (city == null) city = new City("北京", "北京市", "中国", 39.9042, 116.4074, "Asia/Shanghai");
        snapshot = cache.getSnapshot();
        buildScreen();
        render();
        if (snapshot != null) setMessage("下拉页面可立即更新天气");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!cache.isFresh(CACHE_FRESH_MILLIS)) refreshWeather(false);
        mainHandler.removeCallbacks(scheduledRefresh);
        mainHandler.postDelayed(scheduledRefresh, REFRESH_INTERVAL_MILLIS);
    }

    @Override
    protected void onPause() {
        mainHandler.removeCallbacks(scheduledRefresh);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(scheduledRefresh);
        io.shutdownNow();
        super.onDestroy();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Color.rgb(242, 246, 252));
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        cityLabel = text("北京", 22, INK, true);
        cityLabel.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(cityLabel, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView choose = smallButton("城市⌄");
        choose.setOnClickListener(v -> showCitySearch());
        header.addView(choose, wrap());
        TextView settings = smallButton("设置");
        settings.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams settingsParams = wrap();
        settingsParams.leftMargin = dp(8);
        header.addView(settings, settingsParams);
        page.addView(header, matchWrap());

        LinearLayout current = new LinearLayout(this);
        current.setOrientation(LinearLayout.VERTICAL);
        current.setGravity(Gravity.CENTER_HORIZONTAL);
        current.setPadding(dp(20), dp(22), dp(20), dp(20));
        current.setBackground(roundedGradient(
                new int[]{Color.WHITE, Color.rgb(244, 249, 255)}, dp(26)));
        current.setElevation(dp(2));
        LinearLayout.LayoutParams currentParams = matchWrap();
        currentParams.topMargin = dp(14);
        page.addView(current, currentParams);

        String encouragement = ENCOURAGEMENTS[new Random().nextInt(ENCOURAGEMENTS.length)];
        quoteLabel = text("“" + encouragement + "”", 14, MUTED, false);
        quoteLabel.setGravity(Gravity.CENTER);
        quoteLabel.setMaxLines(2);
        current.addView(quoteLabel, matchWrap());

        temperatureLabel = text("--°", 82, INK, false);
        temperatureLabel.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        temperatureLabel.setGravity(Gravity.CENTER);
        temperatureLabel.setIncludeFontPadding(true);
        temperatureLabel.setMinWidth(dp(210));
        LinearLayout.LayoutParams tempParams = new LinearLayout.LayoutParams(-1, dp(122));
        tempParams.topMargin = dp(7);
        current.addView(temperatureLabel, tempParams);
        conditionLabel = text("等待天气数据", 19, INK, true);
        conditionLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams conditionParams = wrap();
        conditionParams.topMargin = -dp(5);
        current.addView(conditionLabel, conditionParams);
        detailsLabel = text("体感 --°    湿度 --%\n风速 -- km/h    降水 -- mm", 14, MUTED, false);
        detailsLabel.setGravity(Gravity.CENTER);
        detailsLabel.setLineSpacing(dp(7), 1.0f);
        LinearLayout.LayoutParams detailParams = wrap();
        detailParams.topMargin = dp(18);
        current.addView(detailsLabel, detailParams);

        LinearLayout updateRow = new LinearLayout(this);
        updateRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowParams = matchWrap();
        rowParams.topMargin = dp(17);
        current.addView(updateRow, rowParams);
        updatedLabel = text("尚未更新", 12, MUTED, false);
        updatedLabel.setGravity(Gravity.CENTER);
        updateRow.addView(updatedLabel, matchWrap());

        messageLabel = text("", 12, MUTED, false);
        messageLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = matchWrap();
        messageParams.topMargin = dp(10);
        current.addView(messageLabel, messageParams);

        alertCard = cardLayout();
        LinearLayout.LayoutParams alertParams = matchWrap();
        alertParams.topMargin = dp(14);
        alertCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        page.addView(alertCard, alertParams);
        TextView alertHeading = text("天气预警", 16, INK, true);
        alertCard.addView(alertHeading, matchWrap());
        alertSummary = text("正在检查预警…", 13, MUTED, false);
        LinearLayout.LayoutParams alertSummaryParams = matchWrap();
        alertSummaryParams.topMargin = dp(6);
        alertCard.addView(alertSummary, alertSummaryParams);
        alertCard.setOnClickListener(v -> showAlertDetails());

        LinearLayout informationRow = new LinearLayout(this);
        informationRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams informationParams = matchWrap();
        informationParams.topMargin = dp(12);
        page.addView(informationRow, informationParams);

        airCard = cardLayout();
        airCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams airParams = new LinearLayout.LayoutParams(0, -2, 1);
        airParams.rightMargin = dp(6);
        informationRow.addView(airCard, airParams);
        airCard.addView(text("空气质量", 15, INK, true), matchWrap());
        airValue = text("--", 30, BLUE, true);
        LinearLayout.LayoutParams airValueParams = matchWrap();
        airValueParams.topMargin = dp(5);
        airCard.addView(airValue, airValueParams);
        airSummary = text("AQI · 暂无数据", 12, MUTED, false);
        airCard.addView(airSummary, matchWrap());
        airCard.setOnClickListener(v -> showAirDetails());

        indicesCard = cardLayout();
        indicesCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams indexCardParams = new LinearLayout.LayoutParams(0, -2, 1);
        indexCardParams.leftMargin = dp(6);
        informationRow.addView(indicesCard, indexCardParams);
        indicesCard.addView(text("生活指数", 15, INK, true), matchWrap());
        indicesRows = new LinearLayout(this);
        indicesRows.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams indicesRowsParams = matchWrap();
        indicesRowsParams.topMargin = dp(5);
        indicesCard.addView(indicesRows, indicesRowsParams);
        indicesCard.setOnClickListener(v -> showIndexDetails());

        TextView hourlyTitle = text("未来 24 小时", 18, INK, true);
        LinearLayout.LayoutParams hourlyTitleParams = matchWrap();
        hourlyTitleParams.topMargin = dp(22);
        hourlyTitleParams.bottomMargin = dp(10);
        page.addView(hourlyTitle, hourlyTitleParams);
        LinearLayout hourlyCard = cardLayout();
        hourlyCard.setPadding(dp(13), dp(14), dp(13), dp(14));
        page.addView(hourlyCard, matchWrap());
        HorizontalScrollView hourlyScroll = new HorizontalScrollView(this);
        hourlyScroll.setHorizontalScrollBarEnabled(false);
        hourlyRows = new LinearLayout(this);
        hourlyRows.setOrientation(LinearLayout.HORIZONTAL);
        hourlyScroll.addView(hourlyRows, new HorizontalScrollView.LayoutParams(-2, -2));
        hourlyCard.addView(hourlyScroll, matchWrap());

        TextView forecastTitle = text("未来 7 天", 18, INK, true);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(22);
        titleParams.bottomMargin = dp(10);
        page.addView(forecastTitle, titleParams);

        LinearLayout forecastCard = new LinearLayout(this);
        forecastCard.setOrientation(LinearLayout.VERTICAL);
        forecastCard.setPadding(dp(16), dp(5), dp(16), dp(5));
        forecastCard.setBackground(rounded(Color.WHITE, dp(20)));
        forecastCard.setElevation(dp(2));
        page.addView(forecastCard, matchWrap());
        forecastRows = forecastCard;

        sourceLabel = text("天气数据来源将在这里显示", 11, MUTED, false);
        sourceLabel.setGravity(Gravity.CENTER);
        sourceLabel.setLinkTextColor(BLUE);
        LinearLayout.LayoutParams sourceParams = matchWrap();
        sourceParams.topMargin = dp(14);
        page.addView(sourceLabel, sourceParams);

        swipeRefresh = new SwipeRefreshLayout(this);
        swipeRefresh.setColorSchemeColors(BLUE, Color.rgb(90, 154, 218));
        swipeRefresh.setProgressBackgroundColorSchemeColor(Color.WHITE);
        swipeRefresh.setOnRefreshListener(() -> refreshWeather(true));
        swipeRefresh.addView(scroll, new SwipeRefreshLayout.LayoutParams(-1, -1));
        setContentView(swipeRefresh);
    }

    private void render() {
        cityLabel.setText(city.displayName());
        if (snapshot == null) {
            temperatureLabel.setText("--°");
            conditionLabel.setText("天气数据加载中");
            detailsLabel.setText("体感 --°    湿度 --%\n风速 -- km/h    降水 -- mm");
            updatedLabel.setText("尚无本地缓存");
            sourceLabel.setText("首次使用会从天气服务下载数据；之后优先显示缓存");
            renderForecast(null);
            renderSupplemental(null);
            return;
        }
        temperatureLabel.setText(String.format(Locale.getDefault(), "%.0f°", snapshot.temperatureC));
        conditionLabel.setText(WeatherText.emoji(snapshot.weatherCode, snapshot.condition) + "  " + snapshot.condition);
        detailsLabel.setText(String.format(Locale.getDefault(),
                "体感 %.0f°    湿度 %d%%\n风速 %.0f km/h    降水 %.1f mm",
                snapshot.feelsLikeC, snapshot.humidityPercent, snapshot.windKmh, snapshot.precipitationMm));
        long saved = cache.getSnapshotSavedAt();
        String savedTime = saved > 0
                ? new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(saved))
                : "时间未知";
        updatedLabel.setText("更新于 " + savedTime + " · " + snapshot.source);
        sourceLabel.setText(snapshot.attribution.isEmpty()
                ? "数据来源：" + snapshot.source
                : "数据来源：" + snapshot.source + "\n" + snapshot.attribution);
        Linkify.addLinks(sourceLabel, Linkify.WEB_URLS);
        renderForecast(snapshot);
        renderSupplemental(snapshot);
    }

    private void renderForecast(WeatherSnapshot weather) {
        forecastRows.removeAllViews();
        if (weather == null || weather.days.isEmpty()) {
            TextView empty = text("暂无预报，刷新后会显示", 14, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(22), 0, dp(22));
            forecastRows.addView(empty);
            return;
        }
        int count = Math.min(7, weather.days.size());
        for (int i = 0; i < count; i++) {
            DayForecast day = weather.days.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(11), 0, dp(11));
            TextView date = text(WeatherText.weekday(day.date), 14, INK, true);
            row.addView(date, new LinearLayout.LayoutParams(dp(52), -2));
            TextView condition = text(WeatherText.emoji(day.weatherCode, day.condition) + " " + day.condition, 13, MUTED, false);
            row.addView(condition, new LinearLayout.LayoutParams(0, -2, 1));
            TextView range = text(formatTemp(day.highC) + " / " + formatTemp(day.lowC), 14, INK, true);
            range.setGravity(Gravity.RIGHT);
            row.addView(range, new LinearLayout.LayoutParams(dp(96), -2));
            forecastRows.addView(row, matchWrap());
            if (i + 1 < count) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.rgb(237, 241, 246));
                forecastRows.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
    }

    private void renderSupplemental(WeatherSnapshot weather) {
        if (weather == null || weather.alerts == null) {
            alertSummary.setText(weather == null
                    ? "天气数据加载后显示预警"
                    : weather.source.contains("Open-Meteo")
                    ? "备用天气源不提供预警数据"
                    : "预警暂未获取，请检查和风 API 权限");
            alertSummary.setTextColor(MUTED);
            alertCard.setBackground(rounded(Color.WHITE, dp(18)));
        } else if (weather.alerts.isEmpty()) {
            alertSummary.setText("当前没有生效中的天气预警");
            alertSummary.setTextColor(Color.rgb(51, 125, 91));
            alertCard.setBackground(rounded(Color.rgb(244, 251, 247), dp(18)));
        } else {
            WeatherAlert first = weather.alerts.get(0);
            String more = weather.alerts.size() > 1 ? "（共 " + weather.alerts.size() + " 条）" : "";
            alertSummary.setText(first.headline + more + " · 点开查看详情");
            alertSummary.setTextColor(Color.rgb(151, 65, 45));
            alertCard.setBackground(rounded(Color.rgb(255, 246, 242), dp(18)));
        }

        AirQuality air = weather == null ? null : weather.airQuality;
        if (air == null) {
            airValue.setText("--");
            airValue.setTextColor(BLUE);
            airSummary.setText(weather != null && weather.source.contains("Open-Meteo")
                    ? "备用源无空气质量数据" : "AQI · 等待空气质量数据");
        } else {
            airValue.setText(air.aqiDisplay.isEmpty() ? "--" : air.aqiDisplay);
            airValue.setTextColor(Color.rgb(air.red, air.green, air.blue));
            String pollutant = air.primaryPollutant.isEmpty() ? "暂无首要污染物" : air.primaryPollutant;
            airSummary.setText(air.category + " · " + pollutant);
        }

        indicesRows.removeAllViews();
        List<WeatherIndex> indices = weather == null ? null : weather.indices;
        if (indices == null || indices.isEmpty()) {
            String unavailable = weather != null && weather.source.contains("Open-Meteo")
                    ? "备用源不提供生活指数" : "穿衣 · 运动 · 洗车";
            indicesRows.addView(text(unavailable, 12, MUTED, false), matchWrap());
        } else {
            int count = Math.min(3, indices.size());
            for (int i = 0; i < count; i++) {
                WeatherIndex index = indices.get(i);
                TextView row = text(index.name + " · " + index.category, 12, MUTED, false);
                row.setMaxLines(1);
                row.setEllipsize(android.text.TextUtils.TruncateAt.END);
                indicesRows.addView(row, matchWrap());
            }
        }

        hourlyRows.removeAllViews();
        List<HourForecast> hours = weather == null ? null : weather.hours;
        if (hours == null || hours.isEmpty()) {
            TextView empty = text("暂无逐小时预报", 13, MUTED, false);
            empty.setPadding(dp(10), dp(14), dp(10), dp(14));
            hourlyRows.addView(empty);
            return;
        }
        int count = Math.min(24, hours.size());
        for (int i = 0; i < count; i++) {
            HourForecast hour = hours.get(i);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(dp(8), dp(8), dp(8), dp(8));
            item.setBackground(rounded(Color.rgb(245, 248, 253), dp(14)));
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(68), dp(108));
            if (i > 0) itemParams.leftMargin = dp(7);
            hourlyRows.addView(item, itemParams);

            TextView time = text(i == 0 ? "现在" : hourLabel(hour.time), 11, MUTED, true);
            time.setGravity(Gravity.CENTER);
            item.addView(time, matchWrap());
            TextView icon = text(WeatherText.emoji(hour.weatherCode, hour.condition), 19, INK, false);
            icon.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams iconParams = matchWrap();
            iconParams.topMargin = dp(5);
            item.addView(icon, iconParams);
            TextView temperature = text(formatTemp(hour.temperatureC), 14, INK, true);
            temperature.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams temperatureParams = matchWrap();
            temperatureParams.topMargin = dp(4);
            item.addView(temperature, temperatureParams);
            String rain = hour.precipitationProbability < 0 ? "" : hour.precipitationProbability + "%";
            TextView precipitation = text(rain, 10, BLUE, false);
            precipitation.setGravity(Gravity.CENTER);
            item.addView(precipitation, matchWrap());
        }
    }

    private static String hourLabel(String value) {
        if (value == null) return "--:--";
        int t = value.indexOf('T');
        if (t >= 0 && value.length() >= t + 6) return value.substring(t + 1, t + 6);
        return value.length() >= 5 ? value.substring(value.length() - 5) : value;
    }

    private void showAlertDetails() {
        if (snapshot == null || snapshot.alerts == null || snapshot.alerts.isEmpty()) {
            showInfoDialog("天气预警", snapshot == null || snapshot.alerts == null
                    ? snapshot != null && snapshot.source.contains("Open-Meteo")
                    ? "当前备用天气源不提供预警数据。配置和风天气并启用“天气预警”后可查看。"
                    : "预警接口尚未返回数据。请确认和风天气 API 限制已启用“天气预警”，并稍后下拉刷新。"
                    : "当前城市暂无生效中的天气预警。");
            return;
        }
        StringBuilder details = new StringBuilder("预警信息可能存在延迟，请以当地气象部门发布为准。\n\n");
        for (WeatherAlert alert : snapshot.alerts) {
            details.append(alert.headline).append('\n');
            if (!alert.eventName.isEmpty()) details.append("类型：").append(alert.eventName).append('\n');
            if (!alert.severity.isEmpty()) details.append("级别：").append(alert.severity).append('\n');
            if (!alert.sender.isEmpty()) details.append("发布：").append(alert.sender).append('\n');
            if (!alert.expireTime.isEmpty()) details.append("预计结束：").append(alert.expireTime).append('\n');
            if (!alert.description.isEmpty()) details.append('\n').append(alert.description).append('\n');
            if (!alert.instruction.isEmpty()) details.append('\n').append("防御建议：").append(alert.instruction);
            details.append("\n\n");
        }
        showInfoDialog("天气预警详情", details.toString().trim());
    }

    private void showAirDetails() {
        AirQuality air = snapshot == null ? null : snapshot.airQuality;
        if (air == null) {
            showInfoDialog("空气质量", snapshot != null && snapshot.source.contains("Open-Meteo")
                    ? "当前备用天气源不提供空气质量数据。配置和风天气并启用“空气质量”后可查看。"
                    : "尚未获取到和风天气空气质量数据。请确认 API 限制已启用“空气质量”，并稍后下拉刷新。");
            return;
        }
        StringBuilder details = new StringBuilder("AQI：").append(air.aqiDisplay)
                .append("（").append(air.category).append("）\n");
        if (!air.primaryPollutant.isEmpty()) details.append("首要污染物：").append(air.primaryPollutant).append('\n');
        if (!air.pm25.isEmpty()) details.append("PM2.5：").append(air.pm25).append('\n');
        if (!air.pm10.isEmpty()) details.append("PM10：").append(air.pm10).append('\n');
        if (!air.advice.isEmpty()) details.append('\n').append("健康提示：").append(air.advice);
        showInfoDialog("空气质量", details.toString());
    }

    private void showIndexDetails() {
        List<WeatherIndex> indices = snapshot == null ? null : snapshot.indices;
        if (indices == null || indices.isEmpty()) {
            showInfoDialog("生活指数", snapshot != null && snapshot.source.contains("Open-Meteo")
                    ? "当前备用天气源不提供生活指数。配置和风天气并启用“天气指数”后可查看。"
                    : "尚未获取到和风天气生活指数。请确认 API 限制已启用“天气指数”，并稍后下拉刷新。");
            return;
        }
        StringBuilder details = new StringBuilder();
        for (WeatherIndex index : indices) {
            details.append(index.name).append("：").append(index.category);
            if (!index.text.isEmpty()) details.append('\n').append(index.text);
            details.append("\n\n");
        }
        showInfoDialog("今日生活指数", details.toString().trim());
    }

    private void showInfoDialog(String title, String body) {
        TextView content = text(body, 14, INK, false);
        content.setLineSpacing(dp(4), 1.0f);
        content.setPadding(dp(4), dp(4), dp(4), dp(4));
        ScrollView scroll = new ScrollView(this);
        scroll.setPadding(dp(20), dp(8), dp(20), dp(8));
        scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .create();
        dialog.show();
        roundDialog(dialog);
    }

    private void refreshWeather(boolean force) {
        if (requestInFlight) {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        if (!force && cache.isFresh(CACHE_FRESH_MILLIS)) {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        requestInFlight = true;
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        setMessage("正在获取天气…");
        final City requestedCity = city;
        io.execute(() -> {
            QWeatherCredentials credentials = CredentialStore.load(getApplicationContext());
            boolean wasQWeatherConfigured = credentials.isConfigured();
            WeatherRepository repository = new WeatherRepository(
                    new QWeatherProvider(credentials), new OpenMeteoProvider());
            RefreshResult result = repository.refresh(requestedCity);
            runOnUiThread(() -> {
                requestInFlight = false;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (requestedCity != city) {
                    refreshWeather(true);
                    return;
                }
                if (result.snapshot != null) {
                    snapshot = result.snapshot;
                    cache.saveSnapshot(snapshot);
                    render();
                    if (result.usedBackup) {
                        setMessage(wasQWeatherConfigured
                                ? "和风天气暂不可用，已自动切换到 Open-Meteo"
                                : "尚未配置和风天气，当前使用 Open-Meteo");
                    } else {
                        setMessage("天气已更新");
                    }
                } else {
                    setMessage(result.errorMessage + (snapshot == null ? "" : "；继续显示上次缓存"));
                    if (snapshot == null) conditionLabel.setText("暂时无法获取天气");
                }
            });
        });
    }

    private void showCitySearch() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(4));
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("输入城市或区县，例如：江苏无锡滨湖区");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setBackground(outlinedInput());
        content.addView(input, matchWrap());
        TextView search = smallButton("搜索城市");
        LinearLayout.LayoutParams searchParams = matchWrap();
        searchParams.topMargin = dp(8);
        content.addView(search, searchParams);
        QWeatherCredentials searchCredentials = CredentialStore.load(this);
        TextView status = text(searchCredentials.isConfigured()
                ? "优先使用和风 GeoAPI，可搜索省、市、区县"
                : "未配置和风时使用 Open-Meteo 城市搜索", 12, MUTED, false);
        status.setPadding(0, dp(7), 0, dp(4));
        content.addView(status, matchWrap());
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results, matchWrap());
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择城市")
                .setView(scroll)
                .setNegativeButton("关闭", null)
                .create();
        search.setOnClickListener(v -> {
            String query = input.getText().toString().trim();
            if (query.isEmpty()) {
                input.setError("请输入城市名");
                return;
            }
            search.setEnabled(false);
            status.setText("正在搜索…");
            results.removeAllViews();
            io.execute(() -> {
                List<City> found = null;
                String providerLabel = "";
                String failure = "";
                QWeatherCredentials credentials = CredentialStore.load(getApplicationContext());
                if (credentials.isConfigured()) {
                    try {
                        found = new QWeatherProvider(credentials).searchCities(query);
                        if (found != null && !found.isEmpty()) providerLabel = "和风 GeoAPI";
                    } catch (Exception e) {
                        failure = e.getMessage() == null ? "和风 GeoAPI 不可用" : e.getMessage();
                    }
                }
                if (found == null || found.isEmpty()) {
                    try {
                        found = new OpenMeteoProvider().searchCities(query);
                        if (found != null && !found.isEmpty()) providerLabel = "Open-Meteo 备用";
                    } catch (Exception e) {
                        if (failure.isEmpty()) failure = e.getMessage() == null ? "城市服务不可用" : e.getMessage();
                    }
                }
                List<City> finalFound = found;
                String finalProviderLabel = providerLabel;
                String finalFailure = failure;
                runOnUiThread(() -> {
                    search.setEnabled(true);
                    if (finalFound == null || finalFound.isEmpty()) {
                        status.setText(finalFailure.isEmpty()
                                ? "没有找到城市，请输入更完整的省、市或区县名"
                                : "搜索失败，可检查网络或和风 GeoAPI 权限后重试");
                        return;
                    }
                    status.setText("结果来自 " + finalProviderLabel + " · 点选省市区路径正确的一项：");
                    for (City result : finalFound) {
                        TextView item = smallButton(result.displayName());
                        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                        LinearLayout.LayoutParams itemParams = matchWrap();
                        itemParams.topMargin = dp(5);
                        results.addView(item, itemParams);
                        item.setOnClickListener(view -> {
                            city = result;
                            cache.saveCity(result);
                            cache.clearSnapshot();
                            snapshot = null;
                            render();
                            dialog.dismiss();
                            refreshWeather(true);
                        });
                    }
                });
            });
        });
        dialog.show();
        roundDialog(dialog);
    }

    private void showSettings() {
        QWeatherCredentials saved = CredentialStore.load(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));
        TextView explainer = text("填入和风天气控制台提供的 API Host 和 API Key。建议在 API 限制中启用：天气预报、天气预警、空气质量、天气指数、GeoAPI。保存后优先使用和风天气；失败时自动切换 Open-Meteo。密钥仅保存在本机加密区。", 13, MUTED, false);
        explainer.setLineSpacing(dp(3), 1.0f);
        form.addView(explainer, matchWrap());
        EditText host = new EditText(this);
        host.setSingleLine(true);
        host.setHint("API Host（控制台“设置”中复制，不带 https://）");
        host.setText(saved.host);
        host.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        host.setPadding(dp(14), dp(10), dp(14), dp(10));
        host.setBackground(outlinedInput());
        LinearLayout.LayoutParams hostParams = matchWrap();
        hostParams.topMargin = dp(12);
        form.addView(host, hostParams);
        EditText key = new EditText(this);
        key.setSingleLine(true);
        key.setHint(saved.isConfigured() ? "API Key（留空保留已保存的密钥）" : "API Key（首次配置时必填）");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setPadding(dp(14), dp(10), dp(14), dp(10));
        key.setBackground(outlinedInput());
        LinearLayout.LayoutParams keyParams = matchWrap();
        keyParams.topMargin = dp(6);
        form.addView(key, keyParams);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("天气服务设置")
                .setView(form)
                .setNegativeButton("取消", null)
                .setNeutralButton("清除和风配置", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String hostValue = host.getText().toString().trim();
                String keyValue = key.getText().toString().trim();
                if (!hostValue.isEmpty() && keyValue.isEmpty() && saved.apiKey.isEmpty()) {
                    key.setError("首次配置请输入 API Key，或清空 Host 使用备用源");
                    return;
                }
                if (!hostValue.isEmpty() && keyValue.isEmpty()) keyValue = saved.apiKey;
                if (hostValue.isEmpty()) keyValue = "";
                try {
                    CredentialStore.save(this, hostValue, keyValue);
                    dialog.dismiss();
                    setMessage(keyValue.isEmpty() ? "已设置为 Open-Meteo 天气源" : "和风天气配置已保存");
                    refreshWeather(true);
                } catch (Exception e) {
                    Toast.makeText(this, "无法安全保存密钥，请重试", Toast.LENGTH_LONG).show();
                }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                try {
                    CredentialStore.save(this, "", "");
                    dialog.dismiss();
                    setMessage("已清除和风配置，将使用 Open-Meteo");
                    refreshWeather(true);
                } catch (Exception e) {
                    Toast.makeText(this, "清除配置失败", Toast.LENGTH_LONG).show();
                }
            });
        });
        dialog.show();
        roundDialog(dialog);
    }

    private void setMessage(String text) {
        if (messageLabel != null) messageLabel.setText(text == null ? "" : text);
    }

    private static String formatTemp(double value) {
        return Double.isNaN(value) ? "—" : String.format(Locale.getDefault(), "%.0f°", value);
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView smallButton(String label) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(13);
        chip.setTextColor(BLUE);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setMinHeight(dp(44));
        chip.setBackground(rounded(Color.WHITE, dp(14)));
        chip.setElevation(dp(1));
        chip.setClickable(true);
        chip.setFocusable(true);
        return chip;
    }

    private LinearLayout cardLayout() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(Color.WHITE, dp(20)));
        card.setElevation(dp(1));
        return card;
    }

    private void roundDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(rounded(Color.WHITE, dp(24)));
        }
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        return shape;
    }

    private GradientDrawable roundedGradient(int[] colors, int radius) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        shape.setCornerRadius(radius);
        return shape;
    }

    private GradientDrawable outlinedInput() {
        GradientDrawable shape = rounded(Color.WHITE, dp(14));
        shape.setStroke(dp(1), Color.rgb(224, 232, 242));
        return shape;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
