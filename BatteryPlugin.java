package com.luisdornellez.tunochedeesperanza;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Ajustes de batería: permite que la página pida "sin restricciones" y abra los ajustes de Android. */
@CapacitorPlugin(name = "Battery")
public class BatteryPlugin extends Plugin {

    public static boolean sinRestriccion(Context c) {
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
    }

    public static void pedirIgnorarBateria(Context c) {
        if (sinRestriccion(c)) return;
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + c.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            abrirAjustesBateria(c);
        }
    }

    public static void abrirAjustesBateria(Context c) {
        try {
            Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + c.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        }
    }

    /** true si abrió la pantalla de Inicio automático (Xiaomi/Poco/Redmi). */
    public static boolean abrirInicioAutomatico(Context c) {
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();
        if (!(m.contains("xiaomi") || m.contains("poco") || m.contains("redmi"))) return false;
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // JS: Capacitor.Plugins.Battery.status() -> { unrestricted: true/false }
    @PluginMethod
    public void status(PluginCall call) {
        JSObject r = new JSObject();
        r.put("unrestricted", sinRestriccion(getContext()));
        call.resolve(r);
    }

    // JS: Capacitor.Plugins.Battery.request()  -> diálogo del sistema
    @PluginMethod
    public void request(PluginCall call) {
        pedirIgnorarBateria(getContext());
        call.resolve();
    }

    // JS: Capacitor.Plugins.Battery.openSettings()
    @PluginMethod
    public void openSettings(PluginCall call) {
        abrirAjustesBateria(getContext());
        call.resolve();
    }

    // JS: Capacitor.Plugins.Battery.openAutostart() -> { opened: true/false }
    @PluginMethod
    public void openAutostart(PluginCall call) {
        JSObject r = new JSObject();
        r.put("opened", abrirInicioAutomatico(getContext()));
        call.resolve(r);
    }
}
