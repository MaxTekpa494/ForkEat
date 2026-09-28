package fr.uge.android.forkeat.designsystem

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

// Utility composable to handle infinite scrolling in a LazyColumn or LazyRow.
@Composable
fun InfiniteListHandler(
    listState: LazyListState,
    isLoading: Boolean,
    buffer: Int = 1,
    onLoadMore: () -> Unit
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null &&
                lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - buffer
        }
    }
    LaunchedEffect(shouldLoadMore, isLoading) {
        if (shouldLoadMore && !isLoading) {
            onLoadMore()
        }
    }
}
