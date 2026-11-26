package nl.ruben.start;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

public class StartWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(context));
        if (ids.length > 0) mgr.notifyAppWidgetViewDataChanged(ids, R.id.w_list);
    }

    static void updateAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, StartWidget.class));
        for (int id : ids) mgr.updateAppWidget(id, build(context));
        if (ids.length > 0) mgr.notifyAppWidgetViewDataChanged(ids, R.id.w_list);
    }

    static PendingIntent link(Context c, String target, int code) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("start://" + target));
        i.setClass(c, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getActivity(c, code, i, flags);
    }

    static int color(SharedPreferences sp, String key, String def) {
        try {
            return Color.parseColor(sp.getString(key, def));
        } catch (Exception e) {
            return Color.parseColor(def);
        }
    }

    static RemoteViews build(Context c) {
        SharedPreferences sp = c.getSharedPreferences(StartNativePlugin.PREFS, Context.MODE_PRIVATE);
        int bg = color(sp, "w_bg", "#0B0B0B") | 0xFF000000;
        int alpha = Math.max(0, Math.min(100, sp.getInt("w_alpha", 55)));
        int text = color(sp, "w_text", "#E6D3A8");
        int acc = color(sp, "w_acc", "#D9B26A");
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_start);
        v.setInt(R.id.w_bg, "setColorFilter", bg);
        v.setInt(R.id.w_bg, "setImageAlpha", Math.round(alpha * 2.55f));
        v.setTextColor(R.id.w_empty, text);
        v.setTextColor(R.id.w_day, acc);
        v.setTextColor(R.id.w_inbox, acc);
        v.setTextColor(R.id.w_notes, acc);
        v.setTextColor(R.id.w_focus, acc);
        Intent svc = new Intent(c, StartWidgetService.class);
        v.setRemoteAdapter(R.id.w_list, svc);
        v.setEmptyView(R.id.w_list, R.id.w_empty);
        Intent tpl = new Intent(c, WidgetActionActivity.class);
        tpl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
        v.setPendingIntentTemplate(R.id.w_list, PendingIntent.getActivity(c, 9, tpl, flags));
        v.setOnClickPendingIntent(R.id.w_empty, link(c, "dag", 11));
        v.setOnClickPendingIntent(R.id.w_day, link(c, "dag", 12));
        v.setOnClickPendingIntent(R.id.w_inbox, link(c, "inbox", 13));
        v.setOnClickPendingIntent(R.id.w_notes, link(c, "notities", 14));
        v.setOnClickPendingIntent(R.id.w_focus, link(c, "focus", 15));
        return v;
    }
}
