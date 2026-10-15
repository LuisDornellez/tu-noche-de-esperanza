package com.luisdornellez.tunochedeesperanza;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(RadioPlugin.class);
        super.onCreate(savedInstanceState);
        handleExit(getIntent());
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
