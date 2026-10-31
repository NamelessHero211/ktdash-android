package app.ktdash.personal;

import android.webkit.WebView;
import org.json.JSONObject;

final class AppTheme {
    // Deeper logo-inspired orange; black text has approximately 5:1 contrast.
    static final int ORANGE = 0xFFCF5718;
    private static final String CSS =
        ".bg-card{background-color:#cf5718!important;border-color:#7b2906!important;color:#000!important;}" +
        ".bg-card :is(h1,h2,h3,h4,h5,h6,p,a,span,label,li,small,strong,em,td,th,div)," +
        ".bg-card [class*='text-']{color:#000!important;}" +
        ".bg-card,.bg-card :is(h1,h2,h3,h4,h5,h6,p,a,span,label,li,small,strong,em,td,th,div)," +
        "button,[role='button'],input,select,textarea{font-weight:700!important;font-synthesis:weight;}" +
        "a,button,[role='button']{touch-action:manipulation;}" +
        ".bg-card :is(.bg-background,.bg-black){background-color:#e58b55!important;}" +
        "button,[role='button'],.bg-main,a.bg-main,input[type='submit']{" +
        "background-color:#cf5718!important;color:#000!important;border:2px solid #000!important;}" +
        "button :is(span,svg),[role='button'] :is(span,svg),a.bg-main *{color:#000!important;}" +
        "input:not([type='checkbox']):not([type='radio']):not([type='range']),select,textarea{" +
        "background-color:#e58b55!important;color:#000!important;border-color:#7b2906!important;}" +
        "input::placeholder,textarea::placeholder{color:#42200b!important;opacity:1;}" +
        ".text-main{color:#cf5718;}" +
        ".bg-card :focus-visible,button:focus-visible,input:focus-visible,select:focus-visible," +
        "textarea:focus-visible{outline:2px solid #000!important;outline-offset:2px;}";
    private static final String SCRIPT = "(function(){if(document.getElementById('ktdash-android-theme'))return;" +
        "var s=document.createElement('style');s.id='ktdash-android-theme';s.textContent=" +
        JSONObject.quote(CSS) + ";(document.head||document.documentElement).appendChild(s);})();";

    static void apply(WebView web) { web.evaluateJavascript(SCRIPT, null); }
}

