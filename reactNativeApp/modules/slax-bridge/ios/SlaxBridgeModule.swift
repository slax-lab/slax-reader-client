import ExpoModulesCore
import SlaxBridgeCore

public class SlaxBridgeModule: Module {
    public func definition() -> ModuleDefinition {
        Name("SlaxBridge")

        AsyncFunction("invoke") { (method: String, payload: [String: Any], promise: Promise) in
            guard let handler = SlaxBridgeRegistry.handler else {
                promise.reject("ERR_NOT_INITIALIZED", "SlaxBridge handler not registered")
                return
            }
            handler(method, payload) { result in
                switch result {
                case .success(let value):
                    promise.resolve(value)
                case .failure(let error):
                    promise.reject("ERR_BRIDGE", error.localizedDescription)
                }
            }
        }
    }
}
