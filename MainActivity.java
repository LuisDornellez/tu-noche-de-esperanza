package com.luisdornellez.tunochedeesperanza;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final int REQ_NOTIF = 4401;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(RadioPlugin.class);
        registerPlugin(BatteryPlugin.class);
        super.onCreate(savedInstanceState);
        handleExit(getIntent());
        pedirPermisosIniciales();
    }

    // Primero notificaciones (Android 13+), después el aviso de ahorro de batería.
    private void pedirPermisosIniciales() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        } else {
            pedirBateriaUnaVez();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIF) pedirBateriaUnaVez();
    }

    // El diálogo sale automáticamente solo la primera vez; luego se puede pedir desde la página.
    private void pedirBateriaUnaVez() {
        SharedPreferences sp = getSharedPreferences("radio_prefs", MODE_PRIVATE);
        if (!sp.getBoolean("bateria_pedida", false) && !BatteryPlugin.sinRestriccion(this)) {
            sp.edit().putBoolean("bateria_pedida", true).apply();
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> BatteryPlugin.pedirIgnorarBateria(MainActivity.this), 600);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleExit(intent);
    }

    // El WebView sigue activo aunque la pantalla se apague o la app pase a segundo plano.
    @Override public void onPause() { super.onPause(); keepWebViewRunning(); }
    @Override public void onStop() { super.onStop(); keepWebViewRunning(); }

    private void keepWebViewRunning() {
        if (getBridge() != null && getBridge().getWebView() != null) {
            WebView w = getBridge().getWebView();
            w.onResume();
            w.resumeTimers();
        }
    }

    // Botón "Detener radio" de la notificación
    private void handleExit(Intent intent) {
        if (intent != null && "EXIT_RADIO".equals(intent.getAction())) {
            stopService(new Intent(this, KeepAliveService.class));
            finishAndRemoveTask();
            new Handler(Looper.getMainLooper()).postDelayed(() -> Process.killProcess(Process.myPid()), 400);
        }
    }
}
