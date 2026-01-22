package com.slax.reader.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSThread
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
        dispatchMainIfNeeded {
            this.navigationController = navController
            gestureDelegate = PopGestureDelegate(navController)
            navController.interactivePopGestureRecognizer?.delegate = gestureDelegate
            navController.interactivePopGestureRecognizer?.setEnabled(true)
        }
    }

    fun pushViewController(viewController: UIViewController, animated: Boolean = true) {
        dispatchMainIfNeeded {
            val navController = navigationController ?: return@dispatchMainIfNeeded
            if (navController.topViewController?.transitionCoordinator != null) {
                return@dispatchMainIfNeeded
            }
            navController.pushViewController(viewController, animated)
        }
    }

    fun popViewController(animated: Boolean = true) {
        dispatchMainIfNeeded {
            val navController = navigationController ?: return@dispatchMainIfNeeded
            if (navController.topViewController?.transitionCoordinator != null) {
                return@dispatchMainIfNeeded
            }
            navController.popViewControllerAnimated(animated)
        }
    }

    private fun dispatchMainIfNeeded(task: () -> Unit) {
        if (NSThread.isMainThread) {
            task()
        } else {
            dispatch_async(dispatch_get_main_queue()) {
                task()
            }
        }
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