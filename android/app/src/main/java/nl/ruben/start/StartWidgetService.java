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

        private static class Row {
            long ms; String n; String t; int a; boolean allDay; String id; String k; String ic = "cal";
        }

        private void load() {
            items = new JSONArray();
            try {
                java.util.List<Row> rows = new java.util.ArrayList<>();
                java.text.SimpleDateFormat hm = new java.text.SimpleDateFormat("HH:mm", new java.util.Locale("nl", "NL"));
                SharedPreferences sp = ctx.getSharedPreferences(StartNativePlugin.PREFS, Context.MODE_PRIVATE);
                JSONArray st = new JSONArray(sp.getString("widget_items", "[]"));
                for (int i = 0; i < st.length(); i++) {
                    JSONObject o = st.getJSONObject(i);
                    Row r = new Row();
                    r.ms = o.optLong("s", 0);
                    r.n = o.optString("n");
                    r.t = o.optString("t");
                    r.a = o.optInt("a", 0);
                    r.ic = o.optString("ic", "cal");
                    r.id = o.optString("id", "");
                    r.k = o.optString("k", "");
                    rows.add(r);
                }
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                long from = cal.getTimeInMillis();
                long to = from + 90L * 24 * 3600 * 1000;
                if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CALENDAR)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    android.net.Uri.Builder ub = android.provider.CalendarContract.Instances.CONTENT_URI.buildUpon();
                    android.content.ContentUris.appendId(ub, from);
                    android.content.ContentUris.appendId(ub, to);
                    android.database.Cursor c = ctx.getContentResolver().query(ub.build(),
                        new String[]{
                            android.provider.CalendarContract.Instances.TITLE,
                            android.provider.CalendarContract.Instances.BEGIN,
                            android.provider.CalendarContract.Instances.END,
                            android.provider.CalendarContract.Instances.ALL_DAY,
                            android.provider.CalendarContract.Instances.DESCRIPTION
                        }, null, null, android.provider.CalendarContract.Instances.BEGIN + " ASC");
                    if (c != null) {
                        while (c.moveToNext()) {
                            String desc = c.getString(4);
                            if (desc != null && desc.contains("[Start]")) continue;
                            String title = c.getString(0);
                            if (title == null || title.length() == 0) title = "(geen titel)";
                            long b = c.getLong(1), e = c.getLong(2);
                            boolean all = c.getInt(3) == 1;
                            Row r = new Row();
                            r.n = title;
                            r.a = 1;
                            r.allDay = all;
                            if (all) {
                                java.util.Calendar u = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
                                u.setTimeInMillis(b);
                                java.util.Calendar l = java.util.Calendar.getInstance();
                                l.clear();
                                l.set(u.get(java.util.Calendar.YEAR), u.get(java.util.Calendar.MONTH), u.get(java.util.Calendar.DAY_OF_MONTH), 0, 0, 0);
                                r.ms = l.getTimeInMillis();
                                r.t = "Hele dag";
                                if (r.ms < from) continue;
                            } else {
                                r.ms = b;
                                r.t = hm.format(new java.util.Date(b)) + " \u2013 " + hm.format(new java.util.Date(e));
                            }
                            // dubbele afspraken (zelfde tijd, begin van de titel gelijk) overslaan
                            String pr = r.n.toLowerCase().replaceAll("\\s+", " ");
                            pr = pr.length() > 10 ? pr.substring(0, 10) : pr;
                            for (int i = rows.size() - 1; i >= 0; i--) {
                                Row q = rows.get(i);
                                if (q.a == 1 && (q.id == null || q.id.length() == 0) && q.ms == r.ms) {
                                    String qp = q.n.toLowerCase().replaceAll("\\s+", " ");
                                    qp = qp.length() > 10 ? qp.substring(0, 10) : qp;
                                    if (qp.equals(pr)) {
                                        rows.remove(i);
                                        break;
                                    }
                                }
                            }
                            rows.add(r);
                        }
                        c.close();
                    }
                }
                java.util.Collections.sort(rows, new java.util.Comparator<Row>() {
                    @Override public int compare(Row x, Row y) {
                        int d = Long.compare(x.ms, y.ms);
                        if (d != 0) return d;
                        return x.n.compareToIgnoreCase(y.n);
                    }
                });
                java.text.SimpleDateFormat dayFmt = new java.text.SimpleDateFormat("d/M", new java.util.Locale("nl", "NL"));
                final String[] wdShort = {"ZON", "MAA", "DIN", "WOE", "DON", "VRI", "ZAT"};
                java.text.SimpleDateFormat wdFmt = new java.text.SimpleDateFormat("EEEE", new java.util.Locale("nl", "NL"));
                String lastDay = "";
                for (Row r : rows) {
                    if (r.ms < from || r.ms >= to) continue;
                    java.util.Date dt = new java.util.Date(r.ms);
                    String key = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(dt);
                    JSONObject o = new JSONObject();
                    if (!key.equals(lastDay)) {
                        lastDay = key;
                        long diff = (r.ms - from) / (24L * 3600 * 1000);
                        String dn = dayFmt.format(dt).replace(".", "");
                        java.util.Calendar wcal = java.util.Calendar.getInstance();
                        wcal.setTime(dt);
                        String dl = wdShort[wcal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
                        o.put("d", dn);
                        o.put("l", dl);
                    }
                    o.put("n", r.n);
                    o.put("t", r.t);
                    o.put("a", r.a);
                    o.put("ic", r.ic);
                    if (r.id != null && r.id.length() > 0) {
                        o.put("id", r.id);
                        o.put("k", r.k);
                    }
                    items.put(o);
                }
            } catch (Exception e) {
                // negeren
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
                android.content.SharedPreferences sp = ctx.getSharedPreferences(StartNativePlugin.PREFS, Context.MODE_PRIVATE);
                int text = StartWidget.color(sp, "w_text", "#E6D3A8");
                int acc = StartWidget.color(sp, "w_acc", "#D9B26A");
                int soft = (text & 0x00FFFFFF) | 0xC0000000;
                v.setTextColor(R.id.i_date, text);
                v.setTextColor(R.id.i_title, text);
                v.setTextColor(R.id.i_time, soft);
                v.setTextColor(R.id.i_check, acc);
                v.setTextViewText(R.id.i_date, d.length() > 0 ? (l + "\n" + d) : "");
                String icn = o.optString("ic", "cal");
                int icr = "home".equals(icn) ? R.drawable.ic_w_home : "check".equals(icn) ? R.drawable.ic_w_check : "sync".equals(icn) ? R.drawable.ic_w_sync : R.drawable.ic_w_cal;
                v.setImageViewResource(R.id.i_icon, icr);
                v.setInt(R.id.i_icon, "setColorFilter", o.optInt("a", 0) == 1 ? soft : acc);
                v.setTextViewText(R.id.i_title, o.optString("n"));
                v.setTextViewText(R.id.i_time, o.optString("t"));
                boolean noTime = o.optString("t").length() == 0;
                boolean hasLabel = o.optString("d").length() > 0;
                if (noTime && hasLabel) v.setTextViewText(R.id.i_time, " ");
                v.setViewVisibility(R.id.i_time, (noTime && !hasLabel) ? android.view.View.GONE : android.view.View.VISIBLE);
                v.setInt(R.id.i_bar, "setBackgroundColor", o.optInt("a", 0) == 1 ? soft : acc);
                Intent fill = new Intent();
                fill.putExtra("act", "open");
                v.setOnClickFillInIntent(R.id.i_title, fill);
                v.setOnClickFillInIntent(R.id.i_time, fill);
                v.setOnClickFillInIntent(R.id.i_date, fill);
                String id = o.optString("id", "");
                if (id.length() > 0) {
                    Intent dn = new Intent();
                    dn.putExtra("act", "done");
                    dn.putExtra("id", id);
                    dn.putExtra("k", o.optString("k", ""));
                    v.setOnClickFillInIntent(R.id.i_check, dn);
                    v.setViewVisibility(R.id.i_check, android.view.View.VISIBLE);
                } else {
                    v.setViewVisibility(R.id.i_check, android.view.View.GONE);
                }
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
