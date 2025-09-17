# Android VOIP Plugin Integration Guide

## Overview
The updated VOIP plugin now uses the main Capacitor WebView activity instead of a separate CallActivity. This provides better integration with your app's UI and allows you to handle call UI entirely in your web application.

## How It Works

### 1. Plugin Initialization
When the app starts, initialize the plugin with your main activity class:

```javascript
import { CallKitVoip } from 'capacitor-plugin-callkit-voip';

// Initialize plugin on app start
await CallKitVoip.initialize({
  mainActivityClass: 'com.yourcompany.yourapp.MainActivity' // Optional, defaults to current activity
});

// Register for push notifications
await CallKitVoip.register({
  userToken: 'user_unique_token'
});
```

### 2. Handling Incoming Calls
Set up listeners for incoming call events:

```javascript
// Listen for incoming calls
CallKitVoip.addListener('incomingCall', (data) => {
  console.log('Incoming call from:', data.username);
  console.log('Connection ID:', data.connectionId);
  console.log('Token:', data.token);
  console.log('Room:', data.roomName);

  // Show your custom incoming call UI
  showIncomingCallScreen(data);
});

// Listen for call answered from notification
CallKitVoip.addListener('callAnswered', (data) => {
  console.log('Call answered:', data.username);
  console.log('Answered from:', data.answeredFrom); // 'notification' or 'ui'

  if (data.answeredFrom === 'notification') {
    // Call was answered from notification, connect WebRTC
    connectWebRTC(data.connectionId);
  }
});

// Listen for call rejected
CallKitVoip.addListener('callRejected', (data) => {
  console.log('Call rejected:', data.username);
  // Hide call UI
  hideCallScreen();
});
```

### 3. Answering/Rejecting from UI
When user interacts with your Capacitor UI:

```javascript
// Answer call from your UI
async function answerCall(connectionId) {
  await CallKitVoip.answerCall({ connectionId });
  // Connect WebRTC
  connectWebRTC(connectionId);
}

// Reject call from your UI
async function rejectCall(connectionId) {
  await CallKitVoip.rejectCall({ connectionId });
  // Hide call UI
  hideCallScreen();
}
```

## Push Notification Flow

1. **Push Received**: FCM receives push with `type: "call"`
2. **Foreground Service Started**: Plugin starts foreground service with incoming call notification
3. **Callback to App**: Plugin sends `incomingCall` event to your app
4. **Main Activity Opened**: If app is in background/locked, main activity opens
5. **User Action**: User can answer/reject from:
   - Notification actions in status bar
   - Your Capacitor UI

## Notification Handling

The plugin creates an incoming call notification with:
- **Title**: Caller's username
- **Actions**: Answer and Reject buttons
- **Full Screen Intent**: Opens your app when phone is locked
- **Ringtone & Vibration**: Plays default ringtone and vibrates

## Activity Lifecycle

When a call arrives:
- If app is **foreground**: Shows your incoming call UI directly
- If app is **background**: Notification appears, app opens when interacted
- If phone is **locked**: Full screen intent wakes and shows app

## Required Permissions

The following permissions are already configured in the plugin's AndroidManifest:
- `android.permission.CALL_PHONE`
- `android.permission.DISABLE_KEYGUARD`
- `android.permission.MANAGE_OWN_CALLS`
- `android.permission.VIBRATE`
- `android.permission.INTERNET`

## Migration from CallActivity

If you're migrating from the old CallActivity approach:

1. Remove any references to CallActivity in your code
2. Implement incoming call UI in your web app
3. Handle call state through the plugin's event listeners
4. Use `answerCall` and `rejectCall` methods instead of activity intents

## Example Implementation

```javascript
// IncomingCallComponent.vue or similar
export default {
  data() {
    return {
      incomingCall: null,
      isCallActive: false
    };
  },

  mounted() {
    // Set up listeners
    CallKitVoip.addListener('incomingCall', this.handleIncomingCall);
    CallKitVoip.addListener('callAnswered', this.handleCallAnswered);
    CallKitVoip.addListener('callRejected', this.handleCallRejected);
  },

  methods: {
    handleIncomingCall(data) {
      this.incomingCall = data;
      // Show incoming call UI
      this.$refs.incomingCallModal.show();
      // Play ringtone if needed
      this.playRingtone();
    },

    handleCallAnswered(data) {
      if (data.answeredFrom === 'notification') {
        // Call was answered from notification
        this.isCallActive = true;
        this.connectToCall(data.connectionId);
      }
    },

    handleCallRejected(data) {
      this.incomingCall = null;
      this.$refs.incomingCallModal.hide();
      this.stopRingtone();
    },

    async answerCall() {
      await CallKitVoip.answerCall({
        connectionId: this.incomingCall.connectionId
      });
      this.isCallActive = true;
      this.connectToCall(this.incomingCall.connectionId);
      this.$refs.incomingCallModal.hide();
    },

    async rejectCall() {
      await CallKitVoip.rejectCall({
        connectionId: this.incomingCall.connectionId
      });
      this.$refs.incomingCallModal.hide();
      this.stopRingtone();
    },

    connectToCall(connectionId) {
      // Your WebRTC connection logic here
      // Use the token and roomName from incomingCall data
    }
  }
};
```

## Testing

To test the integration:

1. Send a push notification with the following data:
```json
{
  "type": "call",
  "username": "John Doe",
  "connectionId": "unique_call_id",
  "token": "webrtc_token",
  "roomName": "room_123"
}
```

2. Verify that:
   - Notification appears with Answer/Reject actions
   - Your app receives the `incomingCall` event
   - Answering from notification triggers `callAnswered` event
   - Your UI updates appropriately

## Troubleshooting

- **App not opening on locked screen**: Ensure your main activity has proper flags in AndroidManifest
- **No notification sound**: Check device ringer mode and notification channel settings
- **Events not received**: Verify plugin is initialized and listeners are registered before calls arrive
- **Main activity class not found**: Double-check the class name passed to `initialize()`