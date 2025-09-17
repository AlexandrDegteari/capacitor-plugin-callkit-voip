import type { PluginListenerHandle } from '@capacitor/core';

export interface CallKitVoipPlugin {
  /**
   * Initialize plugin with main activity class
   */
  initialize(options?: { mainActivityClass?: string }): Promise<{ success: boolean }>;

  /**
   * Register for push notifications
   */
  register(options?: { userToken?: string }): Promise<void>;

  /**
   * Answer an incoming call (from Capacitor UI)
   */
  answerCall(options: { connectionId: string }): Promise<{ success: boolean }>;

  /**
   * Reject an incoming call
   */
  rejectCall(options: { connectionId: string }): Promise<{ success: boolean }>;

  addListener(
      eventName: 'registration',
      listenerFunc: (token: CallToken) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  addListener(
      eventName: 'incomingCall',
      listenerFunc: (callData: IncomingCallData) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  addListener(
      eventName: 'callAnswered',
      listenerFunc: (callData: CallAnsweredData) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  addListener(
      eventName: 'callRejected',
      listenerFunc: (callData: CallRejectedData) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  addListener(
      eventName: 'callStarted',
      listenerFunc: (callData: CallData) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  addListener(
      eventName: 'callEnded',
      listenerFunc: (callData: CallData) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}




export type CallType = 'video' | 'audio';

export interface CallToken {
  /**
   * VOIP Token
   */
  value: string;
}

export interface CallData {
  /**
   * Call ID
   */
  id: string;
  /**
   * Call Type
   */
  media?: CallType;
  /**
   * Call Display name
   */
  name?: string;
  /**
   * Call duration
   */
  duration?: string;
}

export interface IncomingCallData {
  /**
   * Username of the caller
   */
  username: string;
  /**
   * Connection ID for the call
   */
  connectionId: string;
  /**
   * Token for WebRTC connection
   */
  token?: string;
  /**
   * Room name for the call
   */
  roomName?: string;
  /**
   * Type of notification (incoming)
   */
  type: 'incoming';
}

export interface CallAnsweredData {
  /**
   * Username of the caller
   */
  username: string;
  /**
   * Connection ID for the call
   */
  connectionId: string;
  /**
   * Where the call was answered from
   */
  answeredFrom: 'notification' | 'ui';
}

export interface CallRejectedData {
  /**
   * Username of the caller
   */
  username: string;
  /**
   * Connection ID for the call
   */
  connectionId: string;
}