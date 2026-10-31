package app.ktdash.personal;

import android.webkit.WebView;
import org.json.JSONObject;

final class AppTheme {
    // Sampled from the orange in KTDash's own launcher logo.
    static final int ORANGE = 0xFFE84F0B;
    private static final String CSS =
        ".bg-card{background-color:#e84f0b!important;border-color:#7b2906!important;color:#000!important;}" +
        ".bg-card :is(h1,h2,h3,h4,h5,h6,p,a,span,label,li,small,strong,em,td,th,div)," +
        ".bg-card [class*='text-']{color:#000!important;}" +
        ".bg-card :is(.bg-background,.bg-black){background-color:#ffab7c!important;}" +
        "button,[role='button'],.bg-main,a.bg-main,input[type='submit']{" +
        "background-color:#e84f0b!important;color:#000!important;border-color:#7b2906!important;}" +
        "button :is(span,svg),[role='button'] :is(span,svg),a.bg-main *{color:#000!important;}" +
        "input:not([type='checkbox']):not([type='radio']):not([type='range']),select,textarea{" +
        "background-color:#ffab7c!important;color:#000!important;border-color:#7b2906!important;}" +
        "input::placeholder,textarea::placeholder{color:#42200b!important;opacity:1;}" +
        ".text-main{color:#e84f0b;}" +
        ".bg-card :focus-visible,button:focus-visible,input:focus-visible,select:focus-visible," +
        "textarea:focus-visible{outline:2px solid #000!important;outline-offset:2px;}";
    private static final String SCRIPT = "(function(){if(document.getElementById('ktdash-android-theme'))return;" +
        "var s=document.createElement('style');s.id='ktdash-android-theme';s.textContent=" +
        JSONObject.quote(CSS) + ";(document.head||document.documentElement).appendChild(s);})();";

    static void apply(WebView web) { web.evaluateJavascript(SCRIPT, null); }
}
