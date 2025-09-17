package com.bfine.capactior.callkitvoip.androidcall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.bfine.capactior.callkitvoip.CallKitVoipPlugin;


public class VoipForegroundServiceActionReceiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null ) {
            String action = intent.getAction();
            String token = intent.getStringExtra("token");
            String roomName = intent.getStringExtra("roomName");
            String username = intent.getStringExtra("username");


            if (action != null) {
                performClickAction(context, action,token,roomName,username);
            }

            // Close the notification after the click action is performed.


        }
    }
    private void performClickAction(Context context, String action,String token,String roomName,String username) {
        Log.d("performClickAction","action "+action + "   "+username);

        CallKitVoipPlugin plugin = CallKitVoipPlugin.getInstance();

        if (action.equals("RECEIVE_CALL")) {
            // Answer call from notification
            if (plugin != null) {
                plugin.notifyCallAnswered(username, roomName);
            }

            // Open main activity to show call UI
            Class<?> mainActivityClass = CallKitVoipPlugin.getMainActivityClass();
            if (mainActivityClass != null) {
                Intent dialogIntent = new Intent(context, mainActivityClass);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                dialogIntent.putExtra("call_action", "answer");
                dialogIntent.putExtra("token", token);
                dialogIntent.putExtra("roomName", roomName);
                dialogIntent.putExtra("username", username);
                context.startActivity(dialogIntent);
            }

            // Stop the foreground service
            context.stopService(new Intent(context, VoipForegroundService.class));
        }
        else if (action.equals("FULLSCREEN_CALL")) {
            // Open main activity to show incoming call UI
            Class<?> mainActivityClass = CallKitVoipPlugin.getMainActivityClass();
            if (mainActivityClass != null) {
                Intent dialogIntent = new Intent(context, mainActivityClass);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                dialogIntent.putExtra("call_action", "incoming");
                dialogIntent.putExtra("token", token);
                dialogIntent.putExtra("roomName", roomName);
                dialogIntent.putExtra("username", username);
                context.startActivity(dialogIntent);
            }
        }
        else if (action.equals("CANCEL_CALL")) {
            // Reject call
            if (plugin != null) {
                plugin.notifyCallRejected(username, roomName);
            }

            // Stop the foreground service
            context.stopService(new Intent(context, VoipForegroundService.class));

            // Close notification drawer
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        }
    }

}