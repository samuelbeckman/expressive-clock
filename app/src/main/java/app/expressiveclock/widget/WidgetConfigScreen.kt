package app.expressiveclock.widget

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import app.expressiveclock.R
import app.expressiveclock.data.ClockSettings
import app.expressiveclock.data.ColourSource
import app.expressiveclock.data.DateMode
import app.expressiveclock.data.DigitalBackground
import app.expressiveclock.data.DigitalLayout
import app.expressiveclock.data.DigitalTypefaceWeight
import app.expressiveclock.data.FaceFill
import app.expressiveclock.data.FaceShape
import app.expressiveclock.data.HandStyle
import app.expressiveclock.data.IndexSet
import app.expressiveclock.data.MinorIndex
import app.expressiveclock.data.NumeralLayout
import app.expressiveclock.data.NumeralSize
import app.expressiveclock.data.NumeralSystem
import app.expressiveclock.data.NumeralWidth
import app.expressiveclock.data.OutlineStyle
import app.expressiveclock.data.WidgetColorSwatch
import app.expressiveclock.data.WidgetConfig
import app.expressiveclock.data.WidgetFontFamily
import app.expressiveclock.data.WidgetType
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CURATED_SHAPES = listOf(
    FaceShape.CIRCLE,
    FaceShape.ROUNDED_SQUARE,
)

private val CURATED_HANDS = listOf(
    HandStyle.BATON,
    HandStyle.NEEDLE,
    HandStyle.LEAF,
    HandStyle.TAPER,
    HandStyle.ARROW,
)

private enum class DigitalLabelState {
    NONE,
    DAY_ONLY,
    DATE_ONLY,
    BOTH,
}

@Composable
fun WidgetFontFamily.toComposeFontFamily(weight: Int = 400): FontFamily = when (this) {
    WidgetFontFamily.GOOGLE_SANS -> FontFamily(
        Font(
            resId = R.font.google_sans_flex,
            weight = FontWeight(weight.coerceIn(1, 1000)),
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("opsz", 144f),
                FontVariation.Setting("wdth", 25f),
                FontVariation.Setting("wght", weight.coerceIn(1, 1000).toFloat()),
            ),
        )
    )
    WidgetFontFamily.GOOGLE_SANS_NORMAL -> FontFamily(
        Font(
            resId = R.font.google_sans_flex,
            weight = FontWeight(weight.coerceIn(1, 1000)),
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("opsz", 144f),
                FontVariation.Setting("wdth", 100f),
                FontVariation.Setting("wght", weight.coerceIn(1, 1000).toFloat()),
            ),
        )
    )
    WidgetFontFamily.SERIF -> FontFamily.Serif
    WidgetFontFamily.MONOSPACE -> FontFamily.Monospace
    WidgetFontFamily.CURSIVE -> FontFamily.Cursive
}

/**
 * The refined Widget Customisation Screen matching the visual mockups.
 */
