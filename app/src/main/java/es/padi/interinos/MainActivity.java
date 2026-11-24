package es.padi.interinos;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(Color.parseColor("#C8102E"));

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        web.addJavascriptInterface(new Puente(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("https".equals(u.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                    return true;
                }
                return false;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);

        ConsultaJobService.crearCanal(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        Scheduler.programarDiaria(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript("window.alVolver && window.alVolver()", null);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    /** Métodos que la pantalla (index.html) puede llamar. */
    private class Puente {
        @JavascriptInterface
        public String fechas() {
            return Store.fechasGuardadas(MainActivity.this).toString();
        }

        @JavascriptInterface
        public String consulta(String fecha) {
            JSONObject o = Store.leer(MainActivity.this, fecha);
            return o == null ? "null" : o.toString();
        }

        @JavascriptInterface
        public void borrar(String fecha) {
            Store.borrar(MainActivity.this, fecha);
        }

        @JavascriptInterface
        public String estado() {
            JSONObject o = Store.ajustes(MainActivity.this);
            try {
                o.put("proxima", Store.prefs(MainActivity.this).getLong("proxima", 0));
                o.put("exacta", Store.prefs(MainActivity.this).getBoolean("exacta", true));
                o.put("ultimoIntento", Store.prefs(MainActivity.this).getString("ultimoIntento", ""));
                o.put("ultimoEstado", Store.prefs(MainActivity.this).getString("ultimoEstado", ""));
            } catch (Exception ignored) {}
            return o.toString();
        }

        @JavascriptInterface
        public void guardarAjustes(String json) {
            try {
                Store.guardarAjustes(MainActivity.this, new JSONObject(json));
                Scheduler.programarDiaria(MainActivity.this);
            } catch (Exception ignored) {}
        }

        /** Consulta ahora, en segundo plano; al terminar llama a window.alActualizar(resultado). */
        @JavascriptInterface
        public void actualizar() {
            new Thread(() -> {
                JSONObject r = Consulta.ejecutar(getApplicationContext());
                String js = "window.alActualizar && window.alActualizar(" + JSONObject.quote(r.toString()) + ")";
                runOnUiThread(() -> web.evaluateJavascript(js, null));
            }).start();
        }

        @JavascriptInterface
        public void compartir(String texto, String asunto) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, asunto);
            i.putExtra(Intent.EXTRA_TEXT, texto);
            runOnUiThread(() -> startActivity(Intent.createChooser(i, "Compartir")));
        }

        /** Android 12: abre el ajuste para permitir alarmas exactas. */
        @JavascriptInterface
        public void permitirAlarmasExactas() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager am = getSystemService(AlarmManager.class);
                if (!am.canScheduleExactAlarms()) {
                    Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
                    runOnUiThread(() -> startActivity(i));
                }
            }
        }

        @JavascriptInterface
        public String catalogo() {
            try {
                return Store.ajustes(MainActivity.this).getJSONArray("catalogo").toString();
            } catch (Exception e) {
                return new JSONArray().toString();
            }
        }
    }
}
