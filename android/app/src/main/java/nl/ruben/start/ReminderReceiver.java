package nl.ruben.start;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL = "start_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String text = intent.getStringExtra("text");
        int id = intent.getIntExtra("id", 1);
        showNotification(context, id, title == null ? "Start" : title, text == null ? "" : text);
    }

    static void showNotification(Context context, int id, String title, String text) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Herinneringen", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Herinneringen voor je geplande blokken");
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("start://dag"));
        open.setClass(context, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(context, id, open, flags);
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(context).notify(id, b.build());
        } catch (SecurityException e) {
            // geen toestemming voor meldingen
        }
    }

    /** Plant alle herinneringen opnieuw en bewaart de lijst voor na een herstart. */
    static void schedule(Context context, JSONArray items) {
        SharedPreferences sp = context.getSharedPreferences(StartNativePlugin.PREFS, Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        try {
            // Oude annuleren
            String prev = sp.getString("rem_items", "[]");
            JSONArray old = new JSONArray(prev);
            for (int i = 0; i < old.length(); i++) {
                int id = old.getJSONObject(i).getInt("id");
                Intent in = new Intent(context, ReminderReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(context, id, in, flags);
                am.cancel(pi);
            }
            // Nieuwe plannen
            long now = System.currentTimeMillis();
            for (int i = 0; i < items.length(); i++) {
                JSONObject o = items.getJSONObject(i);
                long at = o.getLong("at");
                if (at <= now) continue;
                int id = o.getInt("id");
                Intent in = new Intent(context, ReminderReceiver.class);
                in.putExtra("id", id);
                in.putExtra("title", o.optString("title"));
                in.putExtra("text", o.optString("text"));
                PendingIntent pi = PendingIntent.getBroadcast(context, id, in, flags);
                if (Build.VERSION.SDK_INT >= 23) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, at, pi);
                }
            }
            sp.edit().putString("rem_items", items.toString()).apply();
        } catch (Exception e) {
            // negeren
        }
    }
}