@Composable
fun WidgetConfigScreen(
    initial: WidgetConfig,
    settings: ClockSettings,
    isNew: Boolean = true,
    onCancel: () -> Unit,
    onSave: (WidgetConfig) -> Unit,
) {
    var config by remember(initial) { mutableStateOf(initial) }
    val mainScrollState = rememberScrollState()

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Header Bar with Back arrow, Title, Trash icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Edit widget",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                IconButton(onClick = onCancel) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete widget")
                }
            }

            // Animated Type Segmented Switcher: [ Analog | Digital ]
            val isDigital = config.type == WidgetType.DIGITAL
            val alignmentBias by animateFloatAsState(
                targetValue = if (isDigital) 1f else -1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                label = "typeBias",
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp),
            ) {
                // Sliding selection pill background
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxSize()
                        .align(BiasAlignment(alignmentBias, 0f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2170E8)),
                )

                // Foreground clickable labels
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { config = config.copy(type = WidgetType.ANALOG) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Analog",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = if (!isDigital) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { config = config.copy(type = WidgetType.DIGITAL) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Digital",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = if (isDigital) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Prominent Live Preview Card
            AnimatedContent(
                targetState = config.type,
                transitionSpec = {
                    (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(200)))
                },
                label = "previewTransition",
            ) { targetType ->
                if (targetType == WidgetType.DIGITAL) {
                    DigitalPreview(config, settings, Modifier.padding(horizontal = 24.dp))
                } else {
                    AnalogPreview(config, settings, Modifier.padding(horizontal = 24.dp))
                }
            }

            // Customisation Controls Scroll View with top and bottom progressive fade overlays
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(mainScrollState),
                ) {
                    Spacer(Modifier.height(8.dp))
                    AnimatedContent(
                        targetState = config.type,
                        transitionSpec = {
                            fadeIn(tween(200)).togetherWith(fadeOut(tween(150)))
                        },
                        label = "controlsTransition",
                    ) { targetType ->
                        if (targetType == WidgetType.DIGITAL) {
                            DigitalControls(config, settings, mainScrollState) { updated -> config = updated }
                        } else {
                            AnalogControls(config, settings) { updated -> config = updated }
                        }
                    }
                    Spacer(Modifier.height(36.dp))
                }

                // Top progressive fade overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                ),
                            ),
                        ),
                )

                // Bottom progressive fade overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.surface,
                                ),
                            ),
                        ),
                )
            }

            // Primary Save Pill Button
            Button(
                onClick = { onSave(config) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2170E8)),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .height(54.dp),
            ) {
                Text(
                    text = if (isNew) "Add widget" else "Save",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun TypefaceChooserCardRow(
    value: WidgetFontFamily,
    onSelect: (WidgetFontFamily) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        items(WidgetFontFamily.entries, key = { it.name }) { fontOption ->
            val selected = fontOption == value
            val fontFam = fontOption.toComposeFontFamily()

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Color(0xFF2170E8) else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .width(108.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(fontOption) },
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "12:45",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = fontFam,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = if (selected) Color(0xFF2170E8) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = fontOption.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalControls(
    config: WidgetConfig,
    settings: ClockSettings,
    mainScrollState: ScrollState,
    onUpdate: (WidgetConfig) -> Unit,
) {
    var showAdvancedColors by remember { mutableStateOf(false) }
    var showCustomColorPickerTarget by remember { mutableStateOf<String?>(null) }

    val availableWeights = remember(config.digitalFont) {
        when (config.digitalFont) {
            WidgetFontFamily.GOOGLE_SANS, WidgetFontFamily.GOOGLE_SANS_NORMAL -> listOf(
                DigitalTypefaceWeight.THIN,
                DigitalTypefaceWeight.REGULAR,
                DigitalTypefaceWeight.MEDIUM,
                DigitalTypefaceWeight.BOLD,
            )
            WidgetFontFamily.SERIF, WidgetFontFamily.MONOSPACE -> listOf(
                DigitalTypefaceWeight.REGULAR,
                DigitalTypefaceWeight.BOLD,
            )
            WidgetFontFamily.CURSIVE -> listOf(
                DigitalTypefaceWeight.REGULAR,
            )
        }
    }

    val currentWeight = if (config.digitalWeight in availableWeights) config.digitalWeight else availableWeights.first()
    val selectableLayouts = remember {
        listOf(DigitalLayout.STACKED, DigitalLayout.HORIZONTAL, DigitalLayout.COMPACT)
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (showAdvancedColors) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chevronRotation",
    )

    LaunchedEffect(showAdvancedColors) {
        if (showAdvancedColors) {
            delay(120L)
            mainScrollState.animateScrollTo(mainScrollState.maxValue)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Layout")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            selectableLayouts.forEach { layout ->
                val selected = config.digitalLayout == layout
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF2170E8) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onUpdate(config.copy(digitalLayout = layout)) },
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = when (layout) {
                                DigitalLayout.STACKED -> "08 30"
                                DigitalLayout.HORIZONTAL -> "08:30"
                                DigitalLayout.COMPACT -> "08:30"
                                else -> "08:30"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) Color(0xFF2170E8) else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = layout.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        SectionHeader("Typeface")
        TypefaceChooserCardRow(
            value = config.digitalFont,
            onSelect = { newFont ->
                val newWeights = when (newFont) {
                    WidgetFontFamily.GOOGLE_SANS, WidgetFontFamily.GOOGLE_SANS_NORMAL -> listOf(
                        DigitalTypefaceWeight.THIN,
                        DigitalTypefaceWeight.REGULAR,
                        DigitalTypefaceWeight.MEDIUM,
                        DigitalTypefaceWeight.BOLD,
                    )
                    WidgetFontFamily.SERIF, WidgetFontFamily.MONOSPACE -> listOf(
                        DigitalTypefaceWeight.REGULAR,
                        DigitalTypefaceWeight.BOLD,
                    )
                    WidgetFontFamily.CURSIVE -> listOf(
                        DigitalTypefaceWeight.REGULAR,
                    )
                }
                val safeWeight = if (config.digitalWeight in newWeights) config.digitalWeight else newWeights.first()
                onUpdate(config.copy(digitalFont = newFont, digitalWeight = safeWeight))
            },
        )

        SectionHeader("Weight")
        Box(modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) {
            OptionGroupSimple(
                value = currentWeight,
                options = availableWeights,
                optionLabel = { it.label },
                onSelect = { onUpdate(config.copy(digitalWeight = it)) },
            )
        }

        SectionHeader("Background")
        OptionGroupSimple(
            value = config.digitalBackground,
            options = listOf(DigitalBackground.NONE, DigitalBackground.FILLED),
            optionLabel = {
                when (it) {
                    DigitalBackground.NONE -> "None"
                    else -> "Filled"
                }
            },
            onSelect = { onUpdate(config.copy(digitalBackground = it)) },
        )

        ControlSwitchRow(
            title = "Date",
            checked = config.showDate,
            onChange = { onUpdate(config.copy(showDate = it)) },
        )

        ControlSwitchRow(
            title = "Day",
            checked = config.showDayOfWeek,
            onChange = { onUpdate(config.copy(showDayOfWeek = it)) },
        )

        // Simple Mode: Unified Colour for all 5 elements
        SectionHeader("Colour")
        ColourSwatchRowWithCustom(
            selectedColor = config.digitalHourColor,
            onSelectColor = { colorArgb ->
                onUpdate(
                    config.copy(
                        digitalHourColor = colorArgb,
                        digitalColonColor = colorArgb,
                        digitalMinuteColor = colorArgb,
                        digitalDayColor = colorArgb,
                        digitalDateColor = colorArgb,
                    ),
                )
            },
            onOpenCustomPicker = { showCustomColorPickerTarget = "ALL" },
        )

        // Expanding Toggle Button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { showAdvancedColors = !showAdvancedColors },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Advanced colours",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (showAdvancedColors) "Collapse" else "Expand",
                    tint = Color(0xFF2170E8),
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                )
            }
        }

        // Inline Expanding Advanced Colours Section
        AnimatedVisibility(
            visible = showAdvancedColors,
            enter = fadeIn(tween(250, easing = FastOutSlowInEasing)) +
                    expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                   shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Hours colour")
                ColourSwatchRowWithCustom(
                    selectedColor = config.digitalHourColor,
                    onSelectColor = { onUpdate(config.copy(digitalHourColor = it)) },
                    onOpenCustomPicker = { showCustomColorPickerTarget = "HOUR" },
                )

                SectionHeader("Colon colour")
                ColourSwatchRowWithCustom(
                    selectedColor = config.digitalColonColor,
                    onSelectColor = { onUpdate(config.copy(digitalColonColor = it)) },
                    onOpenCustomPicker = { showCustomColorPickerTarget = "COLON" },
                )

                SectionHeader("Minutes colour")
                ColourSwatchRowWithCustom(
                    selectedColor = config.digitalMinuteColor,
                    onSelectColor = { onUpdate(config.copy(digitalMinuteColor = it)) },
                    onOpenCustomPicker = { showCustomColorPickerTarget = "MIN" },
                )

                SectionHeader("Day colour")
                ColourSwatchRowWithCustom(
                    selectedColor = config.digitalDayColor,
                    onSelectColor = { onUpdate(config.copy(digitalDayColor = it)) },
                    onOpenCustomPicker = { showCustomColorPickerTarget = "DAY" },
                )

                SectionHeader("Date colour")
                ColourSwatchRowWithCustom(
                    selectedColor = config.digitalDateColor,
                    onSelectColor = { onUpdate(config.copy(digitalDateColor = it)) },
                    onOpenCustomPicker = { showCustomColorPickerTarget = "DATE" },
                )
            }
        }
    }

    if (showCustomColorPickerTarget != null) {
        val initialColorLong = when (showCustomColorPickerTarget) {
            "HOUR" -> config.digitalHourColor
            "COLON" -> config.digitalColonColor
            "MIN" -> config.digitalMinuteColor
            "DAY" -> config.digitalDayColor
            "DATE" -> config.digitalDateColor
            "ANALOG_SECOND" -> config.analogSecondHandColor
            "ANALOG_MAIN" -> config.analogPrimaryColor ?: if (config.colour == ColourSource.PALETTE) config.swatch.colorArgb else null
            else -> config.digitalHourColor
        } ?: WidgetColorSwatch.BLUE.colorArgb

        CustomColorPickerDialog(
            initialColor = Color(initialColorLong),
            onDismiss = { showCustomColorPickerTarget = null },
            onColorConfirmed = { chosenColor ->
                val argb = chosenColor.toArgb().toLong() and 0xFFFFFFFFL
                when (showCustomColorPickerTarget) {
                    "ALL" -> onUpdate(
                        config.copy(
                            digitalHourColor = argb,
                            digitalColonColor = argb,
                            digitalMinuteColor = argb,
                            digitalDayColor = argb,
                            digitalDateColor = argb,
                        ),
                    )
                    "HOUR" -> onUpdate(config.copy(digitalHourColor = argb))
                    "COLON" -> onUpdate(config.copy(digitalColonColor = argb))
                    "MIN" -> onUpdate(config.copy(digitalMinuteColor = argb))
                    "DAY" -> onUpdate(config.copy(digitalDayColor = argb))
                    "DATE" -> onUpdate(config.copy(digitalDateColor = argb))
                    "ANALOG_SECOND" -> onUpdate(config.copy(analogSecondHandColor = argb))
                    "ANALOG_MAIN" -> {
                        val closestSwatch = WidgetColorSwatch.entries.firstOrNull { it.colorArgb == argb } ?: WidgetColorSwatch.BLUE
                        onUpdate(
                            config.copy(
                                colour = ColourSource.PALETTE,
                                swatch = closestSwatch,
                                analogPrimaryColor = argb,
                            ),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorConfirmed: (Color) -> Unit,
) {
    val hsvArray = remember(initialColor) {
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), array)
        array
    }
    var hue by remember { mutableFloatStateOf(hsvArray[0]) }
    var saturation by remember { mutableFloatStateOf(hsvArray[1]) }
    var value by remember { mutableFloatStateOf(hsvArray[2]) }

    val currentColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    var hexInput by remember(currentColor) {
        mutableStateOf(String.format("%06X", (currentColor.toArgb() and 0xFFFFFF)))
    }
    var isHexValid by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Choose custom colour",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Color Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                )

                Spacer(Modifier.height(16.dp))

                // 2D Saturation / Value Canvas Field
                val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        },
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Saturation gradient (white to pure hue)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, pureHueColor),
                            ),
                        )
                        // Value gradient (transparent to black)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                            ),
                        )

                        // Draw handle indicator
                        val handleX = saturation * widthPx
                        val handleY = (1f - value) * heightPx
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = Offset(handleX, handleY),
                            style = Stroke(width = 3.dp.toPx()),
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 13.5.dp.toPx(),
                            center = Offset(handleX, handleY),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 1D Hue Slider Canvas
                val rainbowColors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        },
                ) {
                    val widthPx = constraints.maxWidth.toFloat()

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(brush = Brush.horizontalGradient(colors = rainbowColors))

                        val handleX = (hue / 360f) * widthPx
                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = Offset(handleX, size.height / 2f),
                            style = Stroke(width = 3.dp.toPx()),
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 11.5.dp.toPx(),
                            center = Offset(handleX, size.height / 2f),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Hex Entry Field
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        val cleaned = input.removePrefix("#").trim().uppercase(Locale.ROOT)
                        hexInput = cleaned
                        if (cleaned.length == 6 && cleaned.all { it in "0123456789ABCDEF" }) {
                            isHexValid = true
                            try {
                                val parsedArgb = android.graphics.Color.parseColor("#$cleaned")
                                val newHsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsedArgb, newHsv)
                                hue = newHsv[0]
                                saturation = newHsv[1]
                                value = newHsv[2]
                            } catch (e: Exception) {
                                isHexValid = false
                            }
                        } else {
                            isHexValid = cleaned.length < 6 || cleaned.all { it in "0123456789ABCDEF" }
                        }
                    },
                    label = { Text("Hex Colour (#RRGGBB)") },
                    prefix = { Text("#") },
                    isError = !isHexValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onColorConfirmed(currentColor); onDismiss() }) {
                Text("Apply")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ColourSwatchRowWithCustom(
    selectedColor: Long?,
    onSelectColor: (Long?) -> Unit,
    onOpenCustomPicker: () -> Unit,
) {
    val predefinedArgbs = WidgetColorSwatch.entries.map { it.colorArgb }
    val isCustomSelected = selectedColor != null && selectedColor !in predefinedArgbs
    val customDisplayColor = if (isCustomSelected) Color(selectedColor) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Swatches 1 - 6
        WidgetColorSwatch.entries.forEach { swatch ->
            val selected = selectedColor == swatch.colorArgb
            val swatchColor = Color(swatch.colorArgb)

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .border(
                        width = if (selected) 3.dp else 0.dp,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clip(CircleShape)
                    .clickable { onSelectColor(swatch.colorArgb) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Swatch 7: Custom Colour Option with Palette Icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isCustomSelected) customDisplayColor else MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = if (isCustomSelected) 3.dp else 1.dp,
                    color = if (isCustomSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .clip(CircleShape)
                .clickable { onOpenCustomPicker() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = "Custom colour",
                tint = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AnalogControls(
    config: WidgetConfig,
    settings: ClockSettings,
    onUpdate: (WidgetConfig) -> Unit,
) {
    var showCustomColorPickerTarget by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Shape")
        OptionGroupSimple(
            value = config.shape,
            options = CURATED_SHAPES,
            optionLabel = {
                when (it) {
                    FaceShape.CIRCLE -> "Circle"
                    FaceShape.ROUNDED_SQUARE -> "Rounded"
                    else -> it.label
                }
            },
            onSelect = { onUpdate(config.copy(shape = it)) },
        )

        SectionHeader("Background")
        OptionGroupSimple(
            value = config.faceFill,
            options = FaceFill.entries,
            optionLabel = { it.label },
            onSelect = { onUpdate(config.copy(faceFill = it)) },
        )

        val currentAnalogMainColor = config.analogPrimaryColor
            ?: if (config.colour == ColourSource.PALETTE) config.swatch.colorArgb else null

        SectionHeader("Colour")
        ColourSwatchRowWithCustom(
            selectedColor = currentAnalogMainColor,
            onSelectColor = { colorArgb ->
                if (colorArgb != null) {
                    val swatch = WidgetColorSwatch.entries.firstOrNull { it.colorArgb == colorArgb } ?: WidgetColorSwatch.BLUE
                    onUpdate(
                        config.copy(
                            colour = ColourSource.PALETTE,
                            swatch = swatch,
                            analogPrimaryColor = colorArgb,
                        ),
                    )
                }
            },
            onOpenCustomPicker = { showCustomColorPickerTarget = "ANALOG_MAIN" },
        )

        IndexCarousel(
            label = "Clock face",
            value = config.minorIndices,
            settings = settings,
            config = config,
            onSelect = { onUpdate(config.copy(minorIndices = it)) },
        )

        HandCarousel(
            label = "Hands",
            value = config.hourHand,
            settings = settings,
            config = config,
            lengthR = DialGeometry.MINUTE_LENGTH,
            widthR = DialGeometry.MINUTE_WIDTH,
            tailR = DialGeometry.MINUTE_TAIL,
            colour = { it.hourHand },
        ) { style ->
            onUpdate(config.copy(hourHand = style, minuteHand = style))
        }

        ControlSwitchRow(
            title = "Second hand",
            checked = config.secondHand != HandStyle.OFF,
            onChange = { enabled ->
                onUpdate(config.copy(secondHand = if (enabled) HandStyle.BATON else HandStyle.OFF))
            },
        )

        AnimatedVisibility(
            visible = config.secondHand != HandStyle.OFF,
            enter = fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                    expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(tween(220, easing = FastOutSlowInEasing)) +
                   shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Second hand colour")
                ColourSwatchRowWithCustom(
                    selectedColor = config.analogSecondHandColor,
                    onSelectColor = { onUpdate(config.copy(analogSecondHandColor = it)) },
                    onOpenCustomPicker = { showCustomColorPickerTarget = "ANALOG_SECOND" },
                )
            }
        }

        ControlSwitchRow(
            title = "Numerals",
            checked = config.numerals != NumeralSystem.NONE,
            onChange = { enabled ->
                onUpdate(config.copy(numerals = if (enabled) NumeralSystem.ARABIC else NumeralSystem.NONE))
            },
        )

        AnimatedVisibility(
            visible = config.numerals != NumeralSystem.NONE,
            enter = fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                    expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(tween(220, easing = FastOutSlowInEasing)) +
                   shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Typeface")
                TypefaceChooserCardRow(
                    value = config.analogFont,
                    onSelect = { newFont ->
                        val newWeights = when (newFont) {
                            WidgetFontFamily.GOOGLE_SANS, WidgetFontFamily.GOOGLE_SANS_NORMAL -> listOf(
                                DigitalTypefaceWeight.THIN,
                                DigitalTypefaceWeight.REGULAR,
                                DigitalTypefaceWeight.MEDIUM,
                                DigitalTypefaceWeight.BOLD,
                            )
                            WidgetFontFamily.SERIF, WidgetFontFamily.MONOSPACE -> listOf(
                                DigitalTypefaceWeight.REGULAR,
                                DigitalTypefaceWeight.BOLD,
                            )
                            WidgetFontFamily.CURSIVE -> listOf(
                                DigitalTypefaceWeight.REGULAR,
                            )
                        }
                        val safeWeight = if (config.analogWeight in newWeights) config.analogWeight else newWeights.first()
                        onUpdate(config.copy(analogFont = newFont, analogWeight = safeWeight))
                    },
                )

                val availableAnalogWeights = remember(config.analogFont) {
                    when (config.analogFont) {
                        WidgetFontFamily.GOOGLE_SANS, WidgetFontFamily.GOOGLE_SANS_NORMAL -> listOf(
                            DigitalTypefaceWeight.THIN,
                            DigitalTypefaceWeight.REGULAR,
                            DigitalTypefaceWeight.MEDIUM,
                            DigitalTypefaceWeight.BOLD,
                        )
                        WidgetFontFamily.SERIF, WidgetFontFamily.MONOSPACE -> listOf(
                            DigitalTypefaceWeight.REGULAR,
                            DigitalTypefaceWeight.BOLD,
                        )
                        WidgetFontFamily.CURSIVE -> listOf(
                            DigitalTypefaceWeight.REGULAR,
                        )
                    }
                }

                val currentAnalogWeight = if (config.analogWeight in availableAnalogWeights) config.analogWeight else availableAnalogWeights.first()

                SectionHeader("Weight")
                Box(modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) {
                    OptionGroupSimple(
                        value = currentAnalogWeight,
                        options = availableAnalogWeights,
                        optionLabel = { it.label },
                        onSelect = { onUpdate(config.copy(analogWeight = it)) },
                    )
                }

                SectionHeader("Numeral placement")
                OptionGroupSimple(
                    value = config.numeralLayout,
                    options = NumeralLayout.entries,
                    optionLabel = {
                        when (it) {
                            NumeralLayout.CIRCLE_UPRIGHT -> "Circle"
                            NumeralLayout.SHAPE_UPRIGHT -> "Shape"
                            NumeralLayout.SHAPE_ROTATED -> "Turned"
                        }
                    },
                    onSelect = { onUpdate(config.copy(numeralLayout = it)) },
                )

                SectionHeader("Numeral size")
                OptionGroupSimple(
                    value = config.numeralSize,
                    options = NumeralSize.entries,
                    optionLabel = { it.label },
                    onSelect = { onUpdate(config.copy(numeralSize = it)) },
                )
            }
        }
    }

    if (showCustomColorPickerTarget != null) {
        val initialColorLong = when (showCustomColorPickerTarget) {
            "ANALOG_SECOND" -> config.analogSecondHandColor
            "ANALOG_MAIN" -> config.analogPrimaryColor ?: if (config.colour == ColourSource.PALETTE) config.swatch.colorArgb else null
            else -> config.analogPrimaryColor ?: if (config.colour == ColourSource.PALETTE) config.swatch.colorArgb else null
        } ?: WidgetColorSwatch.BLUE.colorArgb

        CustomColorPickerDialog(
            initialColor = Color(initialColorLong),
            onDismiss = { showCustomColorPickerTarget = null },
            onColorConfirmed = { chosenColor ->
                val argb = chosenColor.toArgb().toLong() and 0xFFFFFFFFL
                when (showCustomColorPickerTarget) {
                    "ANALOG_SECOND" -> onUpdate(config.copy(analogSecondHandColor = argb))
                    "ANALOG_MAIN" -> {
                        val closestSwatch = WidgetColorSwatch.entries.firstOrNull { it.colorArgb == argb } ?: WidgetColorSwatch.BLUE
                        onUpdate(
                            config.copy(
                                colour = ColourSource.PALETTE,
                                swatch = closestSwatch,
                                analogPrimaryColor = argb,
                            ),
                        )
                    }
                    else -> {
                        val closestSwatch = WidgetColorSwatch.entries.firstOrNull { it.colorArgb == argb } ?: WidgetColorSwatch.BLUE
                        onUpdate(
                            config.copy(
                                colour = ColourSource.PALETTE,
                                swatch = closestSwatch,
                                analogPrimaryColor = argb,
                            ),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun DigitalPreview(
    config: WidgetConfig,
    settings: ClockSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val wallpaper = rememberWallpaperBitmap()
    val isDark = isSystemInDarkTheme()

    val colours = remember(settings, config.colour, config.palette, config.swatch) {
        WidgetColours.resolve(context, config, settings)
    }

    val faceColor = Color(colours.face)
    val outlineColor = Color(colours.outline)

    val targetHourColor = config.digitalHourColor?.let { Color(it) } ?: Color(colours.hourHand)
    val targetColonColor = config.digitalColonColor?.let { Color(it) } ?: Color(colours.onFace)
    val targetMinuteColor = config.digitalMinuteColor?.let { Color(it) } ?: Color(colours.minuteHand)
    val targetDayColor = config.digitalDayColor?.let { Color(it) } ?: Color(colours.minor)
    val targetDateColor = config.digitalDateColor?.let { Color(it) } ?: Color(colours.minor)

    val hourColor by animateColorAsState(targetHourColor, tween(220), label = "hourColorAnim")
    val colonColor by animateColorAsState(targetColonColor, tween(220), label = "colonColorAnim")
    val minuteColor by animateColorAsState(targetMinuteColor, tween(220), label = "minuteColorAnim")
    val dayColor by animateColorAsState(targetDayColor, tween(220), label = "dayColorAnim")
    val dateColor by animateColorAsState(targetDateColor, tween(220), label = "dateColorAnim")

    val isStacked = config.digitalLayout == DigitalLayout.STACKED
    val isCompact = config.digitalLayout == DigitalLayout.COMPACT
    val isCursive = config.digitalFont == WidgetFontFamily.CURSIVE

    val previewCardHeight by animateDpAsState(
        targetValue = if (isStacked) 310.dp else 250.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "previewCardHeightAnim",
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(previewCardHeight)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Persistent Wallpaper / Gradient Card Background - NEVER flashes white!
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = "Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val gradientColors = if (isDark) {
                listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF111827))
            } else {
                listOf(Color(0xFFC7D9FA), Color(0xFFE2D9F3), Color(0xFFE5ECF6))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradientColors)),
            )
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = when (config.digitalBackground) {
                DigitalBackground.FILLED -> faceColor
                else -> Color.Transparent
            },
            border = if (config.digitalBackground == DigitalBackground.OUTLINED) {
                BorderStroke(3.dp, outlineColor)
            } else null,
            modifier = Modifier
                .padding(6.dp)
                .fillMaxSize(),
        ) {
            val fontFam = config.digitalFont.toComposeFontFamily(config.digitalWeight.weightInt)
            val targetWeight = FontWeight(config.digitalWeight.weightInt)

            val timeFontSize by animateFloatAsState(
                targetValue = when {
                    isStacked -> 68f
                    isCompact -> 38f
                    else -> 72f
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "fontSizeAnim",
            )

            val stackedLineHeightMultiplier = if (isCursive) 0.88f else 0.78f
            val timeBoxHeight = if (isStacked) {
                if (isCursive) 140.dp else 120.dp
            } else if (isCompact) 52.dp else 78.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val targetStatePair = Pair(isStacked, config.digitalFont)

                AnimatedContent(
                    targetState = targetStatePair,
                    transitionSpec = {
                        if (initialState.first != targetState.first) {
                            (fadeIn(tween(220)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy), initialScale = 0.94f))
                                .togetherWith(fadeOut(tween(180)) + scaleOut(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy), targetScale = 0.94f))
                        } else {
                            fadeIn(tween(260, easing = FastOutSlowInEasing))
                                .togetherWith(fadeOut(tween(220, easing = FastOutSlowInEasing)))
                        }
                    },
                    label = "stackedLayoutTransition",
                    modifier = Modifier.fillMaxWidth(),
                ) { (isStackedLayout, _) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // Fixed-height Time Region (Stationary Position)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(timeBoxHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isStackedLayout) {
                                val stackedAnnotatedText = remember(hourColor, minuteColor) {
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(color = hourColor)) {
                                            append("08")
                                        }
                                        append("\n")
                                        withStyle(SpanStyle(color = minuteColor)) {
                                            append("30")
                                        }
                                    }
                                }
                                Text(
                                    text = stackedAnnotatedText,
                                    style = MaterialTheme.typography.displayLargeEmphasized.copy(
                                        fontFamily = fontFam,
                                        fontWeight = targetWeight,
                                        fontSize = timeFontSize.sp,
                                        lineHeight = (timeFontSize * stackedLineHeightMultiplier).sp,
                                        textAlign = TextAlign.Center,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    ),
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = "08",
                                        style = MaterialTheme.typography.displayLargeEmphasized.copy(
                                            fontFamily = fontFam,
                                            fontWeight = targetWeight,
                                            fontSize = timeFontSize.sp,
                                        ),
                                        color = hourColor,
                                    )
                                    Text(
                                        text = ":",
                                        style = MaterialTheme.typography.displayLargeEmphasized.copy(
                                            fontFamily = fontFam,
                                            fontWeight = targetWeight,
                                            fontSize = timeFontSize.sp,
                                        ),
                                        color = colonColor,
                                    )
                                    Text(
                                        text = if (config.showSeconds && !isCompact) "30:45" else "30",
                                        style = MaterialTheme.typography.displayLargeEmphasized.copy(
                                            fontFamily = fontFam,
                                            fontWeight = targetWeight,
                                            fontSize = timeFontSize.sp,
                                        ),
                                        color = minuteColor,
                                    )
                                }
                            }
                        }

                        // Reserved Label Region (Stationary 4-State Crossfade)
                        val labelState = when {
                            config.showDayOfWeek && config.showDate -> DigitalLabelState.BOTH
                            config.showDayOfWeek -> DigitalLabelState.DAY_ONLY
                            config.showDate -> DigitalLabelState.DATE_ONLY
                            else -> DigitalLabelState.NONE
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                targetState = labelState,
                                transitionSpec = {
                                    fadeIn(tween(240, easing = FastOutSlowInEasing))
                                        .togetherWith(fadeOut(tween(180, easing = FastOutSlowInEasing)))
                                },
                                label = "labelStateCrossfade",
                            ) { targetLabelState ->
                                when (targetLabelState) {
                                    DigitalLabelState.NONE -> {
                                        Spacer(Modifier.height(0.dp))
                                    }
                                    DigitalLabelState.DAY_ONLY -> {
                                        Text(
                                            text = "MON",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = fontFam,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = dayColor,
                                        )
                                    }
                                    DigitalLabelState.DATE_ONLY -> {
                                        Text(
                                            text = "OCT 27",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = fontFam,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = dateColor,
                                        )
                                    }
                                    DigitalLabelState.BOTH -> {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "MON",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = fontFam,
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = dayColor,
                                            )
                                            Text(
                                                text = ", ",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = fontFam,
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = dateColor,
                                            )
                                            Text(
                                                text = "OCT 27",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = fontFam,
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = dateColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalogPreview(
    config: WidgetConfig,
    settings: ClockSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val wallpaper = rememberWallpaperBitmap()
    val isDark = isSystemInDarkTheme()

    Box(
        modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Persistent Wallpaper / Gradient Card Background - NEVER flashes white!
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = "Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val gradientColors = if (isDark) {
                listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF111827))
            } else {
                listOf(Color(0xFFC7D9FA), Color(0xFFE2D9F3), Color(0xFFE5ECF6))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradientColors)),
            )
        }

        val bitmap: Bitmap = remember(config, settings, density) {
            val px = with(density) { 360.dp.toPx() }.toInt()
            renderStill(context, config, settings, px, px)
        }

        Box(
            modifier = Modifier
                .size(220.dp)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = bitmap,
                animationSpec = tween(durationMillis = 200),
                label = "analogBitmapCrossfade",
            ) { currentBitmap ->
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Analog Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun HandCarousel(
    label: String,
    value: HandStyle,
    settings: ClockSettings,
    config: WidgetConfig,
    lengthR: Float,
    widthR: Float,
    tailR: Float,
    colour: (FaceRenderer.Colours) -> Int,
    onSelect: (HandStyle) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    OptionCarousel(
        label = label,
        value = value,
        options = CURATED_HANDS,
        optionLabel = { it.label },
        onSelect = onSelect,
    ) { style, selected ->
        val colours = remember(settings, config.colour, config.palette, config.swatch) {
            WidgetColours.resolve(context, config, settings)
        }
        val ink = if (selected) MaterialTheme.colorScheme.onPrimary.toArgb() else colour(colours)
        val pin = if (selected) ink else colours.pin
        val bitmap = remember(style, ink, pin, density) {
            val px = with(density) { 62.dp.toPx() }
            HandRenderer.render(
                style, ink, px / 2f, lengthR, widthR, tailR, DialGeometry.PIN_DIAMETER, pin,
            )
        }
        ThumbnailImage(bitmap, style.label)
    }
}

@Composable
private fun IndexCarousel(
    label: String,
    value: MinorIndex,
    settings: ClockSettings,
    config: WidgetConfig,
    onSelect: (MinorIndex) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    OptionCarousel(
        label = label,
        value = value,
        options = MinorIndex.entries,
        optionLabel = { it.label },
        onSelect = onSelect,
    ) { option, selected ->
        val onCell = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        if (option == MinorIndex.NONE) {
            Text("None", style = MaterialTheme.typography.labelLarge, color = onCell)
        } else {
            val ink = onCell.toArgb()
            val bitmap = remember(option, settings, config.colour, config.palette, config.swatch, ink, density) {
                val px = with(density) { 62.dp.toPx() }.toInt()
                val probe = config.copy(
                    shape = FaceShape.CIRCLE,
                    faceFill = FaceFill.NONE,
                    outline = OutlineStyle.NONE,
                    numerals = NumeralSystem.NONE,
                    indexSet = IndexSet.NONE,
                    minorIndices = option,
                    date = DateMode.NONE,
                )
                val base = WidgetColours.resolve(context, probe, settings)
                FaceRenderer.render(
                    context, probe,
                    FaceRenderer.Colours(
                        face = base.face, onFace = ink, minor = ink, outline = ink, accent = ink,
                        hourHand = ink, minuteHand = ink, secondHand = ink, pin = ink,
                    ),
                    px, px,
                ).bitmap
            }
            ThumbnailImage(bitmap, option.label)
        }
    }
}

private fun renderStill(
    context: Context,
    config: WidgetConfig,
    settings: ClockSettings,
    w: Int,
    h: Int,
): Bitmap {
    val colours = WidgetColours.resolve(context, config, settings)
    val face = FaceRenderer.render(context, config, colours, w, h)
    val canvas = Canvas(face.bitmap)

    fun hand(style: HandStyle, colour: Int, lengthR: Float, widthR: Float, tailR: Float, minutes: Float) {
        if (style == HandStyle.OFF) return
        val bmp = HandRenderer.render(style, colour, face.rMin, lengthR, widthR, tailR, 0f, colours.pin)
        canvas.save()
        canvas.rotate(minutes * 6f, face.cx, face.cy)
        canvas.drawBitmap(bmp, face.cx - bmp.width / 2f, face.cy - bmp.height / 2f, null)
        canvas.restore()
    }
    hand(config.hourHand, colours.hourHand, DialGeometry.HOUR_LENGTH, DialGeometry.HOUR_WIDTH, DialGeometry.HOUR_TAIL, 50.75f)
    hand(config.minuteHand, colours.minuteHand, DialGeometry.MINUTE_LENGTH, DialGeometry.MINUTE_WIDTH, DialGeometry.MINUTE_TAIL, 9f)
    hand(config.secondHand, colours.secondHand, DialGeometry.SECOND_LENGTH, DialGeometry.SECOND_WIDTH, DialGeometry.SECOND_TAIL, 35f)
    if (config.centrePin) {
        canvas.drawCircle(
            face.cx, face.cy, DialGeometry.PIN_DIAMETER * face.rMin / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = colours.pin },
        )
    }
    return face.bitmap
}

@Composable
private fun <T> OptionGroupSimple(
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val selectedIndex = options.indexOf(value).coerceAtLeast(0)
    val alignmentBias by animateFloatAsState(
        targetValue = if (options.size > 1) -1f + (2f * selectedIndex / (options.size - 1)) else -1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "optionPillBias",
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f / options.size)
                .fillMaxSize()
                .align(BiasAlignment(alignmentBias, 0f))
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2170E8)),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { option ->
                val selected = option == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = optionLabel(option),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlSwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 4.dp),
    )
}

@SuppressLint("MissingPermission")
@Composable
private fun rememberWallpaperBitmap(): ImageBitmap? {
    val context = LocalContext.current
    return remember {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            @Suppress("MissingPermission")
            val drawable = wallpaperManager.drawable
            drawable?.toBitmap()?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
