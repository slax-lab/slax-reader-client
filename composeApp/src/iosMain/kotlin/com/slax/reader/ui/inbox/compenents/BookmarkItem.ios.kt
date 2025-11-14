package com.slax.reader.ui.inbox.compenents

import androidx.compose.ui.window.ComposeUIViewController
import com.slax.reader.ui.bookmark.DetailScreen
import com.slax.reader.utils.NavigationHelper
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
actual fun navigateToDetail(bookmarkId: String, title: String) {
    val composeViewController = ComposeUIViewController {
        DetailScreen(bookmarkId = bookmarkId, onBackClick = {
            NavigationHelper.popViewController(animated = true)
        })
    }

    val containerViewController = object : UIViewController(nibName = null, bundle = null) {

        override fun prefersStatusBarHidden(): Boolean = true

        override fun preferredStatusBarUpdateAnimation(): UIStatusBarAnimation {
            return UIStatusBarAnimation.UIStatusBarAnimationSlide
        }

        override fun viewDidLoad() {
            super.viewDidLoad()

            addChildViewController(composeViewController)

            composeViewController.view.setFrame(view.bounds)
            composeViewController.view.autoresizingMask =
                UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight

            view.addSubview(composeViewController.view)

            composeViewController.didMoveToParentViewController(this)
        }

        override fun viewWillDisappear(animated: Boolean) {
            super.viewWillDisappear(animated)
            composeViewController.willMoveToParentViewController(null)
            composeViewController.removeFromParentViewController()
        }
    }

    containerViewController.edgesForExtendedLayout = UIRectEdgeAll
    containerViewController.extendedLayoutIncludesOpaqueBars = false
    containerViewController.title = ""

    containerViewController.view.backgroundColor = UIColor.whiteColor

    NavigationHelper.pushViewController(containerViewController, animated = false)
}