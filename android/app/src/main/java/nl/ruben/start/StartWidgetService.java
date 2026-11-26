package nl.ruben.start;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONArray;
import org.json.JSONObject;

public class StartWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    static class Factory implements RemoteViewsFactory {
        private final Context ctx;
        private JSONArray items = new JSONArray();

        Factory(Context c) {
            ctx = c;
        }

        private void load() {
            try {
                SharedPreferences sp = ctx.getSharedPreferences(StartNativePlugin.PREFS, Context.MODE_PRIVATE);
                items = new JSONArray(sp.getString("widget_items", "[]"));
            } catch (Exception e) {
                items = new JSONArray();
            }
        }

        @Override public void onCreate() { load(); }
        @Override public void onDataSetChanged() { load(); }
        @Override public void onDestroy() {}
        @Override public int getCount() { return items.length(); }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_item);
            try {
                JSONObject o = items.getJSONObject(position);
                String d = o.optString("d", "");
                String l = o.optString("l", "");
                v.setTextViewText(R.id.i_date, d.length() > 0 ? (d + "\n" + l) : "");
                v.setTextViewText(R.id.i_title, o.optString("n"));
                v.setTextViewText(R.id.i_time, o.optString("t"));
                v.setInt(R.id.i_bar, "setBackgroundColor", o.optInt("a", 0) == 1 ? Color.parseColor("#9AB2B0") : Color.parseColor("#E8B65C"));
                Intent fill = new Intent();
                fill.setData(Uri.parse("start://dag"));
                v.setOnClickFillInIntent(R.id.i_title, fill);
                v.setOnClickFillInIntent(R.id.i_time, fill);
                v.setOnClickFillInIntent(R.id.i_date, fill);
            } catch (Exception e) {
                // negeren
            }
            return v;
        }

        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int position) { return position; }
        @Override public boolean hasStableIds() { return false; }
    }
}
