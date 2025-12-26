package com.slax.reader.const

val localeString: Map<String, Map<String, String>> = mapOf(
    // ========================================
    // 示例（保留）
    // ========================================
    //    "welcome_message" to mapOf(
    //        "zh" to "欢迎，{0}！",
    //        "en" to "Welcome, {0}!"
    //    ),
    //    "items_count" to mapOf(
    //        "zh" to "您有 {0} 个项目。",
    //        "en" to "You have {0} items."
    //    ),
    //    "user_greeting" to mapOf(
    //        "zh" to "你好，{name}，今天是 {day}。",
    //        "en" to "Hello, {name}, today is {day}."
    //    ),

    // 错误提示
    "login_failed" to mapOf(
        "zh" to "登录失败",
        "en" to "Login Failed"
    ),
    "btn_ok" to mapOf(
        "zh" to "确定",
        "en" to "OK"
    ),

    // 欢迎页面
    "login_welcome_title" to mapOf(
        "zh" to "欢迎来到 \nSlax Reader",
        "en" to "Welcome to \nSlax Reader"
    ),
    "login_welcome_subtitle" to mapOf(
        "zh" to "Read Smarter\nConnect Deeper",
        "en" to "Read Smarter\nConnect Deeper"
    ),

    // 登录按钮
    "login_google" to mapOf(
        "zh" to "Google 登录",
        "en" to "Sign in with Google"
    ),
    "login_apple" to mapOf(
        "zh" to "通过 Apple 登录",
        "en" to "Sign in with Apple"
    ),

    // 用户协议相关
    "agreement_prefix" to mapOf(
        "zh" to "阅读并同意",
        "en" to "Read and agree to "
    ),
    "agreement_user_agreement" to mapOf(
        "zh" to "《用户协议》",
        "en" to "Terms of Service"
    ),
    "agreement_and" to mapOf(
        "zh" to "和",
        "en" to " and "
    ),
    "agreement_privacy_policy" to mapOf(
        "zh" to "《隐私政策》",
        "en" to "Privacy Policy"
    ),

    // 协议弹窗
    "agreement_tab_terms" to mapOf(
        "zh" to "用户协议",
        "en" to "Terms of Service"
    ),
    "agreement_tab_privacy" to mapOf(
        "zh" to "隐私政策",
        "en" to "Privacy Policy"
    ),
    "agreement_btn_agree" to mapOf(
        "zh" to "同意",
        "en" to "Agree"
    ),
    "agreement_btn_disagree" to mapOf(
        "zh" to "不同意",
        "en" to "Disagree"
    ),

    // ========================================
    // 主页（Inbox）
    // ========================================
    "app_name" to mapOf(
        "zh" to "Slax Reader",
        "en" to "Slax Reader"
    ),
    "inbox_title" to mapOf(
        "zh" to "Inbox",
        "en" to "Inbox"
    ),

    // 添加链接对话框
    "add_link_dialog_title" to mapOf(
        "zh" to "添加链接",
        "en" to "Add Link"
    ),
    "add_link_placeholder" to mapOf(
        "zh" to "https://…",
        "en" to "https://…"
    ),
    "add_link_btn_submit" to mapOf(
        "zh" to "添加收藏",
        "en" to "Add Bookmark"
    ),
    "add_link_error_invalid_url" to mapOf(
        "zh" to "请输入有效的网址链接",
        "en" to "Please enter a valid URL"
    ),
    "add_link_error_no_clipboard" to mapOf(
        "zh" to "剪贴板中没有有效的链接",
        "en" to "No valid link in clipboard"
    ),
    "add_link_error_multiple_links" to mapOf(
        "zh" to "剪贴板发现多个链接，请手动处理",
        "en" to "Multiple links found in clipboard, please handle manually"
    ),

    // 处理中对话框
    "processing_message" to mapOf(
        "zh" to "快照将在几分钟内准备就绪。在此期间，您可以阅读原文。",
        "en" to "Snapshot will be ready in a few minutes. In the meantime, you can read the original."
    ),
    "processing_btn_read_original" to mapOf(
        "zh" to "阅读原文",
        "en" to "Read Original"
    ),

    // 继续阅读
    "continue_reading_desc" to mapOf(
        "zh" to "继续阅读",
        "en" to "Continue Reading"
    ),
    "continue_reading_close_desc" to mapOf(
        "zh" to "关闭",
        "en" to "Close"
    ),

    // ========================================
    // 设置页面
    // ========================================
    "setting_title" to mapOf(
        "zh" to "设置",
        "en" to "Settings"
    ),
    "setting_delete_account" to mapOf(
        "zh" to "删除账号",
        "en" to "Delete Account"
    ),
    "setting_language" to mapOf(
        "zh" to "语言",
        "en" to "Language"
    ),
    "language_chinese" to mapOf(
        "zh" to "简体中文",
        "en" to "Simplified Chinese"
    ),
    "language_english" to mapOf(
        "zh" to "English",
        "en" to "English"
    ),

    // ========================================
    // 通用按钮
    // ========================================
    "btn_back" to mapOf(
        "zh" to "返回",
        "en" to "Back"
    ),
    "btn_close" to mapOf(
        "zh" to "关闭",
        "en" to "Close"
    ),

    // ========================================
    // Sidebar（侧边栏）
    // ========================================
    "sidebar_close" to mapOf(
        "zh" to "关闭",
        "en" to "Close"
    ),
    "sidebar_user_avatar" to mapOf(
        "zh" to "用户头像",
        "en" to "User Avatar"
    ),
    "sidebar_subscription" to mapOf(
        "zh" to "会员订阅",
        "en" to "Subscription"
    ),
    "sidebar_settings" to mapOf(
        "zh" to "设置",
        "en" to "Settings"
    ),
    "sidebar_about" to mapOf(
        "zh" to "关于",
        "en" to "About"
    ),
    "sidebar_logout" to mapOf(
        "zh" to "退出登录",
        "en" to "Logout"
    ),

    // ========================================
    // 同步状态
    // ========================================
    "sync_uploading" to mapOf(
        "zh" to "上传中",
        "en" to "Uploading"
    ),
    "sync_downloading" to mapOf(
        "zh" to "下载中",
        "en" to "Downloading"
    ),
    "sync_no_network" to mapOf(
        "zh" to "无法连接网络",
        "en" to "Unable to connect to network"
    ),
    "sync_connecting" to mapOf(
        "zh" to "连接中",
        "en" to "Connecting"
    ),

    // ========================================
    // BookmarkItem（书签项菜单）
    // ========================================
    "bookmark_star" to mapOf(
        "zh" to "加星",
        "en" to "Star"
    ),
    "bookmark_archive" to mapOf(
        "zh" to "归档",
        "en" to "Archive"
    ),
    "bookmark_edit_title" to mapOf(
        "zh" to "修改标题",
        "en" to "Edit Title"
    ),
    "bookmark_delete" to mapOf(
        "zh" to "删除",
        "en" to "Delete"
    ),

    // ========================================
    // 关于页面
    // ========================================
    "about_version" to mapOf(
        "zh" to "版本",
        "en" to "Version"
    ),

    // ========================================
    // DetailScreen（书签详情页）
    // ========================================
    // 底部工具栏
    "detail_toolbar_summary" to mapOf(
        "zh" to "提纲",
        "en" to "Outline"
    ),
    "detail_toolbar_star" to mapOf(
        "zh" to "加星",
        "en" to "Star"
    ),
    "detail_toolbar_archive" to mapOf(
        "zh" to "归档",
        "en" to "Archive"
    ),
    "detail_toolbar_edit_title" to mapOf(
        "zh" to "改标题",
        "en" to "Edit Title"
    ),

    // 标签管理
    "tags_title" to mapOf(
        "zh" to "标签",
        "en" to "Tags"
    ),
    "tags_added" to mapOf(
        "zh" to "已添加",
        "en" to "Added"
    ),
    "tags_available" to mapOf(
        "zh" to "可添加",
        "en" to "Available"
    ),
    "tags_create_new" to mapOf(
        "zh" to "创建新标签",
        "en" to "Create New Tag"
    ),
    "tags_create_title" to mapOf(
        "zh" to "创建标签",
        "en" to "Create Tag"
    ),
    "tags_input_placeholder" to mapOf(
        "zh" to "输入标签名称",
        "en" to "Enter tag name"
    ),
    "tags_create_prefix" to mapOf(
        "zh" to "创建：",
        "en" to "Create: "
    ),

    // Outline（大纲）
    "outline_collapse" to mapOf(
        "zh" to "收缩",
        "en" to "Collapse"
    ),
    "outline_expand" to mapOf(
        "zh" to "展开",
        "en" to "Expand"
    ),
    "outline_completed" to mapOf(
        "zh" to "已完成",
        "en" to "Completed"
    ),
    "outline_summarizing" to mapOf(
        "zh" to "总结中",
        "en" to "Summarizing"
    ),
    "outline_completed_prefix" to mapOf(
        "zh" to "已完成：",
        "en" to "Completed: "
    ),
    "outline_summarizing_prefix" to mapOf(
        "zh" to "总结中：",
        "en" to "Summarizing: "
    ),
    "outline_empty" to mapOf(
        "zh" to "暂无内容",
        "en" to "No content"
    ),
    "outline_error" to mapOf(
        "zh" to "加载失败",
        "en" to "Load failed"
    ),

    // 通用按钮
    "btn_cancel" to mapOf(
        "zh" to "取消",
        "en" to "Cancel"
    ),
    "btn_confirm" to mapOf(
        "zh" to "确定",
        "en" to "Confirm"
    ),

    // ========================================
    // 外部链接警告对话框（iOS）
    // ========================================
    "external_link_alert_title" to mapOf(
        "zh" to "你即将跳转到第三方页面",
        "en" to "You are about to visit a third-party page"
    ),
    "external_link_alert_message" to mapOf(
        "zh" to "是否确认在浏览器中打开此链接？\n",
        "en" to "Do you want to open this link in the browser?\n"
    ),
    "external_link_do_not_alert" to mapOf(
        "zh" to " 不再提示",
        "en" to " Don't show again"
    ),

    // ========================================
    // 删除账号页面
    // ========================================
    "delete_account_title" to mapOf(
        "zh" to "删除账号",
        "en" to "Delete Account"
    ),
    "delete_account_button" to mapOf(
        "zh" to "删除账号",
        "en" to "Delete Account"
    ),
    "delete_account_confirm_title" to mapOf(
        "zh" to "确认删除账号",
        "en" to "Confirm Delete Account"
    ),
    "delete_account_confirm_message" to mapOf(
        "zh" to "删除账号后，您的所有数据将被永久删除且无法恢复。确定要继续吗？",
        "en" to "After deleting your account, all your data will be permanently deleted and cannot be recovered. Are you sure you want to continue?"
    ),
    "delete_account_confirm_button" to mapOf(
        "zh" to "确认删除",
        "en" to "Confirm Delete"
    ),
    "delete_account_error_title" to mapOf(
        "zh" to "删除失败",
        "en" to "Delete Failed"
    ),

    // ========================================
    // 主页列表
    // ========================================
    "list_no_more" to mapOf(
        "zh" to "没有更多了",
        "en" to "No more items"
    ),

    // ========================================
    // DetailScreen - 查看原网页
    // ========================================
    "detail_view_original" to mapOf(
        "zh" to "查看原网页",
        "en" to "View Original"
    ),

    // ========================================
    // 修改标题对话框
    // ========================================
    "edit_title_dialog_title" to mapOf(
        "zh" to "修改标题",
        "en" to "Edit Title"
    ),

    // ========================================
    // 全文概要
    // ========================================
    "overview_prefix" to mapOf(
        "zh" to "全文概要: ",
        "en" to "Overview: "
    ),
    "overview_expand_all" to mapOf(
        "zh" to "展开全部",
        "en" to "Expand All"
    ),
    "overview_dialog_title" to mapOf(
        "zh" to "全文概要：",
        "en" to "Full Overview:"
    ),
    "load_bookmark_content_error" to mapOf(
        "zh" to "加载书签内容失败",
        "en" to "Failed to load bookmark content"
    ),

    // ========================================
    // 订阅提示
    // ========================================
    "subscription_required_title" to mapOf(
        "zh" to "需要订阅",
        "en" to "Subscription Required"
    ),
    "subscription_required_message" to mapOf(
        "zh" to "该功能需要订阅后才能使用，请前往网页端进行订阅",
        "en" to "This feature requires a subscription. Please subscribe via web"
    ),
    "subscription_required_btn_ok" to mapOf(
        "zh" to "我知道了",
        "en" to "Got it"
    )
)