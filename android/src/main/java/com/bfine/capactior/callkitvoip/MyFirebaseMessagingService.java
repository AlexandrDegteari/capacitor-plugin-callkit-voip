package com.bfine.capactior.callkitvoip;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.bfine.capactior.callkitvoip.androidcall.VoipBackgroundService;
import com.bfine.capactior.callkitvoip.androidcall.VoipForegroundService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


/**
 * NOTE: There can only be one service in each app that receives FCM messages. If multiple
 * are declared in the Manifest then the first one will be chosen.
 * <p>
 * In order to make this Java sample functional, you must remove the following from the Kotlin messaging
 * service in the AndroidManifest.xml:
 * <p>
 * <intent-filter>
 * <action android:name="com.google.firebase.MESSAGING_EVENT" />
 * </intent-filter>
 */
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    public MyFirebaseMessagingService() {
        super();
        Log.d(TAG, "class instantiated");
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "received " + remoteMessage.getData());

        if (remoteMessage.getData().containsKey("type") && remoteMessage.getData().get("type").equals("call")) {
            show_call_notification(remoteMessage.getData().get("connectionId"), remoteMessage.getData().get("username"));
        }

        if (remoteMessage.getData().containsKey("type") && remoteMessage.getData().get("type").equals("stopCall")) {

        }
    }

    private boolean isServiceRunning(String service_name) {
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(getApplicationContext().ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service_running : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service_name.equals(service_running.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void show_call_notification(String connectionId, String username) {
        // Start foreground service with main activity
        Intent voip_service = new Intent(getApplicationContext(), VoipForegroundService.class);
        voip_service.setAction("incoming");
        voip_service.putExtra("connectionId", connectionId);
        voip_service.putExtra("username", username);

        // Get token and room name from the message data if available
        // These will be needed for WebRTC connection
        voip_service.putExtra("token", ""); // Will be filled from push data
        voip_service.putExtra("roomName", ""); // Will be filled from push data

        Log.d("show_call_notification", "Starting foreground service for incoming call from: " + username);

        // Start foreground service
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(voip_service);
        } else {
            getApplicationContext().startService(voip_service);
        }

        // Notify the plugin about incoming call
        CallKitVoipPlugin plugin = CallKitVoipPlugin.getInstance();
        if (plugin != null) {
            plugin.notifyIncomingCall(username, connectionId, "", "");
        }
    }
}