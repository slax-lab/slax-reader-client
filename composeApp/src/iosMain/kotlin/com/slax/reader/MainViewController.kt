package com.slax.reader

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.rememberNavController
import com.slax.reader.di.configureKoin
import com.slax.reader.ui.SlaxNavigation
import com.slax.reader.utils.NavigationHelper
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.startKoin
import platform.UIKit.UIColor
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class, ExperimentalFoundationApi::class)
fun MainViewController(): UIViewController {
    ComposeFoundationFlags.isNewContextMenuEnabled = true

    startKoin {
        configureKoin()
    }

    val composeVC = ComposeUIViewController {
        val navController = rememberNavController()
        SlaxNavigation(navController)
    }

    composeVC.extendedLayoutIncludesOpaqueBars = false

    val navigationController = UINavigationController(rootViewController = composeVC)

    navigationController.view.backgroundColor = UIColor.whiteColor

    // 启用滑动返回手势
    navigationController.interactivePopGestureRecognizer?.delegate = null
    navigationController.interactivePopGestureRecognizer?.setEnabled(true)

    // 隐藏导航栏
    navigationController.setNavigationBarHidden(true, false)

    NavigationHelper.setNavigationController(navigationController)

    return navigationController
}