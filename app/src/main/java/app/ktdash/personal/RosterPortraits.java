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
    private static volatile String viewer;
    private static volatile String catalogScript;
    private static volatile BundledModels modelAssets;
    private static final String ATLAS_PROBE = "(function(){var c=window.__ktdashModelCatalog||{},teams=[],ids=[];" +
        "document.querySelectorAll('a[href^=\"/killteams/\"]').forEach(function(a){var id=a.getAttribute('href').split('/')[2];if(c[id]&&teams.indexOf(id)<0)teams.push(id)});" +
        "document.querySelectorAll('img[src*=\"/killteams/\"]').forEach(function(img){var m=img.getAttribute('src').match(/\\/killteams\\/([A-Z0-9-]+)/);if(m&&c[m[1]]&&teams.indexOf(m[1])<0)teams.push(m[1])});" +
        "var tags=document.querySelector('.opcard .uppercase em');var words=tags?tags.textContent.toLowerCase().replace(/[^a-z0-9,]/g,'').split(','):[];" +
        "Object.keys(c).forEach(function(id){if(words.indexOf(c[id].name.toLowerCase().replace(/[^a-z0-9]/g,''))>=0&&teams.indexOf(id)<0)teams.push(id)});" +
        "teams.slice(0,3).forEach(function(t){Object.keys(c[t].ops||{}).forEach(function(k){var id=c[t].ops[k].atlas||t;if(ids.indexOf(id)<0&&!(window.__ktdashAtlasData||{})[id])ids.push(id)})});return JSON.stringify(ids)})();";
    private static final String REFRESH = "(function(){var n=window.__ktdashRosterNavigation,p=window.__ktdashRosterPortraits;" +
        "if(n)n.check();if(p)p.refresh();return(n?1:0)+(p?2:0);})();";

    static void apply(WebView web) {
        if (navigation != null && portraits != null && viewer != null && catalogScript != null) { run(web); return; }
        Context context = web.getContext().getApplicationContext();
        LOADER.execute(() -> {
            // One failed asset must not prevent the other independent feature from starting.
            if (modelAssets == null) modelAssets = new BundledModels(context);
            if (catalogScript == null) {
                String catalog = modelAssets.catalog();
                if (!catalog.isEmpty()) catalogScript = "window.__ktdashModelCatalog=" + catalog + ";if(window.__ktdashRosterPortraits)window.__ktdashRosterPortraits.refresh();";
            }
            if (viewer == null) viewer = read(context, "portrait-viewer.js");
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
            if (catalogScript != null) web.evaluateJavascript("(function(){return !!window.__ktdashModelCatalog})()", hasCatalog -> {
                if (!ready(web)) return;
                if (!"true".equals(hasCatalog)) web.evaluateJavascript(catalogScript, ignored -> start(web, result));
                else start(web, result);
            });
            else start(web, result);
        });
    }

    private static void start(WebView web, String result) {
            if (!ready(web)) return;
            int loaded = 0;
            try { loaded = Integer.parseInt(result); } catch (Exception ignored) {}
            if ((loaded & 1) == 0 && navigation != null) web.evaluateJavascript(navigation, null);
            if (viewer != null) web.evaluateJavascript(viewer, null);
            if ((loaded & 2) == 0 && portraits != null) web.evaluateJavascript(portraits, ignored -> loadVisibleAtlases(web));
            else loadVisibleAtlases(web);
            // Hydrated roster cards may arrive after route notification. These are bounded checks,
            // not a permanent timer. Cached atlases are never read or sent again in that document.
            for (long delay : new long[] {200, 800, 2000}) web.postDelayed(() -> loadVisibleAtlases(web), delay);
    }

    private static void loadVisibleAtlases(WebView web) {
        if (!ready(web) || modelAssets == null) return;
        String page = web.getUrl();
        web.evaluateJavascript(ATLAS_PROBE, result -> {
            try {
                Object decoded = new org.json.JSONTokener(result).nextValue();
                org.json.JSONArray ids = new org.json.JSONArray(String.valueOf(decoded));
                if (ids.length() == 0) return;
                LOADER.execute(() -> {
                    for (int i = 0; i < ids.length(); i++) {
                        String id = ids.optString(i); String image = modelAssets.atlas(id);
                        if (image.isEmpty()) continue;
                        org.json.JSONObject data = new org.json.JSONObject();
                        try { data.put(id, image); } catch (org.json.JSONException ignored) { continue; }
                        // Send one atlas per message, including teams which borrow several sheets.
                        String script = "window.__ktdashAtlasData=Object.assign(window.__ktdashAtlasData||{}," + data +
                            ");if(window.__ktdashRosterPortraits)window.__ktdashRosterPortraits.refresh();";
                        web.post(() -> { if (ready(web) && page.equals(web.getUrl())) web.evaluateJavascript(script, null); });
                    }
                });
            } catch (org.json.JSONException ignored) {}
        });
    }
}
