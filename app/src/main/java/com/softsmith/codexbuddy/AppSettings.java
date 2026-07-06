package com.softsmith.codexbuddy;

import android.content.Context;
import android.content.SharedPreferences;

final class AppSettings {
    private static final String PREFS = "codex_buddy_settings";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";

    private AppSettings() {
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String apiKey(Context context) {
        return prefs(context).getString(KEY_API_KEY, "");
    }

    static String model(Context context) {
        return prefs(context).getString(KEY_MODEL, DEFAULT_MODEL);
    }

    static void save(Context context, String apiKey, String model) {
        prefs(context)
            .edit()
            .putString(KEY_API_KEY, apiKey == null ? "" : apiKey.trim())
            .putString(KEY_MODEL, model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim())
            .apply();
    }
}
