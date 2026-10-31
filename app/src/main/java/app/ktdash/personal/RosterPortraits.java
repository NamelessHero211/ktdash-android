package app.ktdash.personal;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class RosterPortraits {
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor();
    private static volatile String script;

    static void apply(WebView web) {
        if (script != null) { run(web); return; }
        Context context = web.getContext().getApplicationContext();
        LOADER.execute(() -> {
            try {
                if (script == null) {
                    StringBuilder source = new StringBuilder();
                    for (String asset : new String[] { "models/catalog.json", "roster-navigation.js", "roster-portraits.js" }) {
                    if (asset.endsWith(".json")) source.append("window.__ktdashModelCatalog=");
                    try (InputStream input = context.getAssets().open(asset);
                         InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                        char[] buffer = new char[4096]; int count;
                        while ((count = reader.read(buffer)) != -1) source.append(buffer, 0, count);
                    }
                    source.append(';').append('\n');
                    }
                    script = source.toString();
                }
                web.post(() -> run(web));
            } catch (java.io.IOException ignored) { /* The website remains usable. */ }
        });
    }

    private static void run(WebView web) {
        if (!web.isAttachedToWindow() || web.getUrl() == null || script == null) return;
        Uri url = Uri.parse(web.getUrl());
        if ("https".equalsIgnoreCase(url.getScheme()) &&
            ("ktdash.app".equalsIgnoreCase(url.getHost()) || "www.ktdash.app".equalsIgnoreCase(url.getHost()))) {
            web.evaluateJavascript(script, null);
        }
    }
}
