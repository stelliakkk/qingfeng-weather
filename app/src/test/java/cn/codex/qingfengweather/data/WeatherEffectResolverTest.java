package cn.codex.qingfengweather.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WeatherEffectResolverTest {
    @Test public void resolvesSunnyConditions() {
        assertEquals(WeatherEffect.SUNNY, WeatherEffectResolver.resolve("晴"));
        assertEquals(WeatherEffect.SUNNY, WeatherEffectResolver.resolve("Clear sky"));
    }

    @Test public void resolvesPartlyCloudyConditions() {
        assertEquals(WeatherEffect.PARTLY_CLOUDY, WeatherEffectResolver.resolve("局部多云"));
        assertEquals(WeatherEffect.PARTLY_CLOUDY, WeatherEffectResolver.resolve("晴间多云"));
    }

    @Test public void resolvesCloudyConditions() {
        assertEquals(WeatherEffect.CLOUDY, WeatherEffectResolver.resolve("阴"));
        assertEquals(WeatherEffect.CLOUDY, WeatherEffectResolver.resolve("Overcast"));
    }

    @Test public void resolvesFogAndHaze() {
        assertEquals(WeatherEffect.FOG, WeatherEffectResolver.resolve("有雾"));
        assertEquals(WeatherEffect.FOG, WeatherEffectResolver.resolve("Haze"));
    }

    @Test public void resolvesRainAndDrizzle() {
        assertEquals(WeatherEffect.RAIN, WeatherEffectResolver.resolve("毛毛雨"));
        assertEquals(WeatherEffect.RAIN, WeatherEffectResolver.resolve("Rain showers"));
    }

    @Test public void thunderstormTakesPrecedenceOverRain() {
        assertEquals(WeatherEffect.THUNDERSTORM, WeatherEffectResolver.resolve("雷阵雨"));
        assertEquals(WeatherEffect.THUNDERSTORM, WeatherEffectResolver.resolve("Thunderstorm"));
    }

    @Test public void snowTakesPrecedenceOverRain() {
        assertEquals(WeatherEffect.SNOW, WeatherEffectResolver.resolve("雨夹雪"));
        assertEquals(WeatherEffect.SNOW, WeatherEffectResolver.resolve("Sleet"));
    }

    @Test public void unknownConditionsStayStatic() {
        assertEquals(WeatherEffect.UNKNOWN, WeatherEffectResolver.resolve("天气"));
        assertEquals(WeatherEffect.UNKNOWN, WeatherEffectResolver.resolve(" "));
        assertEquals(WeatherEffect.UNKNOWN, WeatherEffectResolver.resolve(null));
    }
}
