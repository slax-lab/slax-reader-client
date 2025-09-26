package com.slax.reader.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.navigation.NavController
import com.slax.reader.ui.home.HomeScreen.Companion.routes
import io.github.aakira.napier.Napier
import kotlinx.cinterop.*
import platform.Foundation.NSSelectorFromString
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN
import platform.objc.objc_setAssociatedObject


actual fun HomeScreen(): HomeScreen = object : HomeScreen {
    override fun onButtonClicked(buttonTitle: String, route: String, navController: NavController) {
        Napier.i("iOS user click $buttonTitle, then route to $route")
        super.onButtonClicked(buttonTitle, route, navController)
    }

    @Composable
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun Screen(navController: NavController) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val viewController = UIViewController()

                viewController.view.backgroundColor =
                    UIColor.colorWithRed(0.949, green = 0.949, blue = 0.969, alpha = 1.0)

                val stackView = UIStackView().apply {
                    axis = UILayoutConstraintAxisVertical
                    distribution = UIStackViewDistributionEqualSpacing
                    alignment = UIStackViewAlignmentFill
                    spacing = 20.0
                    translatesAutoresizingMaskIntoConstraints = false
                }

                val titleLabel = UILabel().apply {
                    text = "Slax Reader (Native iOS)"
                    font = UIFont.boldSystemFontOfSize(28.0)
                    textAlignment = NSTextAlignmentCenter
                    textColor = UIColor.labelColor
                }
                stackView.addArrangedSubview(titleLabel)

                routes.forEach { (title, route) ->
                    val button = UIButton.buttonWithType(UIButtonTypeSystem).apply {
                        setTitle(title, UIControlStateNormal)

                        val target = ButtonTarget {
                            onButtonClicked(title, route, navController)
                        }
                        addTarget(target, NSSelectorFromString("buttonTapped"), UIControlEventTouchUpInside)

                        // 保持target的引用，避免被垃圾回收
                        objc_setAssociatedObject(
                            this,
                            "target".cstr.getPointer(MemScope()),
                            target,
                            OBJC_ASSOCIATION_RETAIN
                        )
                    }

                    button.heightAnchor.constraintEqualToConstant(44.0).active = true
                    stackView.addArrangedSubview(button)
                }

                viewController.view.addSubview(stackView)

                NSLayoutConstraint.activateConstraints(
                    listOf(
                        stackView.centerXAnchor.constraintEqualToAnchor(viewController.view.centerXAnchor),
                        stackView.centerYAnchor.constraintEqualToAnchor(viewController.view.centerYAnchor),
                        stackView.widthAnchor.constraintEqualToAnchor(viewController.view.widthAnchor, multiplier = 0.8)
                    )
                )

                viewController.view
            }
        )
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class ButtonTarget(private val action: () -> Unit) : NSObject() {
    @ObjCAction
    fun buttonTapped() {
        action()
    }
}