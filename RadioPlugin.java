package com.luisdornellez.tunochedeesperanza;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.common.util.concurrent.ListenableFuture;

/** Puente entre la web (JS) y el reproductor nativo. */
@CapacitorPlugin(name = "Radio")
public class RadioPlugin extends Plugin {
    private MediaController controller;
    private final Handler main = new Handler(Looper.getMainLooper());

    private final Player.Listener listener = new Player.Listener() {
        @Override public void onIsPlayingChanged(boolean isPlaying) {
            if (isPlaying) emit("playing");
            else if (controller != null && controller.getPlaybackState() == Player.STATE_BUFFERING) emit("buffering");
            else emit("paused");
        }
        @Override public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_BUFFERING && controller != null && controller.getPlayWhenReady()) emit("buffering");
        }
        @Override public void onPlayerError(PlaybackException error) { emit("error"); }
    };

    private void emit(String state) {
        JSObject o = new JSObject();
        o.put("state", state);
        notifyListeners("state", o);
    }

    private void withController(PluginCall call, Runnable action) {
        main.post(() -> {
            if (controller != null) { action.run(); return; }
            Context c = getContext();
            SessionToken token = new SessionToken(c, new ComponentName(c, RadioService.class));
            ListenableFuture<MediaController> f = new MediaController.Builder(c, token).buildAsync();
            f.addListener(() -> {
                try {
                    controller = f.get();
                    controller.addListener(listener);
                    action.run();
                } catch (Exception e) {
                    call.reject("No se pudo iniciar el reproductor: " + e.getMessage());
                }
            }, ContextCompat.getMainExecutor(c));
        });
    }

    @PluginMethod
    public void play(PluginCall call) {
        String url = call.getString("url");
        if (url == null || url.isEmpty()) { call.reject("Falta la URL del stream"); return; }
        String title = call.getString("title", "Radio");
        String artist = call.getString("artist", "");
        if (Build.VERSION.SDK_INT >= 33 && getActivity() != null
                && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
        }
        withController(call, () -> {
            MediaItem item = new MediaItem.Builder()
                    .setUri(url)
                    .setMediaMetadata(new MediaMetadata.Builder().setTitle(title).setArtist(artist).build())
                    .build();
            controller.setMediaItem(item);
            controller.prepare();
            controller.play();
            call.resolve();
        });
    }

    @PluginMethod
    public void stop(PluginCall call) {
        withController(call, () -> {
            controller.stop();
            controller.clearMediaItems();
            call.resolve();
        });
    }

    @PluginMethod
    public void setVolume(PluginCall call) {
        float v = call.getFloat("volume", 1f);
        withController(call, () -> { controller.setVolume(v); call.resolve(); });
    }

    /** Modo widget de Caster.fm: mantiene viva la app con la pantalla apagada. */
    @PluginMethod
    public void keepAlive(PluginCall call) {
        Context c = getContext();
        if (Build.VERSION.SDK_INT >= 33 && getActivity() != null
                && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
        }
        Intent i = new Intent(c, KeepAliveService.class);
        i.putExtra("title", call.getString("title", "Radio"));
        i.putExtra("text", call.getString("text", "Radio activa"));
        try { ContextCompat.startForegroundService(c, i); call.resolve(); }
        catch (Exception e) { call.reject("No se pudo iniciar el servicio: " + e.getMessage()); }
    }
}
