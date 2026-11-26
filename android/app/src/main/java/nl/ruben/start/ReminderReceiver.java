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
        if ("nudge".equals(intent.getStringExtra("type"))) {
            title = "Start";
            text = composeNudge(context, intent);
        }
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

    static String composeNudge(Context context, Intent intent) {
        int slot = intent.getIntExtra("slot", 0);
        boolean heavy = intent.getIntExtra("heavy", 0) == 1;
        int total = intent.getIntExtra("total", 0);
        String k = intent.getStringExtra("k");
        java.util.List<String[]> open = new java.util.ArrayList<>();
        try {
            SharedPreferences sp = context.getSharedPreferences(StartNativePlugin.PREFS, Context.MODE_PRIVATE);
            JSONArray done = new JSONArray(sp.getString("widget_done", "[]"));
            JSONArray cands = new JSONArray(intent.getStringExtra("cands"));
            for (int i = 0; i < cands.length(); i++) {
                JSONObject c = cands.getJSONObject(i);
                boolean isDone = false;
                for (int j = 0; j < done.length(); j++) {
                    JSONObject d = done.getJSONObject(j);
                    if (c.optString("id").equals(d.optString("id")) && (k == null || k.equals(d.optString("k")))) isDone = true;
                }
                if (!isDone) open.add(new String[]{c.optString("n"), String.valueOf(c.optInt("m", -1))});
            }
        } catch (Exception e) {
            // negeren
        }
        java.util.Random rnd = new java.util.Random();
        if (total == 0) {
            String[] g = {
                "H\u00E9 Ruben, een vriendelijk hoi. Zin in \u00E9\u00E9n klein dingetje? Tik op Wat nu? voor een idee \u2728",
                "Hoi Ruben! Er staat niets dringends open. Als je zin hebt, is Wat nu? een zacht begin \uD83C\uDF31"
            };
            return g[rnd.nextInt(g.length)];
        }
        if (open.isEmpty()) {
            String[] g = {
                "H\u00E9 Ruben, alles wat je voor vandaag gepland had is gedaan. Wat knap, geniet van je rust \uD83C\uDF1F",
                "Ruben, je lijstje voor vandaag is leeg. Lekker bezig geweest! Neem gerust rust \uD83D\uDC9B",
                "Alles afgevinkt voor vandaag. Goed gedaan, Ruben \uD83C\uDF1F"
            };
            return g[rnd.nextInt(g.length)];
        }
        String n1 = open.get(0)[0];
        int m1 = Integer.parseInt(open.get(0)[1]);
        String names = n1;
        if (!heavy && open.size() > 1) names = n1 + " en " + open.get(1)[0];
        String hint = m1 >= 0 ? String.format(java.util.Locale.US, "%02d:%02d", m1 / 60, m1 % 60) : null;
        String[] t;
        if (heavy) {
            t = new String[]{
                "Rustig aan vandaag, Ruben. Als je wilt, alleen " + n1 + ". Of gewoon lekker niks, ook goed \uD83D\uDC9B",
                "Het is een zware dag, dus het mag klein blijven. Misschien alleen " + n1 + "? Anders is rust ook prima \uD83C\uDF31"
            };
        } else if (slot >= 2) {
            t = new String[]{
                "Na het avondeten is er misschien nog ruimte voor \u00E9\u00E9n ding: " + names + ". Alleen als het je past \uD83C\uDF19",
                "H\u00E9 Ruben, een vriendelijk duwtje: " + names + " staat nog open. Een kwartiertje is al mooi \uD83C\uDF19",
                "Rustig avondmoment? " + n1 + " wacht nog op je. Begin gerust met \u00E9\u00E9n minuut \u2728"
            };
        } else if (hint != null) {
            t = new String[]{
                "H\u00E9 Ruben, " + n1 + " staat om " + hint + " gepland. Misschien een mooi moment om te beginnen? \uD83D\uDCAA",
                "Een zacht duwtje: " + n1 + " (gepland om " + hint + "). \u00C9\u00E9n kleine stap is genoeg om te starten \uD83C\uDF31",
                "Hoi Ruben! Om " + hint + " staat " + n1 + " op de planning. Pak het lekker rustig op \u2600\uFE0F"
            };
        } else {
            t = new String[]{
                "H\u00E9 Ruben, " + names + " staat nog op je lijstje. Misschien een mooi moment om er even een kwartiertje in te steken? \uD83D\uDCAA",
                "Een zacht duwtje: " + names + " wacht nog op je. \u00C9\u00E9n kleine stap is genoeg om te beginnen \uD83C\uDF31",
                "Hoi Ruben! " + names + " staat nog open. Jij bepaalt het tempo \u2600\uFE0F"
            };
        }
        return t[rnd.nextInt(t.length)];
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
                if ("nudge".equals(o.optString("type"))) {
                    in.putExtra("type", "nudge");
                    in.putExtra("slot", o.optInt("slot", 0));
                    in.putExtra("heavy", o.optInt("heavy", 0));
                    in.putExtra("total", o.optInt("total", 0));
                    in.putExtra("k", o.optString("k", ""));
                    JSONArray cc = o.optJSONArray("cands");
                    in.putExtra("cands", cc == null ? "[]" : cc.toString());
                }
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
