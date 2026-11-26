package nl.ruben.start;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

public class StartWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(context));
    }

    static void updateAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, StartWidget.class));
        for (int id : ids) mgr.updateAppWidget(id, build(context));
    }

    static PendingIntent link(Context c, String target, int code) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("start://" + target));
        i.setClass(c, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getActivity(c, code, i, flags);
    }

    static RemoteViews build(Context c) {
        SharedPreferences sp = c.getSharedPreferences(StartNativePlugin.PREFS, Context.MODE_PRIVATE);
        String text = sp.getString("widget_text", "");
        int more = sp.getInt("widget_more", 0);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_start);
        String body = text;
        if (body == null || body.length() == 0) {
            body = more > 0 ? "Nog niets ingepland." : "Niets meer te doen. Fijn.";
        }
        v.setTextViewText(R.id.w_body, body);
        v.setTextViewText(R.id.w_more, more > 0 ? (more + " zonder tijd, tik om te plaatsen") : "");
        v.setOnClickPendingIntent(R.id.w_root, link(c, "dag", 10));
        v.setOnClickPendingIntent(R.id.w_body, link(c, "dag", 11));
        v.setOnClickPendingIntent(R.id.w_title, link(c, "dag", 12));
        v.setOnClickPendingIntent(R.id.w_inbox, link(c, "inbox", 13));
        v.setOnClickPendingIntent(R.id.w_notes, link(c, "notities", 14));
        v.setOnClickPendingIntent(R.id.w_focus, link(c, "focus", 15));
        return v;
    }
}
