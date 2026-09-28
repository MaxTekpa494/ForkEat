package fr.uge.android.forkeat.admin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Secondary900
import kotlin.math.abs

private const val REPEAT = 200

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    itemHeight: Dp = 48.dp,
    visibleItems: Int = 5,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (REPEAT / 2) * items.size + selectedIndex - visibleItems / 2
    )
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex + visibleItems / 2 }
    }

    LaunchedEffect(centerIndex) {
        onSelected(centerIndex % items.size)
    }

    Box(
        modifier = Modifier
            .height(itemHeight * visibleItems)
            .width(64.dp)
    ) {
        LazyColumn(state = listState, flingBehavior = fling) {
            items(items.size * REPEAT) { i ->
                val dist = abs(i - centerIndex)
                val alpha = when (dist) { 0 -> 1f; 1 -> 0.5f; else -> 0.2f }
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .width(64.dp)
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[i % items.size],
                        fontSize = if (dist == 0) 22.sp else 18.sp,
                        fontWeight = if (dist == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (dist == 0) Secondary900 else Gray500
                    )
                }
            }
        }

        // Ligne supérieure de la zone de sélection
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = itemHeight * (visibleItems / 2))
                .width(64.dp)
                .height(1.dp)
                .background(AdminPurple500.copy(alpha = 0.5f))
        )
        // Ligne inférieure de la zone de sélection
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = itemHeight * (visibleItems / 2 + 1))
                .width(64.dp)
                .height(1.dp)
                .background(AdminPurple500.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun WheelTimePicker(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
) {
    val hours = (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }

    val selectedHour = remember { mutableIntStateOf(initialHour) }
    val selectedMinute = remember { mutableIntStateOf(initialMinute) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelColumn(
            items = hours,
            selectedIndex = initialHour,
            onSelected = { h ->
                selectedHour.intValue = h
                onTimeSelected(selectedHour.intValue, selectedMinute.intValue)
            }
        )
        Spacer(Modifier.width(8.dp))
        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Secondary900)
        Spacer(Modifier.width(8.dp))
        WheelColumn(
            items = minutes,
            selectedIndex = initialMinute,
            onSelected = { m ->
                selectedMinute.intValue = m
                onTimeSelected(selectedHour.intValue, selectedMinute.intValue)
            }
        )
    }
}
