package com.cidercollective.cider.sidcloud;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Minimal Gemini REST client (generateContent). Pure Java so it is testable on the JVM. */
public final class GeminiClient {
    public static final String DEFAULT_MODEL = "gemini-flash-latest";
    private static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9._-]+");
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 60000;
    private static final int MAX_OUTPUT_TOKENS = 2048;

    /** Set once the API rejects thinkingConfig; later calls omit it. */
    private static volatile boolean thinkingUnsupported = false;

    private final String baseUrl;

    public GeminiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static final class Reply {
        public final String text;
        public final int finishReason;

        Reply(String text, int finishReason) {
            this.text = text;
            this.finishReason = finishReason;
        }
    }

    private static final class Http {
        final int status;
        final String body;

        Http(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    public Reply generate(String apiKey, String model, String prompt, float temperature, String schemaText)
            throws CloudSidException {
        String m = model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim();
        if (!MODEL_ID.matcher(m).matches()) {
            throw new CloudSidException(CloudSidException.FAILED, "Invalid Gemini model id");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new CloudSidException(CloudSidException.FAILED, "No Gemini API key");
        }
        String url = baseUrl + "/v1beta/models/" + m + ":generateContent";

        boolean withThinking = !thinkingUnsupported;
        Http res = post(url, apiKey, buildBody(prompt, temperature, schemaText, withThinking));
        if (withThinking && res.status == 400 && res.body.toLowerCase(Locale.ROOT).contains("thinking")) {
            thinkingUnsupported = true;
            res = post(url, apiKey, buildBody(prompt, temperature, schemaText, false));
        }
        if (res.status < 200 || res.status >= 300) {
            int code = res.status == 429 || res.status == 500 || res.status == 503
                    ? CloudSidException.BUSY : CloudSidException.FAILED;
            throw new CloudSidException(code, describe(res));
        }
        return parse(res.body, schemaText != null);
    }

    private static String buildBody(String prompt, float temperature, String schemaText, boolean thinking)
            throws CloudSidException {
        try {
            String text = prompt;
            if (schemaText != null) {
                text = prompt + "\n\nRespond with a single JSON object that matches this schema and nothing else:\n"
                        + schemaText;
            }
            JSONObject part = new JSONObject().put("text", text);
            JSONObject content = new JSONObject().put("role", "user").put("parts", new JSONArray().put(part));
            JSONObject config = new JSONObject()
                    .put("temperature", (double) temperature)
                    .put("maxOutputTokens", MAX_OUTPUT_TOKENS)
                    .put("candidateCount", 1);
            if (schemaText != null) config.put("responseMimeType", "application/json");
            if (thinking) config.put("thinkingConfig", new JSONObject().put("thinkingBudget", 0));
            return new JSONObject()
                    .put("contents", new JSONArray().put(content))
                    .put("generationConfig", config)
                    .toString();
        } catch (JSONException e) {
            throw new CloudSidException(CloudSidException.FAILED, "Could not build request: " + e.getMessage());
        }
    }

    private static Http post(String url, String apiKey, String body) throws CloudSidException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-goog-api-key", apiKey);
            byte[] bytes = body.getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(bytes.length);
            OutputStream os = conn.getOutputStream();
            try {
                os.write(bytes);
            } finally {
                os.close();
            }
            int status = conn.getResponseCode();
            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            return new Http(status, is == null ? "" : readAll(is));
        } catch (IOException e) {
            throw new CloudSidException(CloudSidException.FAILED, "Network error: " + e.getClass().getSimpleName());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream is) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            is.close();
        }
    }

    private static String describe(Http res) {
        String msg = "Gemini HTTP " + res.status;
        try {
            JSONObject err = new JSONObject(res.body).optJSONObject("error");
            if (err != null) {
                String m = err.optString("message", "");
                if (!m.isEmpty()) msg += ": " + m;
            }
        } catch (JSONException ignored) {
            // non-JSON error body; status is enough
        }
        return msg;
    }

    private static Reply parse(String body, boolean structured) throws CloudSidException {
        JSONObject root;
        try {
            root = new JSONObject(body);
        } catch (JSONException e) {
            throw new CloudSidException(CloudSidException.FAILED, "Gemini returned invalid JSON");
        }
        JSONObject feedback = root.optJSONObject("promptFeedback");
        if (feedback != null && feedback.has("blockReason")) {
            throw new CloudSidException(CloudSidException.FAILED,
                    "Gemini blocked the prompt: " + feedback.optString("blockReason"));
        }
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            throw new CloudSidException(CloudSidException.FAILED, "Gemini returned no candidates");
        }
        JSONObject cand = candidates.optJSONObject(0);
        StringBuilder text = new StringBuilder();
        JSONObject content = cand == null ? null : cand.optJSONObject("content");
        JSONArray parts = content == null ? null : content.optJSONArray("parts");
        if (parts != null) {
            for (int i = 0; i < parts.length(); i++) {
                JSONObject p = parts.optJSONObject(i);
                if (p != null && p.has("text")) text.append(p.optString("text", ""));
            }
        }
        String out = text.toString();
        if (structured) out = stripFence(out);
        if (out.trim().isEmpty()) {
            throw new CloudSidException(CloudSidException.FAILED, "Gemini returned empty text");
        }
        String finish = cand.optString("finishReason", "STOP");
        int reason = "STOP".equals(finish) ? 0 : "MAX_TOKENS".equals(finish) ? 1 : 2;
        return new Reply(out, reason);
    }

    private static String stripFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            t = nl < 0 ? "" : t.substring(nl + 1);
            t = t.trim();
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }
}
