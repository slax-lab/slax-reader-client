package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.utils.i18n
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.global_default_avatar
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun CollectionFeedSwitcher(
    ownAvatar: String,
    collections: List<SubscribedCollection>,
    activeOwnerId: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "own-inbox") {
            FeedSource(
                imageUrl = ownAvatar,
                label = "collection_my_inbox".i18n(),
                active = activeOwnerId == null,
                hasNew = false,
                onClick = { onSelect(null) },
            )
        }
        items(collections, key = { collection -> collection.ownerId }) { collection ->
            FeedSource(
                imageUrl = collection.avatar,
                label = collection.name,
                active = activeOwnerId == collection.ownerId,
                hasNew = collection.hasNew,
                onClick = { onSelect(collection.ownerId) },
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
    val borderColor = if (active) Color(0xFFB45837) else Color.Transparent

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(3.dp),
    ) {
        Image(
            painter = painter,
            contentDescription = label,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        if (hasNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB45837))
                    .border(1.dp, Color(0xFFFCFCFC), CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun CollectionFeedContent(
    navCtrl: NavController,
    viewModel: InboxListViewModel,
    collection: SubscribedCollection?,
    bookmarks: List<CollectionBookmarkItem>,
    headerContent: @Composable () -> Unit,
) {
    if (collection == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            headerContent()
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("collection_unavailable".i18n(), color = Color(0xFF999999), fontSize = 14.sp)
            }
        }
        return
    }

    val now = Clock.System.now()
    val endTime = remember(collection.subscriptionEndTime) {
        runCatching { Instant.parse(collection.subscriptionEndTime) }.getOrNull()
    }
    val expired = endTime?.let { it < now } == true
    val closed = collection.status != 1

    if (closed || expired) {
        Column(modifier = Modifier.fillMaxSize()) {
            headerContent()
            Box(modifier = Modifier.weight(1f).padding(32.dp), contentAlignment = Alignment.Center) {
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
            headerContent()
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("collection_empty".i18n(), color = Color(0xFF999999), fontSize = 14.sp)
            }
        }
    } else {
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(viewModel, listState) {
            viewModel.scrollToTopEvent.collectLatest { listState.animateScrollToItem(0) }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            item(key = "collection-feed-switcher", contentType = "collection-feed-switcher") {
                headerContent()
            }
            itemsIndexed(
                items = bookmarks,
                key = { _, item -> item.id },
                contentType = { _, _ -> "collection-bookmark" },
            ) { index, item ->
                CollectionBookmarkRow(
                    index = index,
                    item = item,
                    authorAvatar = collection.avatar,
                    onClick = {
                        navCtrl.navigate(
                            BookmarkRoutes(
                                bookmarkId = item.id,
                                collectionOwnerId = item.ownerId,
                                collectionId = collection.id,
                            )
                        )
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color(0x12000000),
                )
            }
        }
    }
}

@Composable
private fun CollectionBookmarkRow(
    index: Int,
    item: CollectionBookmarkItem,
    authorAvatar: String,
    onClick: () -> Unit,
) {
    val highlightText = remember(item.firstMarkContent) { extractMarkText(item.firstMarkContent) }
    val avatarPainter = rememberAsyncImagePainter(
        request = ComposableImageRequest(authorAvatar.ifBlank { null }) {
            placeholder(Res.drawable.global_default_avatar)
            error(Res.drawable.global_default_avatar)
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = (index + 1).toString(),
            modifier = Modifier.width(26.dp),
            color = Color(0xFF999999),
            fontSize = 12.sp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
                color = Color(0xFF1A1814),
            )
            if (item.source.isNotBlank() || item.markCount > 0) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (item.source.isNotBlank()) {
                        Text(item.source, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = Color(0xFF8A8177))
                    }
                    if (item.markCount > 0) {
                        Text("collection_highlight_count".i18n(item.markCount), fontSize = 12.sp, color = Color(0xFFB45837))
                    }
                }
            }
            if (highlightText.isNotBlank() || item.firstMarkComment.isNotBlank()) {
                Spacer(modifier = Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0x0FB45837), RoundedCornerShape(8.dp)).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Image(
                        painter = avatarPainter,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (highlightText.isNotBlank()) {
                            Text(highlightText, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF6A5F52))
                        }
                        if (item.firstMarkComment.isNotBlank() && item.firstMarkComment != highlightText) {
                            if (highlightText.isNotBlank()) Spacer(modifier = Modifier.height(4.dp))
                            Text(item.firstMarkComment, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF1A1814))
                        }
                    }
                }
            }
        }
    }
}

private fun extractMarkText(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching {
        Json.parseToJsonElement(raw).jsonArray.joinToString("") { element ->
            element.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
        }.trim()
    }.getOrElse { raw.trim() }
}
