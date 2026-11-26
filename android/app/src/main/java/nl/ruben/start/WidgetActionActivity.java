package nl.ruben.start;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONObject;

/** Onzichtbare tussenstap voor klikken in de widget: afvinken of de app openen. */
public class WidgetActionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent in = getIntent();
        String act = in == null ? null : in.getStringExtra("act");
        try {
            if ("done".equals(act)) {
                String id = in.getStringExtra("id");
                String k = in.getStringExtra("k");
                if (id != null && k != null) recordDone(this, id, k);
            } else {
                Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("start://dag"));
                open.setClass(this, MainActivity.class);
                open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(open);
            }
        } catch (Exception e) {
            // negeren
        }
        finish();
        overridePendingTransition(0, 0);
    }

    static void recordDone(Context context, String id, String k) throws Exception {
        SharedPreferences sp = context.getSharedPreferences(StartNativePlugin.PREFS, Context.MODE_PRIVATE);
        JSONArray done = new JSONArray(sp.getString("widget_done", "[]"));
        JSONObject d = new JSONObject();
        d.put("id", id);
        d.put("k", k);
        done.put(d);
        JSONArray it = new JSONArray(sp.getString("widget_items", "[]"));
        JSONArray out = new JSONArray();
        for (int i = 0; i < it.length(); i++) {
            JSONObject o = it.getJSONObject(i);
            if (id.equals(o.optString("id")) && k.equals(o.optString("k"))) continue;
            out.put(o);
        }
        sp.edit().putString("widget_done", done.toString()).putString("widget_items", out.toString()).apply();
        StartWidget.updateAll(context);
    }
}
