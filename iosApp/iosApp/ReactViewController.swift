import UIKit
import React

@objc(ReactViewController)
public class ReactViewController: UIViewController {

    private let moduleName: String
    private let initialProps: [String: Any]?

    @objc public static func create(moduleName: String) -> ReactViewController {
        return ReactViewController(moduleName: moduleName)
    }

    @objc public init(moduleName: String, initialProps: [String: Any]? = nil) {
        self.moduleName = moduleName
        self.initialProps = initialProps
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func loadView() {
        guard let rootView = ReactBridgeManager.shared.createRootView(
            moduleName: moduleName,
            initialProperties: initialProps
        ) else {
            let errorView = UIView()
            errorView.backgroundColor = .systemBackground
            self.view = errorView
            return
        }
        self.view = rootView
    }
}
