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
    private static volatile String navigation;
    private static volatile String portraits;
    private static final String REFRESH = "(function(){var n=window.__ktdashRosterNavigation,p=window.__ktdashRosterPortraits;" +
        "if(n)n.check();if(p)p.refresh();return(n?1:0)+(p?2:0);})();";

    static void apply(WebView web) {
        if (navigation != null && portraits != null) { run(web); return; }
        Context context = web.getContext().getApplicationContext();
        LOADER.execute(() -> {
            // One failed asset must not prevent the other independent feature from starting.
            if (navigation == null) navigation = read(context, "roster-navigation.js");
            if (portraits == null) portraits = read(context, "roster-portraits.js");
            web.post(() -> run(web));
        });
    }

    private static String read(Context context, String asset) {
        StringBuilder source = new StringBuilder();
        try (InputStream input = context.getAssets().open(asset);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            char[] buffer = new char[4096]; int count;
            while ((count = reader.read(buffer)) != -1) source.append(buffer, 0, count);
            return source.toString();
        } catch (java.io.IOException error) {
            android.util.Log.w("KTDash", "Could not read bundled app feature: " + asset, error);
            return null;
        }
    }

    private static boolean ready(WebView web) {
        if (!web.isAttachedToWindow() || web.getUrl() == null) return false;
        Uri url = Uri.parse(web.getUrl());
        return "https".equalsIgnoreCase(url.getScheme()) &&
            ("ktdash.app".equalsIgnoreCase(url.getHost()) || "www.ktdash.app".equalsIgnoreCase(url.getHost()));
    }

    private static void run(WebView web) {
        if (!ready(web)) return;
        // Refresh existing hooks using a small message; do not repeatedly parse a catalog/code bundle.
        web.evaluateJavascript(REFRESH, result -> {
            if (!ready(web)) return;
            int loaded = 0;
            try { loaded = Integer.parseInt(result); } catch (Exception ignored) {}
            if ((loaded & 1) == 0 && navigation != null) web.evaluateJavascript(navigation, null);
            if ((loaded & 2) == 0 && portraits != null) web.evaluateJavascript(portraits, null);
        });
    }
}
