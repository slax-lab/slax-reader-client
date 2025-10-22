package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.bookmark.ToolbarIcon

/**
 * 支持左右滑动分页的工具栏容器
 */
@Composable
fun PagerToolbar(
    pages: List<List<ToolbarIcon>>,
    onIconClick: (pageIndex: Int, iconIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // println("[watch][UI] recomposition PagerToolbar")

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 页面内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(173.dp),
            verticalAlignment = Alignment.Top
        ) { page ->
            IconGridPage(
                icons = pages[page],
                onIconClick = { iconIndex -> onIconClick(page, iconIndex) }
            )
        }

        // 分页指示器
        PageIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(bottom = 34.dp)
        )
    }
}

@Composable
private fun IconGridPage(
    icons: List<ToolbarIcon>,
    onIconClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 上面一行4个icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            icons.take(4).forEachIndexed { index, icon ->
                IconButton(
                    icon = icon,
                    onClick = { onIconClick(index) }
                )
            }

            repeat(4 - icons.take(4).size) {
                Box(modifier = Modifier.width(56.dp))
            }
        }

        // 下面一行4个icon
        if (icons.size > 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                icons.drop(4).take(4).forEachIndexed { index, icon ->
                    IconButton(
                        icon = icon,
                        onClick = { onIconClick(index + 4) }
                    )
                }

                repeat(4 - icons.drop(4).take(4).size) {
                    Box(modifier = Modifier.width(56.dp))
                }
            }
        }
    }
}

