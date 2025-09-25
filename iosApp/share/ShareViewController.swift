import UIKit
import Social
import ComposeApp

@MainActor
final class ShareViewController: UIViewController {
    
    // MARK: - UI Components
    private lazy var backgroundView: UIView = {
        let view = UIView()
        view.backgroundColor = .black.withAlphaComponent(0.5)
        view.translatesAutoresizingMaskIntoConstraints = false
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissExtension))
        view.addGestureRecognizer(tapGesture)
        return view
    }()
    
    private lazy var containerView: UIView = {
        let view = UIView()
        view.backgroundColor = .systemBackground
        view.layer.cornerRadius = 16
        view.layer.shadowColor = UIColor.black.cgColor
        view.layer.shadowOpacity = 0.1
        view.layer.shadowOffset = CGSize(width: 0, height: -2)
        view.layer.shadowRadius = 10
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private lazy var loadingIndicator: UIActivityIndicatorView = {
        let indicator = UIActivityIndicatorView(style: .medium)
        indicator.startAnimating()
        indicator.translatesAutoresizingMaskIntoConstraints = false
        return indicator
    }()
    
    private lazy var statusLabel: UILabel = {
        let label = UILabel()
        label.text = ComposeApp.ShareKt.getShareLabelText(key: "collecting")
        label.textColor = .label
        label.font = .systemFont(ofSize: 16)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var successImageView: UIImageView = {
        let imageView = UIImageView()
        let config = UIImage.SymbolConfiguration(pointSize: 40, weight: .regular)
        imageView.image = UIImage(systemName: "checkmark.circle.fill", withConfiguration: config)
        imageView.tintColor = .systemGreen
        imageView.isHidden = true
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        
        Timer.scheduledTimer(withTimeInterval: 0.3, repeats: false) { _ in
            Task { @MainActor in
                await self.processShareContent()
            }
        }
    }
    
    // MARK: - Setup
    private func setupUI() {
        view.addSubview(backgroundView)
        view.addSubview(containerView)
        
        [loadingIndicator, statusLabel, successImageView].forEach {
            containerView.addSubview($0)
        }
        
        NSLayoutConstraint.activate([
            // Background view
            backgroundView.topAnchor.constraint(equalTo: view.topAnchor),
            backgroundView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            backgroundView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            backgroundView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            // Container view
            containerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            containerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            containerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            containerView.heightAnchor.constraint(equalToConstant: 180),
            
            // Loading indicator
            loadingIndicator.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
            loadingIndicator.centerYAnchor.constraint(equalTo: containerView.centerYAnchor, constant: -20),
            
            // Status label
            statusLabel.topAnchor.constraint(equalTo: loadingIndicator.bottomAnchor, constant: 16),
            statusLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 20),
            statusLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -20),
            
            // Success image view
            successImageView.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
            successImageView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor, constant: -20)
        ])
        
        animatePresentation()
    }
    
    private func animatePresentation() {
        containerView.transform = CGAffineTransform(translationX: 0, y: 200)
        backgroundView.alpha = 0
        
        UIView.animate(
            withDuration: 0.3,
            delay: 0,
            usingSpringWithDamping: 0.8,
            initialSpringVelocity: 0,
            options: .curveEaseOut
        ) {
            self.containerView.transform = .identity
            self.backgroundView.alpha = 1
        }
    }
    
    // MARK: - Content Processing
    private func processShareContent() async {
        guard let extensionItems = extensionContext?.inputItems as? [NSExtensionItem] else {
            showError()
            return
        }
        
        do {
            let sharedURL = try await extractSharedContent(from: extensionItems)
            let state = try await ComposeApp.ShareKt.collectionShare()
            
            // Convert KotlinBoolean to Swift Bool
            if state.boolValue {
                showSuccess()
            } else {
                showError()
            }
        } catch {
            print("Share error: \(error)")
            showError()
        }
    }
    
    private func extractSharedContent(from items: [NSExtensionItem]) async throws -> String? {
        for item in items {
            guard let attachments = item.attachments else { continue }
            
            for attachment in attachments {
                if let content = try await loadContent(from: attachment) {
                    return content
                }
            }
        }
        return nil
    }
    
    private func loadContent(from attachment: NSItemProvider) async throws -> String? {
        let typeIdentifiers = [
            ("public.url", { (item: NSSecureCoding?) -> String? in
                switch item {
                case let url as URL: return url.absoluteString
                case let urlString as String: return urlString
                default: return nil
                }
            }),
            ("public.text", { (item: NSSecureCoding?) -> String? in
                item as? String
            })
        ]
        
        for (identifier, transform) in typeIdentifiers {
            guard attachment.hasItemConformingToTypeIdentifier(identifier) else { continue }
            
            let item = try await attachment.loadItem(forTypeIdentifier: identifier)
            if let result = transform(item) {
                return result
            }
        }
        
        return nil
    }
    
    // MARK: - UI State Updates
    private func showSuccess() {
        UIView.animate(withDuration: 0.3, animations: {
            self.loadingIndicator.alpha = 0
            self.statusLabel.text = ComposeApp.ShareKt.getShareLabelText(key: "success")
        }) { _ in
            self.loadingIndicator.isHidden = true
            self.successImageView.isHidden = false
            self.animateSuccessIcon()
        }
    }
    
    private func animateSuccessIcon() {
        successImageView.transform = CGAffineTransform(scaleX: 0.1, y: 0.1)
        
        UIView.animate(
            withDuration: 0.5,
            delay: 0,
            usingSpringWithDamping: 0.5,
            initialSpringVelocity: 0,
            options: .curveEaseOut,
            animations: {
                self.successImageView.transform = .identity
            }
        ) { _ in
            UIView.animate(withDuration: 0, delay: 1.0, options: []) {
            } completion: { _ in
                self.dismissExtension()
            }
        }
    }
    
    private func showError() {
        statusLabel.text = ComposeApp.ShareKt.getShareLabelText(key: "failed")
        statusLabel.textColor = .systemRed
        loadingIndicator.stopAnimating()
        
        Timer.scheduledTimer(withTimeInterval: 2.0, repeats: false) { _ in
            self.dismissExtension()
        }
    }
    
    // MARK: - Dismissal
    @objc private func dismissExtension() {
        UIView.animate(withDuration: 0.3, animations: {
            self.containerView.transform = CGAffineTransform(translationX: 0, y: 200)
            self.backgroundView.alpha = 0
        }) { _ in
            self.extensionContext?.completeRequest(returningItems: nil)
        }
    }
}

// MARK: - NSItemProvider Extension
extension NSItemProvider {
    func loadItem(forTypeIdentifier typeIdentifier: String) async throws -> NSSecureCoding? {
        try await withCheckedThrowingContinuation { continuation in
            loadItem(forTypeIdentifier: typeIdentifier, options: nil) { item, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: item)
                }
            }
        }
    }
}
