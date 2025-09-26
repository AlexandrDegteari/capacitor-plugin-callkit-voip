import Foundation
import Capacitor
import UIKit
import CallKit
import PushKit

/**
 *  CallKit Voip Plugin provides native PushKit functionality with apple CallKit to capacitor
 */
@objc(CallKitVoipPlugin)
public class CallKitVoipPlugin: CAPPlugin {

    private var provider: CXProvider?
    private let voipRegistry            = PKPushRegistry(queue: nil)
    private var connectionIdRegistry : [UUID: CallConfig] = [:]

    @objc func register(_ call: CAPPluginCall) {
        voipRegistry.delegate = self
        voipRegistry.desiredPushTypes = [.voIP]
        let config = CXProviderConfiguration(localizedName: "Secure Call")
        config.maximumCallGroups = 1
        config.maximumCallsPerCallGroup = 1
        // Native call log shows video icon if it was video call.
        config.supportsVideo = true
        // Support generic type to handle *User ID*
        config.supportedHandleTypes = [.generic]
        provider = CXProvider(configuration: config)
        provider?.setDelegate(self, queue: DispatchQueue.main)
        call.resolve()
    }

    @objc func endActiveCall(_ call: CAPPluginCall) {
        // End any active calls
        print("Ending active calls from JavaScript")

        // Find and end all active calls
        for (uuid, _) in connectionIdRegistry {
            endCall(uuid: uuid)
        }

        call.resolve()
    }

    public func notifyEvent(eventName: String, uuid: UUID, clearConfig: Bool = false){
        if let config = connectionIdRegistry[uuid] {
            notifyListeners(eventName, data: [
                "id": config.id,
                "media": config.media,
                "name"    : config.name,
                "duration"    : config.duration,
            ])
            // Only clear the config if explicitly requested (e.g., when call ends)
            if clearConfig {
                connectionIdRegistry[uuid] = nil
            }
        }
    }

    public func incomingCall(id: String, media: String, name: String, duration: String) {
        let update                      = CXCallUpdate()
        update.remoteHandle             = CXHandle(type: .generic, value: name)
        update.hasVideo                 = media == "video"
        update.supportsDTMF             = false
        update.supportsHolding          = true
        update.supportsGrouping         = false
        update.supportsUngrouping       = false
        let uuid = UUID()
        connectionIdRegistry[uuid] = .init(id: id, media: media, name: name, duration: duration)
        self.provider?.reportNewIncomingCall(with: uuid, update: update, completion: { (_) in })
    }




    public func endCall(uuid: UUID) {
        let controller = CXCallController()
        let transaction = CXTransaction(action: CXEndCallAction(call: uuid));controller.request(transaction,completion: { error in })
    }



}


// MARK: CallKit events handler

extension CallKitVoipPlugin: CXProviderDelegate {

    public func providerDidReset(_ provider: CXProvider) {

    }

    public func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        // Answers an incoming call
        print("CXAnswerCallAction answers an incoming call")

        // ВАЖНО: Сообщаем CallKit что звонок подключился - это скрывает CallKit UI
        provider.reportCall(with: action.callUUID, connectedAt: Date())
        print("📞 iOS CallKit: Reported call as connected - CallKit UI should be hidden")

        // Try to bring app to foreground using various methods
        DispatchQueue.main.async {
            // Method 1: Make window key and visible (works if app is backgrounded)
            if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = scene.windows.first {
                window.makeKeyAndVisible()
                print("📱 iOS: Making window key and visible")
            }

            // Method 2: Request scene activation (iOS 13+)
            if #available(iOS 13.0, *) {
                if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
                    UIApplication.shared.requestSceneSessionActivation(
                        scene.session,
                        userActivity: nil,
                        options: nil,
                        errorHandler: { error in
                            print("❌ Error activating scene: \(error)")
                        }
                    )
                    print("📱 iOS: Requested scene activation")
                }
            }

            // Method 3: Try to open app using URL scheme (fallback)
            if let url = URL(string: "soulmates://call-accepted") {
                UIApplication.shared.open(url, options: [:]) { success in
                    print("📱 iOS: URL scheme open success: \(success)")
                }
            }
        }

        // Notify the app that the call was answered (с небольшой задержкой чтобы CallKit UI успел скрыться)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.notifyEvent(eventName: "callAnswered", uuid: action.callUUID)
            print("📞 iOS CallKit: Notified app about call answer")
        }

        // Mark the action as fulfilled - this tells CallKit the call was accepted
        action.fulfill()

        print("📞 iOS CallKit: Call answered, connected, and app activation attempted")
    }

    public func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        // End the call
        print("CXEndCallAction represents ending call")
        notifyEvent(eventName: "callEnded", uuid: action.callUUID, clearConfig: true)
        action.fulfill()
    }

    public func provider(_ provider: CXProvider, perform action: CXStartCallAction) {
        // Report connection started
        print("CXStartCallAction represents initiating an outgoing call")
        notifyEvent(eventName: "callStarted", uuid: action.callUUID)
        action.fulfill()
    }


}

// MARK: PushKit events handler
extension CallKitVoipPlugin: PKPushRegistryDelegate {

    public func pushRegistry(_ registry: PKPushRegistry, didUpdate pushCredentials: PKPushCredentials, for type: PKPushType) {
        let parts = pushCredentials.token.map { String(format: "%02.2hhx", $0) }
        let token = parts.joined()
        print("Token: \(token)")
        notifyListeners("registration", data: ["value": token])
    }

    public func pushRegistry(_ registry: PKPushRegistry, didReceiveIncomingPushWith payload: PKPushPayload, for type: PKPushType, completion: @escaping () -> Void) {
         print("didReceiveIncomingPushWith")
         guard let id = payload.dictionaryPayload["id"] as? String else {
             return
         }
         let media = (payload.dictionaryPayload["media"] as? String) ?? "voice"
         let name = (payload.dictionaryPayload["name"] as? String) ?? "Unknown"
         let duration = (payload.dictionaryPayload["duration"] as? String) ?? "0"
         print("id: \(id)")
         print("name: \(name)")
         print("media: \(media)")
         print("duration: \(duration)")
        self.incomingCall(id: id, media: media, name: name, duration: duration)
    }

}


extension CallKitVoipPlugin {
    struct CallConfig {
        let id: String
        let media: String
        let name: String
        let duration: String
    }
}
