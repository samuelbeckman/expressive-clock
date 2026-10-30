package app.expressiveclock.widget

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ITEM_CORNER = 20.dp
private val CAROUSEL_EDGE = 24.dp
private val CAROUSEL_GAP = 12.dp

/**
 * Single-choice connected button group.
 */
@Composable
fun <T> OptionGroup(
    label: String,
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(horizontal = CAROUSEL_EDGE, vertical = 6.dp)) {
        PickerLabel(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            options.forEachIndexed { i, option ->
                val shapes = when {
                    options.size == 1 -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    i == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    i == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
                ToggleButton(
                    checked = option == value,
                    onCheckedChange = { if (it) onSelect(option) },
                    shapes = shapes,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text(
                        optionLabel(option),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * A robust horizontally scrolling carousel of option thumbnails using LazyRow.
 */
@Composable
fun <T> OptionCarousel(
    label: String,
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    itemSize: Dp = 76.dp,
    itemLabels: Boolean = true,
    onSelect: (T) -> Unit,
    thumbnail: @Composable (T, Boolean) -> Unit,
) {
    val selectedIndex = options.indexOf(value).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex < options.size) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column(Modifier.padding(vertical = 6.dp)) {
        PickerLabel(label, Modifier.padding(start = CAROUSEL_EDGE))
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(CAROUSEL_GAP),
            contentPadding = PaddingValues(horizontal = CAROUSEL_EDGE),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(options) { option ->
                val selected = option == value
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(itemSize),
                ) {
                    Box(
                        Modifier
                            .size(itemSize)
                            .clip(RoundedCornerShape(ITEM_CORNER))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        thumbnail(option, selected)
                    }
                    if (itemLabels) {
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(itemSize)
                                .padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

/** A bitmap thumbnail, scaled into the carousel cell. */
@Composable
fun ThumbnailImage(bitmap: Bitmap, description: String?) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
    )
}
