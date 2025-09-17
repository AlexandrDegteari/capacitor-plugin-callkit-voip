package com.bfine.capactior.callkitvoip;

import com.getcapacitor.Bridge;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginHandle;
import com.getcapacitor.PluginMethod;
import com.google.firebase.messaging.FirebaseMessaging;
import com.bfine.capactior.callkitvoip.androidcall.VoipForegroundService;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class CallKitVoipPlugin extends Plugin {
    public  static  Bridge                      staticBridge = null;
    public          MyFirebaseMessagingService  messagingService;
    private static  Class<?>                    mainActivityClass = null;
    private static  Context                     applicationContext = null;


    @Override
    public void load(){
        staticBridge   = this.bridge;
        applicationContext = this.getActivity().getApplicationContext();
        // By default, use the current activity class
        mainActivityClass = this.getActivity().getClass();
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        String activityClassName = call.getString("mainActivityClass");
        if (activityClassName != null) {
            try {
                mainActivityClass = Class.forName(activityClassName);
                Log.d("CallKitVoip", "Main activity class set to: " + activityClassName);
            } catch (ClassNotFoundException e) {
                Log.e("CallKitVoip", "Could not find activity class: " + activityClassName);
            }
        }
        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void register(PluginCall call) {
        final String topicName = call.getString("userToken");
        Log.d("CallKitVoip","register");

        if(topicName == null){
            call.reject("Topic name hasn't been specified correctly");
            return;
        }
        FirebaseMessaging
                .getInstance()
                .subscribeToTopic(topicName)
                .addOnSuccessListener(unused -> {
                    JSObject ret = new JSObject();
                    Logger.debug("CallKit: Subscribed");
                    ret.put("message", "Subscribed to topic " + topicName);
                    call.resolve(ret);

                })
                .addOnFailureListener(e -> {
                    Logger.debug("CallKit: Cannot subscribe");
                    call.reject("Cant subscribe to topic" + topicName);
                });
        call.resolve();
    }
    // Called when there's an incoming call
    public void notifyIncomingCall(String username, String connectionId, String token, String roomName) {
        Log.d("CallKitVoip", "Notifying incoming call from: " + username);

        JSObject data = new JSObject();
        data.put("username", username);
        data.put("connectionId", connectionId);
        data.put("token", token);
        data.put("roomName", roomName);
        data.put("type", "incoming");

        notifyListeners("incomingCall", data);
    }

    // Called when call is answered from notification
    public void notifyCallAnswered(String username, String connectionId) {
        Log.d("CallKitVoip", "Call answered from notification: " + username);

        JSObject data = new JSObject();
        data.put("username", username);
        data.put("connectionId", connectionId);
        data.put("answeredFrom", "notification");

        notifyListeners("callAnswered", data);
    }

    // Called when call is rejected
    public void notifyCallRejected(String username, String connectionId) {
        Log.d("CallKitVoip", "Call rejected: " + username);

        JSObject data = new JSObject();
        data.put("username", username);
        data.put("connectionId", connectionId);

        notifyListeners("callRejected", data);
    }

    // Called when call ends
    public void notifyCallEnded(String connectionId) {
        Log.d("CallKitVoip", "Call ended: " + connectionId);

        JSObject data = new JSObject();
        data.put("connectionId", connectionId);

        notifyListeners("callEnded", data);
    }

    @PluginMethod
    public void answerCall(PluginCall call) {
        String connectionId = call.getString("connectionId");
        Log.d("CallKitVoip", "Answer call from UI: " + connectionId);

        // Stop the ringtone/vibration if playing
        stopCallNotification();

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void rejectCall(PluginCall call) {
        String connectionId = call.getString("connectionId");
        Log.d("CallKitVoip", "Reject call: " + connectionId);

        // Stop the ringtone/vibration if playing
        stopCallNotification();

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    private void stopCallNotification() {
        // This will be called to stop the foreground service
        if (applicationContext != null) {
            Intent stopIntent = new Intent(applicationContext, VoipForegroundService.class);
            applicationContext.stopService(stopIntent);
        }
    }

    public void notifyEvent(String eventName, String username, String connectionId){
        Log.d("notifyEvent",eventName + "  " + username + "   " + connectionId);
        // Legacy method kept for compatibility
    }

    public static Class<?> getMainActivityClass() {
        return mainActivityClass;
    }

    public static Context getApplicationContext() {
        return applicationContext;
    }

    public static CallKitVoipPlugin getInstance() {
        if (staticBridge == null || staticBridge.getWebView() == null)
            return  null;

        PluginHandle handler = staticBridge.getPlugin("CallKitVoip");

        return handler == null
                ? null
                : (CallKitVoipPlugin) handler.getInstance();
    }

}
