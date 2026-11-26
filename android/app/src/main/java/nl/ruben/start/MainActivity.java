package nl.ruben.start;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(StartNativePlugin.class);
        super.onCreate(savedInstanceState);
        handleTarget(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTarget(intent);
    }

    private void handleTarget(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data != null && "start".equals(data.getScheme())) {
            String host = data.getHost();
            if (host != null && host.length() > 0) {
                StartNativePlugin.pendingTarget = host;
            }
        }
    }
}
