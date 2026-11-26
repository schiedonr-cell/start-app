package nl.ruben.start;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            SharedPreferences sp = context.getSharedPreferences(StartNativePlugin.PREFS, Context.MODE_PRIVATE);
            JSONArray items = new JSONArray(sp.getString("rem_items", "[]"));
            ReminderReceiver.schedule(context, items);
        } catch (Exception e) {
            // negeren
        }
        StartWidget.updateAll(context);
    }
}
