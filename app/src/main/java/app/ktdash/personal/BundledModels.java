package app.ktdash.personal;

import android.content.Context;
import android.util.Base64;
import android.util.LruCache;
import android.webkit.JavascriptInterface;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Read-only access to public miniature assets; no filesystem paths, accounts or device APIs. */
public final class BundledModels {
    private final Context context;
    private String catalog;
    private final LruCache<String, String> images = new LruCache<String, String>(2 * 1024 * 1024) {
        @Override protected int sizeOf(String key, String value) { return value.length() * 2; }
    };

    BundledModels(Context context) { this.context = context.getApplicationContext(); }

    private byte[] read(String asset) throws java.io.IOException {
        try (InputStream input = context.getAssets().open(asset);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return bytes.toByteArray();
        }
    }

    @JavascriptInterface public synchronized String catalog() {
        if (catalog == null) {
            try { catalog = new String(read("models/catalog.json"), StandardCharsets.UTF_8); }
            catch (java.io.IOException error) { return ""; }
        }
        return catalog;
    }

    @JavascriptInterface public synchronized String atlas(String id) {
        if (id == null || !id.matches("[A-Z0-9-]{1,64}")) return "";
        String image = images.get(id);
        if (image != null) return image;
        try {
            image = "data:image/webp;base64," + Base64.encodeToString(read("models/" + id + ".webp"), Base64.NO_WRAP);
            images.put(id, image); return image;
        } catch (java.io.IOException error) { return ""; }
    }
}
