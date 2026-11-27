package nl.ruben.start;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@CapacitorPlugin(
    name = "StartNative",
    permissions = {
        @Permission(strings = {Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, alias = "calendar")
    }
)
public class StartNativePlugin extends Plugin {

    public static String pendingTarget = null;
    static final String PREFS = "start_prefs";

    // ---------- Navigatie vanuit snelkoppelingen en widget ----------
    @PluginMethod
    public void getTarget(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("target", pendingTarget == null ? "" : pendingTarget);
        pendingTarget = null;
        call.resolve(ret);
    }

    // ---------- Widget ----------
    @PluginMethod
    public void setWidget(PluginCall call) {
        JSArray items = call.getArray("items");
        JSONArray arr = new JSONArray();
        try {
            if (items != null) {
                for (int i = 0; i < items.length(); i++) arr.put(items.getJSONObject(i));
            }
        } catch (Exception e) {
            // negeren
        }
        SharedPreferences sp = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString("widget_items", arr.toString()).apply();
        StartWidget.updateAll(getContext());
        call.resolve();
    }

    @PluginMethod
    public void setWidgetStyle(PluginCall call) {
        SharedPreferences sp = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        String bg = call.getString("bg");
        String tx = call.getString("text");
        String ac = call.getString("acc");
        if (bg != null) ed.putString("w_bg", bg);
        if (tx != null) ed.putString("w_text", tx);
        if (ac != null) ed.putString("w_acc", ac);
        Integer al = call.getInt("alpha");
        if (al != null) ed.putInt("w_alpha", al);
        ed.apply();
        StartWidget.updateAll(getContext());
        call.resolve();
    }

    @PluginMethod
    public void takeDone(PluginCall call) {
        SharedPreferences sp = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSObject ret = new JSObject();
        try {
            ret.put("items", new JSArray(sp.getString("widget_done", "[]")));
        } catch (Exception e) {
            ret.put("items", new JSArray());
        }
        sp.edit().putString("widget_done", "[]").apply();
        call.resolve(ret);
    }

    @PluginMethod
    public void getEvents(PluginCall call) {
        JSObject ret = new JSObject();
        JSArray arr = new JSArray();
        try {
            if (getPermissionState("calendar") == PermissionState.GRANTED) {
                long from = call.getData().optLong("from", 0);
                long to = call.getData().optLong("to", 0);
                Uri.Builder ub = CalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(ub, from);
                ContentUris.appendId(ub, to);
                Cursor c = getContext().getContentResolver().query(ub.build(),
                    new String[]{
                        CalendarContract.Instances.TITLE,
                        CalendarContract.Instances.BEGIN,
                        CalendarContract.Instances.END,
                        CalendarContract.Instances.ALL_DAY,
                        CalendarContract.Instances.DESCRIPTION
                    }, null, null, CalendarContract.Instances.BEGIN + " ASC");
                if (c != null) {
                    while (c.moveToNext()) {
                        String desc = c.getString(4);
                        if (desc != null && desc.contains("[Start]")) continue;
                        String title = c.getString(0);
                        if (title == null || title.length() == 0) title = "(geen titel)";
                        JSObject o = new JSObject();
                        o.put("n", title);
                        o.put("s", c.getLong(1));
                        o.put("e", c.getLong(2));
                        o.put("all", c.getInt(3) == 1);
                        arr.put(o);
                    }
                    c.close();
                }
            }
        } catch (Exception e) {
            // negeren
        }
        ret.put("events", arr);
        call.resolve(ret);
    }

    @PluginMethod
    public void refreshWidget(PluginCall call) {
        StartWidget.updateAll(getContext());
        call.resolve();
    }

    // ---------- Herinneringen ----------
    @PluginMethod
    public void requestNotif(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33) {
            String perm = "android.permission.POST_NOTIFICATIONS";
            if (ContextCompat.checkSelfPermission(getContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{perm}, 4711);
            }
        }
        call.resolve();
    }

    @PluginMethod
    public void startTimer(PluginCall call) {
        Long at = call.getLong("at");
        if (at == null) { call.resolve(); return; }
        ReminderReceiver.startTimer(getContext(), at, call.getString("title", "Start"), call.getString("text", ""));
        call.resolve();
    }

    @PluginMethod
    public void cancelTimer(PluginCall call) {
        ReminderReceiver.cancelTimer(getContext());
        call.resolve();
    }

    @PluginMethod
    public void setReminders(PluginCall call) {
        JSArray items = call.getArray("items");
        JSONArray arr = new JSONArray();
        try {
            if (items != null) {
                for (int i = 0; i < items.length(); i++) arr.put(items.getJSONObject(i));
            }
        } catch (Exception e) {
            // negeren
        }
        ReminderReceiver.schedule(getContext(), arr);
        call.resolve();
    }

    // ---------- Snelkoppeling op beginscherm ----------
    @PluginMethod
    public void pinShortcut(PluginCall call) {
        String target = call.getString("target", "dag");
        Context c = getContext();
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(c)) {
            call.reject("Je startscherm ondersteunt dit niet. Houd het app-icoon ingedrukt en sleep de snelkoppeling naar je beginscherm.");
            return;
        }
        String label;
        switch (target) {
            case "inbox": label = "Inbox"; break;
            case "notities": label = "Notities"; break;
            case "focus": label = "Wat nu?"; break;
            default: label = "Start";
        }
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("start://" + target));
        i.setClass(c, MainActivity.class);
        ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(c, "pin_" + target)
            .setShortLabel(label)
            .setIcon(IconCompat.createWithResource(c, R.mipmap.ic_launcher))
            .setIntent(i)
            .build();
        ShortcutManagerCompat.requestPinShortcut(c, info, null);
        call.resolve();
    }

    // ---------- Agenda ----------
    @PluginMethod
    public void syncCalendar(PluginCall call) {
        if (getPermissionState("calendar") != PermissionState.GRANTED) {
            requestPermissionForAlias("calendar", call, "calendarPermCallback");
            return;
        }
        doSync(call);
    }

    @PermissionCallback
    private void calendarPermCallback(PluginCall call) {
        if (getPermissionState("calendar") == PermissionState.GRANTED) {
            doSync(call);
        } else {
            call.reject("Geen toestemming voor je agenda. Zet die aan in de app-instellingen van Android.");
        }
    }

    private void doSync(PluginCall call) {
        try {
            ContentResolver cr = getContext().getContentResolver();
            long calId = -1;
            String calName = "";
            String[] proj = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.IS_PRIMARY,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
            };
            Cursor c = cr.query(CalendarContract.Calendars.CONTENT_URI, proj,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">=?", new String[]{"500"}, null);
            long firstId = -1;
            String firstName = "";
            if (c != null) {
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    String name = c.getString(1);
                    String type = c.getString(2);
                    int prim = c.getInt(3);
                    if (firstId < 0) { firstId = id; firstName = name; }
                    if (prim == 1 && "com.google".equals(type)) { calId = id; calName = name; break; }
                }
                c.close();
            }
            if (calId < 0) { calId = firstId; calName = firstName; }
            if (calId < 0) {
                call.reject("Geen agenda gevonden waar ik in mag schrijven.");
                return;
            }
            long from = call.getData().optLong("from", 0);
            long to = call.getData().optLong("to", 0);
            JSArray events = call.getArray("events");

            // Oude Start-blokken in dit bereik verwijderen
            List<Long> old = new ArrayList<>();
            Cursor q = cr.query(CalendarContract.Events.CONTENT_URI,
                new String[]{CalendarContract.Events._ID},
                CalendarContract.Events.CALENDAR_ID + "=? AND " + CalendarContract.Events.DESCRIPTION + " LIKE ? AND "
                    + CalendarContract.Events.DTSTART + ">=? AND " + CalendarContract.Events.DTSTART + "<=? AND "
                    + CalendarContract.Events.DELETED + "=0",
                new String[]{String.valueOf(calId), "%[Start]%", String.valueOf(from), String.valueOf(to)}, null);
            if (q != null) {
                while (q.moveToNext()) old.add(q.getLong(0));
                q.close();
            }
            for (Long id : old) {
                cr.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id), null, null);
            }

            int made = 0;
            if (events != null) {
                String tz = TimeZone.getDefault().getID();
                for (int i = 0; i < events.length(); i++) {
                    JSONObject e = events.getJSONObject(i);
                    ContentValues v = new ContentValues();
                    v.put(CalendarContract.Events.CALENDAR_ID, calId);
                    v.put(CalendarContract.Events.TITLE, e.optString("title"));
                    v.put(CalendarContract.Events.DESCRIPTION, e.optString("desc"));
                    v.put(CalendarContract.Events.DTSTART, e.getLong("start"));
                    v.put(CalendarContract.Events.DTEND, e.getLong("end"));
                    v.put(CalendarContract.Events.EVENT_TIMEZONE, tz);
                    v.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE);
                    v.put(CalendarContract.Events.HAS_ALARM, 0);
                    v.put(CalendarContract.Events.EVENT_COLOR, 0xFFE8B65C);
                    cr.insert(CalendarContract.Events.CONTENT_URI, v);
                    made++;
                }
            }
            JSObject ret = new JSObject();
            ret.put("calendar", calName);
            ret.put("count", made);
            call.resolve(ret);
        } catch (Exception ex) {
            call.reject("Agenda synchroniseren mislukt: " + ex.getMessage());
        }
    }
}
