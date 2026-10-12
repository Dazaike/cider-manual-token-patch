package com.cidercollective.cider.sidcloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/** Android-facing entry point used by the patched Cider's Sid hooks. */
public final class CloudSid {
    private static final String PREFS = "cider_sid_cloud";
    private static final String KEY_API = "api_key";
    private static final String KEY_MODEL = "model";

    private CloudSid() {}

    public static final class Result {
        public final String text;
        public final int finishReason;

        Result(String text, int finishReason) {
            this.text = text;
            this.finishReason = finishReason;
        }
    }

    /** True when an API key is configured. Never throws. */
    public static boolean enabled(Context c) {
        try {
            String key = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_API, "");
            return key != null && !key.isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Blank or null {@code apiKey} removes the config; otherwise stores the trimmed values. */
    public static void saveConfig(Context c, String apiKey, String model) {
        SharedPreferences.Editor ed = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            ed.remove(KEY_API).remove(KEY_MODEL);
        } else {
            ed.putString(KEY_API, apiKey.trim());
            ed.putString(KEY_MODEL, model == null ? "" : model.trim());
        }
        ed.commit();
    }

    public static Result generate(Context c, String prompt, float temperature, String schemaText)
            throws CloudSidException {
        SharedPreferences prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String model = prefs.getString(KEY_MODEL, "");
        long start = System.currentTimeMillis();
        try {
            GeminiClient.Reply r = new GeminiClient("https://generativelanguage.googleapis.com").generate(
                    prefs.getString(KEY_API, ""), model, prompt, temperature, schemaText);
            Log.i("CloudSid", "Gemini ok: model=" + (model.isEmpty() ? GeminiClient.DEFAULT_MODEL : model)
                    + " finish=" + r.finishReason + " chars=" + r.text.length()
                    + " ms=" + (System.currentTimeMillis() - start));
            return new Result(r.text, r.finishReason);
        } catch (CloudSidException e) {
            Log.w("CloudSid", e.getMessage());
            throw e;
        }
    }
}
