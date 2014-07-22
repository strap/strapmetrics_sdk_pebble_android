package com.straphq.strapacclandroid;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.json.JSONException;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.view.View;
import android.content.Context;
import android.content.BroadcastReceiver;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.straphq.strap.StrapMetrics;

public class MainActivity extends Activity
{
    private final static UUID PEBBLE_APP_UUID = UUID.fromString("C3F9FFFE-F74A-43F8-A210-14D5B75579D7");
    TextView thetext;
    int menuint = 1;
    BroadcastReceiver pebblebr;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        thetext = (TextView)findViewById(R.id.thetext);
        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        Log.i(getLocalClassName(), "Pebble is " + (connected ? "connected" : "not connected"));
        thetext.setText("Pebble is " + (connected ? "connected" : "not connected"));
        
        if (PebbleKit.areAppMessagesSupported(getApplicationContext())) {
          Log.i(getLocalClassName(), "App Message is supported!");
        }
        else {
          Log.i(getLocalClassName(), "App Message is not supported");
        }
        
        // init StrapMetrics
        // change the appID to match yours!
        
        final StrapMetrics sm = new StrapMetrics("rdjYKgrfeAPeMSjQ4");
        final Properties lp = new Properties();
        lp.put("resolution", "144x168");
        lp.put("useragent", "PEBBLE/2.0");
        
        pebblebr = PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
              
              //Log.i(getLocalClassName(), "Received value=" + data.toJsonString());
              
              if (sm.canHandleMsg(data)) {
              try {
				sm.processReceiveData(data, 200, lp);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
			}
              }
              
              PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
            }
        });
        
        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);

    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        Log.i(getLocalClassName(), "Paused");
//        try {
//            unregisterReceiver(pebblebr);
//		} catch (Exception e) {
//			// TODO: handle exception
//		}
        //PebbleKit.closeAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
    }
    
    public void sendMenu(View view) {
        PebbleDictionary data = new PebbleDictionary();
        data.addString(20, "RUNNING,Running"+Integer.toString(menuint++));
        PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);
    }
}
