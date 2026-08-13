package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.request.error
import com.github.panpf.sketch.request.placeholder
import com.slax.reader.const.BookmarkRoutes
import com.slax.reader.data.database.model.CollectionBookmarkItem
import com.slax.reader.data.database.model.SubscribedCollection
import com.slax.reader.data.database.model.isClosed
import com.slax.reader.data.database.model.isExpired
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.utils.i18n
import com.slax.reader.utils.parseInstantOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.global_default_avatar
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun CollectionFeedSwitcher(
    ownAvatar: String,
    collections: List<SubscribedCollection>,
    activeCollectionId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        item(key = "own-inbox") {
            FeedSource(
                imageUrl = ownAvatar,
                label = "collection_my_inbox".i18n(),
                active = activeCollectionId == null,
                hasNew = false,
                onClick = { onSelect(null) },
            )
        }
        items(collections, key = { collection -> collection.id }) { collection ->
            FeedSource(
                imageUrl = collection.avatar,
                label = collection.name,
                active = activeCollectionId == collection.id,
                hasNew = collection.hasNew,
                onClick = { onSelect(collection.id) },
            )
        }
    }
}

@Composable
private fun FeedSource(
    imageUrl: String,
    label: String,
    active: Boolean,
    hasNew: Boolean,
    onClick: () -> Unit,
) {
    val painter = rememberAsyncImagePainter(
        request = ComposableImageRequest(imageUrl.ifBlank { null }) {
            placeholder(Res.drawable.global_default_avatar)
            error(Res.drawable.global_default_avatar)
        }
    )
    val borderColor = if (active) Color(0xFF34C77B) else Color.Transparent

    Column(
        modifier = Modifier
            .width(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(46.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.5.dp, borderColor, CircleShape)
                    .padding(3.dp),
            ) {
                Image(
                    painter = painter,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }

            if (hasNew) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB45837))
                        .border(1.5.dp, Color(0xFFFCFCFC), CircleShape),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            style = TextStyle(
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                color = if (active) Color(0xFF0F1419) else Color(0xFF999999),
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun CollectionFeedContent(
    navCtrl: NavController,
    viewModel: InboxListViewModel,
    collection: SubscribedCollection?,
    bookmarks: List<CollectionBookmarkItem>,
    headerContent: (@Composable () -> Unit)? = null,
) {
    if (collection == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            headerContent?.invoke()
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("collection_unavailable".i18n(), color = Color(0xFF999999), fontSize = 14.sp)
            }
        }
        return
    }

    val closed = collection.isClosed
    val expired by key(collection.id, collection.subscriptionEndTime) {
        produceState(collection.isExpired()) {
            val endTime = parseInstantOrNull(collection.subscriptionEndTime)
            if (endTime == null) {
                value = true
                return@produceState
            }

            while (true) {
                val now = Clock.System.now()
                value = endTime <= now
                val delayMillis = if (value) {
                    EXPIRY_CLOCK_CHECK_INTERVAL_MS
                } else {
                    minOf((endTime - now).inWholeMilliseconds, EXPIRY_CLOCK_CHECK_INTERVAL_MS)
                }
                delay(delayMillis.coerceAtLeast(1L))
            }
        }
    }

    if (closed || expired) {
        Column(modifier = Modifier.fillMaxSize()) {
            headerContent?.invoke()
            Box(
                modifier = Modifier.weight(1f).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (closed) "collection_closed_message".i18n(collection.name)
                    else "collection_expired_message".i18n(collection.name),
                    color = Color(0xFF777777),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    } else if (bookmarks.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            headerContent?.invoke()
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("collection_empty".i18n(), color = Color(0xFF999999), fontSize = 14.sp)
            }
        }
    } else {
        val listState = rememberLazyListState()
        LaunchedEffect(viewModel, listState) {
            viewModel.scrollToTopEvent.collectLatest { listState.animateScrollToItem(0) }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(
                top = if (headerContent == null) 8.dp else 0.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
            ),
        ) {
            if (headerContent != null) {
                item(key = "collection-feed-switcher", contentType = "collection-feed-switcher") {
                    headerContent()
                }
            }

            item(key = "collection-owner-title", contentType = "collection-owner-title") {
                Text(
                    text = "collection_owner_feed_title".i18n(collection.name),
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 18.dp, top = 16.dp, bottom = 8.dp),
                    style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFF999999)),
                )
            }

            itemsIndexed(
                items = bookmarks,
                key = { _, item -> item.id },
                contentType = { _, _ -> "collection-bookmark" },
            ) { _, item ->
                BookmarkItemRow(
                    item = item,
                    onClick = {
                        navCtrl.navigate(
                            BookmarkRoutes(
                                bookmarkId = item.id,
                                collectionOwnerId = item.ownerId,
                                collectionId = collection.id,
                            )
                        )
                    },
                    viewModel = viewModel,
                    ownerActions = null,
                )
                DividerLine()
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "list_no_more".i18n(),
                        style = TextStyle(color = Color(0xFF999999), fontSize = 14.sp, lineHeight = 20.sp),
                    )
                }
            }
        }
    }
}

private const val EXPIRY_CLOCK_CHECK_INTERVAL_MS = 60_000L
