package cn.codex.qingfengweather.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class CredentialStore {
    private static final String ALIAS = "qingfeng_weather_qweather_key";
    private static final String SETTINGS = "weather_settings";
    private static final String PRIVATE_SETTINGS = "weather_private_settings";

    private CredentialStore() {}

    public static QWeatherCredentials load(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences privateSettings = context.getSharedPreferences(PRIVATE_SETTINGS, Context.MODE_PRIVATE);
        String host = settings.getString("qweather_host", "");
        String encrypted = privateSettings.getString("qweather_key", "");
        String key = "";
        if (encrypted != null && !encrypted.isEmpty()) {
            try {
                key = decrypt(encrypted);
            } catch (Exception ignored) {
                // A lost Android Keystore key falls back to Open-Meteo and can be repaired in settings.
            }
        }
        return new QWeatherCredentials(host, key);
    }

    public static void save(Context context, String host, String apiKey) throws Exception {
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .putString("qweather_host", host == null ? "" : host.trim())
                .apply();
        SharedPreferences.Editor privateEditor = context
                .getSharedPreferences(PRIVATE_SETTINGS, Context.MODE_PRIVATE)
                .edit();
        String value = apiKey == null ? "" : apiKey.trim();
        if (value.isEmpty()) {
            privateEditor.remove("qweather_key").apply();
        } else {
            privateEditor.putString("qweather_key", encrypt(value)).apply();
        }
    }

    private static String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":"
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private static String decrypt(String stored) throws Exception {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid encrypted credential");
        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] ciphertext = Base64.decode(parts[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
