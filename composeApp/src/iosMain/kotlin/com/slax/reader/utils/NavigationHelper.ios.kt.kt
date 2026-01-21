package com.slax.reader.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController
import platform.UIKit.transitionCoordinator
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
object NavigationHelper {

    private var navigationController: UINavigationController? = null
    private var gestureDelegate: PopGestureDelegate? = null

    fun setNavigationController(navController: UINavigationController) {
        this.navigationController = navController
        gestureDelegate = PopGestureDelegate(navController)
        navController.interactivePopGestureRecognizer?.delegate = gestureDelegate
        navController.interactivePopGestureRecognizer?.setEnabled(true)
    }

    fun pushViewController(viewController: UIViewController, animated: Boolean = true) {
        val navController = navigationController ?: return
        if (navController.topViewController?.transitionCoordinator != null) {
            return
        }
        navController.pushViewController(viewController, animated)
        dispatch_async(dispatch_get_main_queue()) {}
    }

    fun popViewController(animated: Boolean = true) {
        val navController = navigationController ?: return
        if (navController.topViewController?.transitionCoordinator != null) {
            return
        }
        navController.popViewControllerAnimated(animated)
        dispatch_async(dispatch_get_main_queue()) {}
    }

}

@OptIn(ExperimentalForeignApi::class)
private class PopGestureDelegate(
    private val navigationController: UINavigationController
) : NSObject(), UIGestureRecognizerDelegateProtocol {

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean {
        return navigationController.viewControllers.size > 1
    }
}