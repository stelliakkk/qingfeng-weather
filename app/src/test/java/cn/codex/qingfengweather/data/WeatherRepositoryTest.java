package cn.codex.qingfengweather.data;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WeatherRepositoryTest {
    private final City city = new City("北京", "北京市", "中国", 39.9, 116.4, "Asia/Shanghai");
    private final WeatherSnapshot sample = new WeatherSnapshot(
            20, 19, 50, 8, 90, 0, "晴", 0, "2026-11-08T10:00", "test", "test attribution", Collections.emptyList());

    @Test
    public void primarySuccessDoesNotCallBackup() {
        FakeProvider primary = new FakeProvider("和风天气", sample, false);
        FakeProvider backup = new FakeProvider("Open-Meteo", sample, false);

        RefreshResult result = new WeatherRepository(primary, backup).refresh(city);

        assertNotNull(result.snapshot);
        assertEquals("和风天气", result.snapshot.source);
        assertFalse(result.usedBackup);
        assertEquals(1, primary.calls);
        assertEquals(0, backup.calls);
    }

    @Test
    public void primaryFailureFallsBackToOpenMeteo() {
        FakeProvider primary = new FakeProvider("和风天气", null, true);
        FakeProvider backup = new FakeProvider("Open-Meteo", sample, false);

        RefreshResult result = new WeatherRepository(primary, backup).refresh(city);

        assertNotNull(result.snapshot);
        assertEquals("Open-Meteo", result.snapshot.source);
        assertTrue(result.usedBackup);
        assertTrue(result.primaryUnavailable);
        assertEquals(1, primary.calls);
        assertEquals(1, backup.calls);
    }

    @Test
    public void bothProvidersFailReturnsFailureWithoutSnapshot() {
        RefreshResult result = new WeatherRepository(
                new FakeProvider("和风天气", null, true),
                new FakeProvider("Open-Meteo", null, true)).refresh(city);

        assertNull(result.snapshot);
        assertFalse(result.errorMessage.isEmpty());
    }

    @Test
    public void noPrimaryConfiguredUsesBackupAsExpected() {
        FakeProvider backup = new FakeProvider("Open-Meteo", sample, false);

        RefreshResult result = new WeatherRepository(null, backup).refresh(city);

        assertNotNull(result.snapshot);
        assertEquals("Open-Meteo", result.snapshot.source);
        assertTrue(result.usedBackup);
        assertEquals(1, backup.calls);
    }

    private static final class FakeProvider implements WeatherProvider {
        private final String label;
        private final WeatherSnapshot response;
        private final boolean fail;
        private int calls;

        FakeProvider(String label, WeatherSnapshot response, boolean fail) {
            this.label = label;
            this.response = response;
            this.fail = fail;
        }

        @Override public String name() { return label; }

        @Override
        public WeatherSnapshot fetch(City requestedCity) throws Exception {
            calls++;
            if (fail) throw new IllegalStateException("simulated network error");
            return response;
        }
    }
}
