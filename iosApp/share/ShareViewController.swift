import UIKit
import Social
import ComposeApp

class ShareViewController: UIViewController {

    private var containerView: UIView!
    private var loadingIndicator: UIActivityIndicatorView!
    private var statusLabel: UILabel!
    private var successImageView: UIImageView!
    private var backgroundView: UIView!

    override func viewDidLoad() {
        super.viewDidLoad()

        setupCustomUI()

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            self.processShareContent()
        }
    }

    private func setupCustomUI() {
        backgroundView = UIView()
        backgroundView.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        backgroundView.translatesAutoresizingMaskIntoConstraints = false

        containerView = UIView()
        containerView.backgroundColor = .systemBackground
        containerView.layer.cornerRadius = 16
        containerView.layer.shadowColor = UIColor.black.cgColor
        containerView.layer.shadowOpacity = 0.1
        containerView.layer.shadowOffset = CGSize(width: 0, height: -2)
        containerView.layer.shadowRadius = 10
        containerView.translatesAutoresizingMaskIntoConstraints = false

        if #available(iOS 13.0, *) {
            loadingIndicator = UIActivityIndicatorView(style: .medium)
        } else {
            loadingIndicator = UIActivityIndicatorView(style: .gray)
        }
        loadingIndicator.startAnimating()
        loadingIndicator.translatesAutoresizingMaskIntoConstraints = false

        // 状态标签
        statusLabel = UILabel()
        statusLabel.text = ComposeApp.ShareKt.getShareLabelText(key: "collecting")
        statusLabel.textColor = .label
        statusLabel.font = UIFont.systemFont(ofSize: 16)
        statusLabel.textAlignment = .center
        statusLabel.translatesAutoresizingMaskIntoConstraints = false

        // 成功图标
        let checkmarkConfig = UIImage.SymbolConfiguration(pointSize: 40, weight: .regular)
        successImageView = UIImageView()
        if #available(iOS 13.0, *) {
            successImageView.image = UIImage(systemName: "checkmark.circle.fill", withConfiguration: checkmarkConfig)
            successImageView.tintColor = .systemGreen
        } else {
            successImageView.image = createCheckmarkImage()
        }
        successImageView.isHidden = true
        successImageView.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(backgroundView)
        view.addSubview(containerView)

        containerView.addSubview(loadingIndicator)
        containerView.addSubview(statusLabel)
        containerView.addSubview(successImageView)


        NSLayoutConstraint.activate([
                                        backgroundView.topAnchor.constraint(equalTo: view.topAnchor),
                                        backgroundView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
                                        backgroundView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
                                        backgroundView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

                                        containerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
                                        containerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
                                        containerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
                                        containerView.heightAnchor.constraint(equalToConstant: 180),

                                        loadingIndicator.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
                                        loadingIndicator.centerYAnchor.constraint(equalTo: containerView.centerYAnchor, constant: -20),

                                        statusLabel.topAnchor.constraint(equalTo: loadingIndicator.bottomAnchor, constant: 16),
                                        statusLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 20),
                                        statusLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -20),

                                        successImageView.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
                                        successImageView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor, constant: -20)
                                    ])

        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissExtension))
        backgroundView.addGestureRecognizer(tapGesture)

        containerView.transform = CGAffineTransform(translationX: 0, y: 200)
        backgroundView.alpha = 0
        UIView.animate(withDuration: 0.3, delay: 0, usingSpringWithDamping: 0.8, initialSpringVelocity: 0, options: .curveEaseOut, animations: {
            self.containerView.transform = .identity
            self.backgroundView.alpha = 1
        })
    }

    private func processShareContent() {
        guard let extensionItems = extensionContext?.inputItems as? [NSExtensionItem] else {
            showError()
            return
        }

        Task {
            do {
                var sharedURL: String? = nil

                for item in extensionItems {
                    if let attachments = item.attachments {
                        for attachment in attachments {
                            if attachment.hasItemConformingToTypeIdentifier("public.url") {
                                try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                                    attachment.loadItem(forTypeIdentifier: "public.url", options: nil) { (item, error) in
                                        if let error = error {
                                            continuation.resume(throwing: error)
                                        } else {
                                            if let url = item as? URL {
                                                sharedURL = url.absoluteString
                                            } else if let urlString = item as? String {
                                                sharedURL = urlString
                                            }
                                            continuation.resume(returning: ())
                                        }
                                    }
                                }
                                break
                            } else if attachment.hasItemConformingToTypeIdentifier("public.text") {
                                try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                                    attachment.loadItem(forTypeIdentifier: "public.text", options: nil) { (item, error) in
                                        if let error = error {
                                            continuation.resume(throwing: error)
                                        } else {
                                            if let text = item as? String {
                                                sharedURL = text
                                            }
                                            continuation.resume(returning: ())
                                        }
                                    }
                                }
                                break
                            }
                        }
                        if sharedURL != nil {
                            break
                        }
                    }
                }

                let state = try await ComposeApp.ShareKt.collectionShare()

                await MainActor.run {
                    if state == true {
                        showSuccess()
                    } else {
                        showError()
                    }
                }
            } catch {
                print("Share error: \(error)")
                await MainActor.run {
                    showError()
                }
            }
        }
    }

    private func showSuccess() {
        UIView.animate(withDuration: 0.3) {
            self.loadingIndicator.alpha = 0
            self.statusLabel.text = ComposeApp.ShareKt.getShareLabelText(key: "success")

        } completion: { _ in
            self.loadingIndicator.isHidden = true
            self.successImageView.isHidden = false

            self.successImageView.transform = CGAffineTransform(scaleX: 0.1, y: 0.1)
            UIView.animate(withDuration: 0.5, delay: 0, usingSpringWithDamping: 0.5, initialSpringVelocity: 0, options: .curveEaseOut, animations: {
                self.successImageView.transform = .identity
            }) { _ in
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    self.dismissExtension()
                }
            }
        }
    }

    private func showError() {
        statusLabel.text = ComposeApp.ShareKt.getShareLabelText(key: "failed")

        statusLabel.textColor = .systemRed
        loadingIndicator.stopAnimating()

        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            self.dismissExtension()
        }
    }

    @objc private func dismissExtension() {
        UIView.animate(withDuration: 0.3, animations: {
            self.containerView.transform = CGAffineTransform(translationX: 0, y: 200)
            self.backgroundView.alpha = 0
        }) { _ in
            self.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
        }
    }

    private func createCheckmarkImage() -> UIImage? {
        let size = CGSize(width: 40, height: 40)
        UIGraphicsBeginImageContextWithOptions(size, false, 0)

        let path = UIBezierPath(ovalIn: CGRect(origin: .zero, size: size))
        UIColor.systemGreen.setFill()
        path.fill()

        let checkPath = UIBezierPath()
        checkPath.lineWidth = 3
        checkPath.move(to: CGPoint(x: 12, y: 20))
        checkPath.addLine(to: CGPoint(x: 18, y: 26))
        checkPath.addLine(to: CGPoint(x: 28, y: 14))
        UIColor.white.setStroke()
        checkPath.stroke()

        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return image
    }
}
