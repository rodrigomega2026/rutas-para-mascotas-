package cl.rutasmascotas.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

/**
 * Abre la página "Rutas para Mascotas" (index.html + config.js) dentro de la app.
 * La página se sirve desde https://appassets.androidplatform.net/assets/ para que funcionen
 * el GPS, el guardado de datos (localStorage) y las consultas a OpenStreetMap.
 */
public class MainActivity extends Activity {

    private static final String DOMINIO = "appassets.androidplatform.net";
    private static final String INICIO = "https://" + DOMINIO + "/assets/index.html";
    private static final int PEDIR_UBICACION = 10;
    private static final int ELEGIR_ARCHIVO = 20;

    private WebView web;
    private LinearLayout sinConexion;
    private WebViewAssetLoader cargador;

    // Pedidos pendientes de la página (ubicación y fotos)
    private String origenUbicacion;
    private GeolocationPermissions.Callback avisoUbicacion;
    private ValueCallback<Uri[]> avisoArchivo;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);

        cargador = new WebViewAssetLoader.Builder()
                .setDomain(DOMINIO)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        FrameLayout raiz = new FrameLayout(this);
        web = new WebView(this);
        raiz.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sinConexion = crearPantallaSinConexion();
        sinConexion.setVisibility(View.GONE);
        raiz.addView(sinConexion, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);            // alertas y lugares guardados en el teléfono
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);           // botón "Mi ubicación"
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setSupportMultipleWindows(false);      // los enlaces "_blank" pasan por shouldOverrideUrlLoading
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " RutasMascotasApp/1.0");

        web.setWebViewClient(new Cliente());
        web.setWebChromeClient(new Navegador());

        if (estado != null) web.restoreState(estado);
        else web.loadUrl(INICIO);
    }

    @Override
    protected void onSaveInstanceState(Bundle salida) {
        super.onSaveInstanceState(salida);
        web.saveState(salida);
    }

    // ---------- Carga de la página y enlaces externos ----------

    private class Cliente extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest pedido) {
            return cargador.shouldInterceptRequest(pedido.getUrl());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest pedido) {
            Uri u = pedido.getUrl();
            if (DOMINIO.equals(u.getHost())) return false;   // páginas propias: se quedan en la app
            abrirFuera(u);                                     // Google Maps, WhatsApp, teléfono, OSM…
            return true;
        }

        @Override
        public void onReceivedError(WebView v, WebResourceRequest pedido, WebResourceError error) {
            if (pedido.isForMainFrame()) sinConexion.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView v, String url) {
            if (url == null || !url.startsWith(INICIO)) return;
            // El mapa (Leaflet) viene de internet: si no cargó, no hay conexión
            v.evaluateJavascript("typeof L", tipo -> {
                boolean sinMapa = tipo == null || tipo.contains("undefined");
                sinConexion.setVisibility(sinMapa ? View.VISIBLE : View.GONE);
            });
        }
    }

    /** Abre un enlace en la app que corresponda (o en el navegador). */
    private void abrirFuera(Uri u) {
        String esquema = u.getScheme() == null ? "" : u.getScheme();
        Intent i;
        if (esquema.equals("tel")) {
            i = new Intent(Intent.ACTION_DIAL, u);
        } else if (esquema.equals("intent")) {
            try {
                i = Intent.parseUri(u.toString(), Intent.URI_INTENT_SCHEME);
                i.addCategory(Intent.CATEGORY_BROWSABLE);
                i.setComponent(null);
                i.setSelector(null);
            } catch (Exception e) {
                return;
            }
        } else {
            i = new Intent(Intent.ACTION_VIEW, u);
        }
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay una app para abrir este enlace.", Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Ubicación y fotos que pide la página ----------

    private class Navegador extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origen, GeolocationPermissions.Callback aviso) {
            if (tienePermisoUbicacion()) {
                aviso.invoke(origen, true, false);
                return;
            }
            origenUbicacion = origen;
            avisoUbicacion = aviso;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PEDIR_UBICACION);
        }

        @Override
        public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> aviso, FileChooserParams parametros) {
            if (avisoArchivo != null) avisoArchivo.onReceiveValue(null);
            avisoArchivo = aviso;
            try {
                Intent elegir = parametros.createIntent();
                elegir.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(elegir, "Elegir foto"), ELEGIR_ARCHIVO);
                return true;
            } catch (ActivityNotFoundException e) {
                avisoArchivo = null;
                Toast.makeText(MainActivity.this, "No se pudo abrir la galería.", Toast.LENGTH_LONG).show();
                return false;
            }
        }
    }

    private boolean tienePermisoUbicacion() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int codigo, String[] permisos, int[] resultados) {
        super.onRequestPermissionsResult(codigo, permisos, resultados);
        if (codigo == PEDIR_UBICACION && avisoUbicacion != null) {
            boolean ok = tienePermisoUbicacion();
            avisoUbicacion.invoke(origenUbicacion, ok, false);
            if (!ok) Toast.makeText(this, "Sin permiso de ubicación. Puedes activarlo en Ajustes > Apps > Rutas Mascotas.", Toast.LENGTH_LONG).show();
            avisoUbicacion = null;
            origenUbicacion = null;
        }
    }

    @Override
    protected void onActivityResult(int codigo, int resultado, Intent datos) {
        super.onActivityResult(codigo, resultado, datos);
        if (codigo == ELEGIR_ARCHIVO && avisoArchivo != null) {
            avisoArchivo.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultado, datos));
            avisoArchivo = null;
        }
    }

    // ---------- Botón "atrás" ----------

    @Override
    public void onBackPressed() {
        // Si hay un formulario abierto en la página, "atrás" lo cierra; si no, sale de la app.
        web.evaluateJavascript(
                "(function(){var b=document.getElementById('sheetBg');"
                        + "if(b&&!b.hidden){b.hidden=true;var s=document.getElementById('sheet');if(s)s.innerHTML='';return 'cerrado';}"
                        + "var p=document.getElementById('pickbar');if(p&&!p.hidden){var c=document.getElementById('pickCancel');if(c)c.click();return 'cerrado';}"
                        + "return '';})()",
                valor -> {
                    if (valor != null && valor.contains("cerrado")) return;
                    if (web.canGoBack()) web.goBack();
                    else confirmarSalida();
                });
    }

    private void confirmarSalida() {
        new AlertDialog.Builder(this)
                .setMessage("¿Salir de Rutas para Mascotas?")
                .setPositiveButton("Salir", (d, w) -> finish())
                .setNegativeButton("Quedarme", null)
                .show();
    }

    // ---------- Sin conexión ----------

    private LinearLayout crearPantallaSinConexion() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(0xFFEDF1EA);
        int p = dp(28);
        l.setPadding(p, p, p, p);

        TextView t = new TextView(this);
        t.setText("Sin conexión");
        t.setTextSize(22);
        t.setTextColor(0xFF18241E);
        t.setGravity(Gravity.CENTER);
        l.addView(t);

        TextView d = new TextView(this);
        d.setText("La app necesita internet para mostrar el mapa y los lugares. Revisa tus datos móviles o WiFi.");
        d.setTextSize(15);
        d.setTextColor(0xFF56665C);
        d.setGravity(Gravity.CENTER);
        d.setPadding(0, dp(10), 0, dp(20));
        l.addView(d);

        Button b = new Button(this);
        b.setText("Reintentar");
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(0xFF1E5C43);
        b.setOnClickListener(v -> {
            sinConexion.setVisibility(View.GONE);
            web.loadUrl(INICIO);
        });
        l.addView(b, new LinearLayout.LayoutParams(dp(180), dp(50)));
        return l;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
