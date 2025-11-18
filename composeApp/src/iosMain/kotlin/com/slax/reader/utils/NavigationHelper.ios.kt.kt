package com.slax.reader.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
object NavigationHelper {

    private var navigationController: UINavigationController? = null

    fun setNavigationController(navController: UINavigationController) {
        this.navigationController = navController
    }

    fun pushViewController(viewController: UIViewController, animated: Boolean = true) {
        navigationController?.pushViewController(viewController, animated)
    }

    fun popViewController(animated: Boolean = true) {
        navigationController?.popViewControllerAnimated(animated)
    }

}