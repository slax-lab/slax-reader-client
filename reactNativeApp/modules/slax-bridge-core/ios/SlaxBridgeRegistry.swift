import Foundation

public typealias SlaxBridgeCallback = (Result<[String: Any], Error>) -> Void
public typealias SlaxBridgeHandler = (String, [String: Any], @escaping SlaxBridgeCallback) -> Void

public final class SlaxBridgeRegistry {
    public static var handler: SlaxBridgeHandler?
}
