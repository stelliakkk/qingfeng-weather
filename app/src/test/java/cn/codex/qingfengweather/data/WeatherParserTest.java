package cn.codex.qingfengweather.data;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class WeatherParserTest {
    @Test
    public void qweatherParserMapsCurrentAndSevenDayForecast() throws Exception {
        JSONObject current = new JSONObject("{"
                + "'condition':{'text':'多云','code':'101'},"
                + "'temperature':{'value':21.5},'feelsLike':{'value':20.0},"
                + "'humidity':0.68,'wind':{'speed':{'value':2.5},'direction':{'degree':90}},"
                + "'precipitation':{'amount':{'value':0.2}},'updateTime':'2026-11-08T09:00+08:00',"
                + "'metadata':{'attributions':['QWeather']}}".replace('\'', '"'));
        JSONObject daily = new JSONObject("{ 'days': ["
                + "{'daytime':{'forecastStartTime':'2026-11-08T06:00+08:00','condition':{'text':'晴','code':'100'}},"
                + "'temperatureMax':{'value':25},'temperatureMin':{'value':14}},"
                + "{'forecastStartTime':'2026-11-09T00:00+08:00','daytime':{'condition':{'text':'小雨','code':'305'}},"
                + "'temperatureMax':{'value':20},'temperatureMin':{'value':12}}]}".replace('\'', '"'));

        WeatherSnapshot parsed = QWeatherProvider.parseSnapshot(current, daily);

        assertEquals(21.5, parsed.temperatureC, 0.001);
        assertEquals(20.0, parsed.feelsLikeC, 0.001);
        assertEquals(68, parsed.humidityPercent);
        assertEquals(9.0, parsed.windKmh, 0.001);
        assertEquals(90, parsed.windDirection);
        assertEquals("QWeather", parsed.attribution);
        assertEquals(2, parsed.days.size());
        assertEquals("2026-11-08", parsed.days.get(0).date);
        assertEquals(25, parsed.days.get(0).highC, 0.001);
        assertEquals(12, parsed.days.get(1).lowC, 0.001);
    }

    @Test
    public void qweatherParserMapsHourlyAlertsAirAndIndices() throws Exception {
        JSONObject current = new JSONObject("{'condition':{'text':'晴','code':'100'},"
                + "'temperature':{'value':19},'humidity':0.5}".replace('\'', '"'));
        JSONObject daily = new JSONObject("{'days':[]}".replace('\'', '"'));
        JSONObject hourly = new JSONObject("{'hours':["
                + "{'forecastTime':'2026-11-08T10:00+08:00','condition':{'text':'晴','code':'100'},"
                + "'temperature':{'value':19},'precipitation':{'probability':0.1}}]}".replace('\'', '"'));
        JSONObject alerts = new JSONObject("{'alerts':[{'headline':'暴雨黄色预警','senderName':'气象台',"
                + "'severity':'黄色','eventType':{'name':'暴雨'},'description':'注意防范',"
                + "'instruction':'减少外出','expireTime':'2026-11-08T18:00+08:00'}]}".replace('\'', '"'));
        JSONObject air = new JSONObject("{'indexes':[{'code':'cn-mee','aqi':'65','aqiDisplay':'65',"
                + "'category':'良','primaryPollutant':{'name':'PM2.5'},"
                + "'color':{'red':120,'green':190,'blue':80},'health':{'advice':{'generalPopulation':'正常活动'}}}],"
                + "'pollutants':[{'code':'pm2p5','concentration':{'value':18,'unit':'μg/m³'}}]}"
                .replace('\'', '"'));
        JSONObject indices = new JSONObject("{'daily':[{'name':'穿衣','category':'舒适','text':'适合薄外套'}]}"
                .replace('\'', '"'));

        WeatherSnapshot parsed = QWeatherProvider.parseSnapshot(current, daily, hourly, alerts, air, indices);

        assertEquals(1, parsed.hours.size());
        assertEquals(19, parsed.hours.get(0).temperatureC, 0.001);
        assertEquals(10, parsed.hours.get(0).precipitationProbability);
        assertEquals("暴雨黄色预警", parsed.alerts.get(0).headline);
        assertEquals("65", parsed.airQuality.aqiDisplay);
        assertEquals("18 μg/m³", parsed.airQuality.pm25);
        assertEquals("穿衣", parsed.indices.get(0).name);

        WeatherSnapshot restored = WeatherSnapshot.fromJson(parsed.toJson());
        assertEquals(1, restored.hours.size());
        assertEquals(1, restored.alerts.size());
        assertNotNull(restored.airQuality);
        assertEquals("65", restored.airQuality.aqiDisplay);
        assertEquals(1, restored.indices.size());
    }

    @Test
    public void qweatherCitySearchKeepsAdministrativePathsAndRemovesDuplicates() throws Exception {
        JSONObject response = new JSONObject("{'location':["
                + "{'id':'WX001','name':'滨湖区','adm2':'无锡市','adm1':'江苏省','country':'中国','lat':'31.5','lon':'120.2'},"
                + "{'id':'WX001','name':'滨湖区','adm2':'无锡市','adm1':'江苏省','country':'中国','lat':'31.5','lon':'120.2'},"
                + "{'id':'WX002','name':'滨湖区','adm2':'无锡市','adm1':'江苏省','country':'中国','lat':'31.6','lon':'120.3'},"
                + "{'id':'HF001','name':'滨湖区','adm2':'合肥市','adm1':'安徽省','country':'中国','lat':'31.8','lon':'117.2'}]}"
                .replace('\'', '"'));

        java.util.List<City> cities = QWeatherProvider.parseCities(response);

        assertEquals(2, cities.size());
        assertEquals("滨湖区 · 无锡市 · 江苏省", cities.get(0).displayName());
        assertEquals("滨湖区 · 合肥市 · 安徽省", cities.get(1).displayName());
    }

    @Test
    public void openMeteoParserMapsCurrentAndDailyArrays() throws Exception {
        JSONObject response = new JSONObject("{"
                + "'current':{'temperature_2m':17.4,'relative_humidity_2m':52,'apparent_temperature':16.2,"
                + "'precipitation':0.0,'weather_code':2,'wind_speed_10m':11,'wind_direction_10m':180,'time':'2026-11-08T10:00'},"
                + "'hourly':{'time':['2026-11-08T10:00','2026-11-08T11:00'],"
                + "'temperature_2m':[17,18],'weather_code':[2,3],'precipitation_probability':[5,15]},"
                + "'daily':{'time':['2026-11-08','2026-11-09'],'weather_code':[2,61],"
                + "'temperature_2m_max':[20,18],'temperature_2m_min':[10,9]}}".replace('\'', '"'));

        WeatherSnapshot parsed = OpenMeteoProvider.parseForecast(response);

        assertEquals(17.4, parsed.temperatureC, 0.001);
        assertEquals(52, parsed.humidityPercent);
        assertEquals(11, parsed.windKmh, 0.001);
        assertEquals("局部多云", parsed.condition);
        assertEquals("2026-11-09", parsed.days.get(1).date);
        assertEquals("下雨", parsed.days.get(1).condition);
        assertEquals(18, parsed.days.get(1).highC, 0.001);
        assertEquals(2, parsed.hours.size());
        assertEquals(15, parsed.hours.get(1).precipitationProbability);
    }

    @Test
    public void snapshotCacheJsonRoundTripsForecast() throws Exception {
        JSONObject response = new JSONObject("{'current':{'temperature_2m':5,'relative_humidity_2m':45,"
                + "'apparent_temperature':3,'weather_code':0,'time':'2026-11-08T10:00'},"
                + "'daily':{'time':['2026-11-08'],'weather_code':[0],"
                + "'temperature_2m_max':[7],'temperature_2m_min':[1]}}".replace('\'', '"'));
        WeatherSnapshot original = OpenMeteoProvider.parseForecast(response);

        WeatherSnapshot restored = WeatherSnapshot.fromJson(original.toJson());

        assertEquals(original.temperatureC, restored.temperatureC, 0.001);
        assertEquals(original.condition, restored.condition);
        assertEquals(original.days.size(), restored.days.size());
        assertEquals(original.days.get(0).highC, restored.days.get(0).highC, 0.001);
    }

    @Test
    public void providerCredentialRequiresBothHostAndKey() {
        assertFalse(new QWeatherCredentials("host.example", "").isConfigured());
        assertFalse(new QWeatherCredentials("", "secret").isConfigured());
        assertTrue(new QWeatherCredentials("host.example", "secret").isConfigured());
    }
}
