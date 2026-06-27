package com.example.customkeyboard.ui.keyboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.core.content.FileProvider
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.customkeyboard.KeyboardService
import java.io.File

// Drawing path model
data class PathInfo(
    val path: Path,
    val color: Color,
    val strokeWidth: Float = 8f
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KeyboardScreen(keyboardService: KeyboardService) {
    // Mode states
    // "qwerty" or "symbol" or "draw" or "history"
    var keyboardMode by remember { mutableStateOf("qwerty") }
    var selectedSymbolTab by remember { mutableStateOf("∑") } // default symbol tab
    var isEnglish by remember { mutableStateOf(false) }
    var isShiftActive by remember { mutableStateOf(false) }

    // Recent symbols collected from DataStore
    val recentSymbolsState = keyboardService.dataStoreHelper.recentSymbols.collectAsState(initial = emptyList())
    val recentSymbols = recentSymbolsState.value

    // Tab items list
    val tabs = listOf("◷", "∑", "◦", "∽", "⊞", "❖", "☼", "✎", "⎘")

    // Core layout container
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(Color(0xFF1E1E24))
    ) {
        // [Row 1: Symbol/Action Tabs]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF121216)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = when (tab) {
                    "✎" -> keyboardMode == "draw"
                    "⎘" -> keyboardMode == "history"
                    "◷" -> keyboardMode == "symbol" && selectedSymbolTab == "◷"
                    else -> keyboardMode == "symbol" && selectedSymbolTab == tab
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            when (tab) {
                                "✎" -> keyboardMode = "draw"
                                "⎘" -> keyboardMode = "history"
                                "◷" -> {
                                    keyboardMode = "symbol"
                                    selectedSymbolTab = "◷"
                                }
                                else -> {
                                    keyboardMode = "symbol"
                                    selectedSymbolTab = tab
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color(0xFF64B5F6) else Color(0xFF9E9EAE),
                            fontSize = 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(width = 16.dp, height = 2.dp)
                                    .background(Color(0xFF64B5F6), RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }
        }

        // [Keyboard Content Area]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E1E24))
        ) {
            when (keyboardMode) {
                "qwerty" -> {
                    QWERTYLayout(
                        keyboardService = keyboardService,
                        isEnglish = isEnglish,
                        isShiftActive = isShiftActive,
                        onShiftToggle = { isShiftActive = !isShiftActive },
                        onLanguageToggle = { isEnglish = !isEnglish },
                        onSymbolPadSwitch = {
                            keyboardMode = "symbol"
                            selectedSymbolTab = "∑"
                        }
                    )
                }
                "symbol" -> {
                    SymbolPadLayout(
                        keyboardService = keyboardService,
                        selectedTab = selectedSymbolTab,
                        recentSymbols = recentSymbols,
                        onReturnToQwerty = { keyboardMode = "qwerty" }
                    )
                }
                "draw" -> {
                    DrawingCanvasLayout(
                        keyboardService = keyboardService,
                        onReturnToQwerty = { keyboardMode = "qwerty" }
                    )
                }
                "history" -> {
                    DrawingsHistoryLayout(
                        keyboardService = keyboardService,
                        onReturnToQwerty = { keyboardMode = "qwerty" }
                    )
                }
            }
        }
    }
}

// --- 1. QWERTY Layout ---
@Composable
fun QWERTYLayout(
    keyboardService: KeyboardService,
    isEnglish: Boolean,
    isShiftActive: Boolean,
    onShiftToggle: () -> Unit,
    onLanguageToggle: () -> Unit,
    onSymbolPadSwitch: () -> Unit
) {
    // Hangul layout definitions
    val koRow2 = if (isShiftActive) listOf("ㅃ", "ㅉ", "ㄸ", "ㄲ", "ㅆ", "ㅛ", "ㅕ", "ㅑ", "ㅒ", "ㅖ")
                 else listOf("ㅂ", "ㅈ", "ㄷ", "ㄱ", "ㅅ", "ㅛ", "ㅕ", "ㅑ", "ㅐ", "ㅔ")
    val koRow3 = listOf("ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ", "ㅗ", "ㅓ", "ㅏ", "ㅣ")
    val koRow4 = listOf("ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ")

    // English layout definitions
    val enRow2 = if (isShiftActive) listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                 else listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val enRow3 = if (isShiftActive) listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                 else listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val enRow4 = if (isShiftActive) listOf("Z", "X", "C", "V", "B", "N", "M")
                 else listOf("z", "x", "c", "v", "b", "n", "m")

    val row2 = if (isEnglish) enRow2 else koRow2
    val row3 = if (isEnglish) enRow3 else koRow3
    val row4 = if (isEnglish) enRow4 else koRow4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row2.forEach { char ->
                KeyButton(text = char, modifier = Modifier.weight(1f)) {
                    if (isEnglish) {
                        keyboardService.handleEnglishInput(char[0])
                    } else {
                        keyboardService.handleJamoInput(char[0])
                    }
                }
            }
        }

        // Row 3 (Centered - added 0.5f weight spacer on each side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            row3.forEach { char ->
                KeyButton(text = char, modifier = Modifier.weight(1f)) {
                    if (isEnglish) {
                        keyboardService.handleEnglishInput(char[0])
                    } else {
                        keyboardService.handleJamoInput(char[0])
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }

        // Row 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift
            KeyButton(
                text = "⇧",
                modifier = Modifier.weight(1.3f),
                bgColor = if (isShiftActive) Color(0xFF64B5F6) else Color(0xFF3E3E48),
                textColor = if (isShiftActive) Color.Black else Color.White
            ) {
                onShiftToggle()
            }

            row4.forEach { char ->
                KeyButton(text = char, modifier = Modifier.weight(1f)) {
                    if (isEnglish) {
                        keyboardService.handleEnglishInput(char[0])
                    } else {
                        keyboardService.handleJamoInput(char[0])
                    }
                }
            }

            // Backspace
            KeyButton(
                text = "⌫",
                modifier = Modifier.weight(1.3f),
                bgColor = Color(0xFF3E3E48)
            ) {
                keyboardService.handleBackspace()
            }
        }

        // Row 5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol pad mode trigger [!#1]
            KeyButton(
                text = "!#1",
                modifier = Modifier.weight(1.3f),
                bgColor = Color(0xFF3E3E48)
            ) {
                onSymbolPadSwitch()
            }

            // Lang Switch [한/영]
            KeyButton(
                text = if (isEnglish) "한글" else "한/영",
                modifier = Modifier.weight(1.3f),
                bgColor = Color(0xFF3E3E48)
            ) {
                onLanguageToggle()
            }

            // Comma
            KeyButton(text = ",", modifier = Modifier.weight(1f)) {
                keyboardService.handleEnglishInput(',')
            }

            // Spacebar
            KeyButton(
                text = "스페이스바",
                modifier = Modifier.weight(3.8f),
                fontSize = 14.sp
            ) {
                keyboardService.handleSpace()
            }

            // Period
            KeyButton(text = ".", modifier = Modifier.weight(1f)) {
                keyboardService.handleEnglishInput('.')
            }

            // Enter
            KeyButton(
                text = "↵",
                modifier = Modifier.weight(1.5f),
                bgColor = Color(0xFF64B5F6),
                textColor = Color.Black
            ) {
                keyboardService.handleEnterInput()
            }
        }
    }
}

// --- Generic Key Button ---
@Composable
fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF2A2A32),
    textColor: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .background(bgColor, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// --- 2. Symbol Pad Layout ---
@Composable
fun SymbolPadLayout(
    keyboardService: KeyboardService,
    selectedTab: String,
    recentSymbols: List<String>,
    onReturnToQwerty: () -> Unit
) {
    // Generate symbols for the selected tab
    val symbols = remember(selectedTab, recentSymbols) {
        when (selectedTab) {
            "◷" -> recentSymbols
            "∑" -> getMathSymbols()
            "◦" -> getCirclesSymbols()
            "∽" -> getCurvesSymbols()
            "⊞" -> getBoxesSymbols()
            "❖" -> getShapesSymbols()
            "☼" -> getWeatherSymbols()
            else -> emptyList()
        }
    }

    // Grid details: 7 cols x 4 rows = 28 per page
    val itemsPerPage = 28
    val pageCount = if (selectedTab == "◷") 1 else maxOf(1, (symbols.size + itemsPerPage - 1) / itemsPerPage)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val pageStart = pageIndex * itemsPerPage
            val pageEnd = minOf(pageStart + itemsPerPage, symbols.size)
            val pageItems = if (pageStart < symbols.size) symbols.subList(pageStart, pageEnd) else emptyList()

            // 7x4 Grid of symbols
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(4) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(7) { colIndex ->
                            val itemIndex = rowIndex * 7 + colIndex
                            if (itemIndex < pageItems.size) {
                                val sym = pageItems[itemIndex]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .shadow(1.dp, RoundedCornerShape(6.dp))
                                        .background(Color(0xFF2E2E38), RoundedCornerShape(6.dp))
                                        .clickable { keyboardService.handleSymbolInput(sym) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sym,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                // Empty spacer cell
                                Spacer(modifier = Modifier.weight(1f).height(42.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bottom Bar (Page Dots in center, [자판으로] on the right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spacer to balance
            Spacer(modifier = Modifier.width(80.dp))

            // Page indicators (Dots)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pageCount) { index ->
                    val color = if (pagerState.currentPage == index) Color(0xFF64B5F6) else Color(0xFF55555C)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Return to QWERTY Button
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(34.dp)
                    .background(Color(0xFF3E3E48), RoundedCornerShape(6.dp))
                    .clickable { onReturnToQwerty() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "자판으로 ⌨",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- 3. Drawing Canvas Layout ---
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvasLayout(keyboardService: KeyboardService, onReturnToQwerty: () -> Unit) {
    val paths = remember { mutableStateListOf<PathInfo>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Color list
    val colors = listOf(
        Pair("흰", Color.White),
        Pair("빨", Color(0xFFE57373)),
        Pair("노", Color(0xFFFFF176)),
        Pair("초", Color(0xFF81C784)),
        Pair("파", Color(0xFF64B5F6)),
        Pair("보", Color(0xFFBA68C8)),
        Pair("검", Color.Black)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Canvas Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .onSizeChanged { canvasSize = it }
        ) {
            // 1. Transparent checkered background pattern in UI (strictly visual)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val squareSize = 14.dp.toPx()
                val cols = (size.width / squareSize).toInt() + 1
                val rows = (size.height / squareSize).toInt() + 1
                for (i in 0 until cols) {
                    for (j in 0 until rows) {
                        val color = if ((i + j) % 2 == 0) Color(0xFF28282E) else Color(0xFF202026)
                        drawRect(
                            color = color,
                            topLeft = Offset(i * squareSize, j * squareSize),
                            size = Size(squareSize, squareSize)
                        )
                    }
                }
            }

            // 2. Painting stroke capture canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                val p = Path().apply { moveTo(event.x, event.y) }
                                currentPath = p
                                paths.add(PathInfo(p, currentColor))
                            }
                            MotionEvent.ACTION_MOVE -> {
                                currentPath?.lineTo(event.x, event.y)
                                // Force recomposition via updating a dummy size or index
                                val last = paths.lastOrNull()
                                if (last != null) {
                                    paths[paths.lastIndex] = last.copy()
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                currentPath = null
                            }
                        }
                        true
                    }
            ) {
                paths.forEach { pathInfo ->
                    drawPath(
                        path = pathInfo.path,
                        color = pathInfo.color,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = pathInfo.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            
            if (paths.isEmpty()) {
                Text(
                    text = "여기에 손가락으로 그림을 그려보세요",
                    color = Color(0x66FFFFFF),
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Bottom Color & Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // [Clear] button
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .weight(1.5f)
                    .background(Color(0xFFE57373).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFE57373), RoundedCornerShape(6.dp))
                    .clickable { paths.clear() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "지우기",
                    color = Color(0xFFE57373),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Colors row
            Row(
                modifier = Modifier.weight(4.5f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { (name, color) ->
                    val isSelected = currentColor == color
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF64B5F6) else Color(0x33FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { currentColor = color },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (color == Color.White) Color.Black else Color.White)
                            )
                        }
                    }
                }
            }

            // [Save & Send] button
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .weight(2f)
                    .background(Color(0xFF64B5F6), RoundedCornerShape(6.dp))
                    .clickable {
                        if (paths.isNotEmpty()) {
                            val w = if (canvasSize.width > 0) canvasSize.width else 600
                            val h = if (canvasSize.height > 0) canvasSize.height else 400
                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            // Transparent background in exported PNG
                            canvas.drawColor(android.graphics.Color.TRANSPARENT)

                            paths.forEach { pathInfo ->
                                val paint = android.graphics.Paint().apply {
                                    this.color = pathInfo.color.toArgb()
                                    this.strokeWidth = pathInfo.strokeWidth
                                    this.style = android.graphics.Paint.Style.STROKE
                                    this.strokeJoin = android.graphics.Paint.Join.ROUND
                                    this.strokeCap = android.graphics.Paint.Cap.ROUND
                                    this.isAntiAlias = true
                                }
                                canvas.drawPath(pathInfo.path.asAndroidPath(), paint)
                            }
                            keyboardService.sendImageToApp(bitmap)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "저장/전송",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Return button
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(44.dp)
                    .background(Color(0xFF3E3E48), RoundedCornerShape(6.dp))
                    .clickable { onReturnToQwerty() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌨",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Helper color converter extension
fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
           ((this.red * 255.0f + 0.5f).toInt() shl 16) or
           ((this.green * 255.0f + 0.5f).toInt() shl 8) or
           (this.blue * 255.0f + 0.5f).toInt()
}

// --- 4. Drawings History Layout ---
@Composable
fun DrawingsHistoryLayout(keyboardService: KeyboardService, onReturnToQwerty: () -> Unit) {
    var savedDrawings by remember { mutableStateOf<List<File>>(emptyList()) }

    // Load drawings on entry
    LaunchedEffect(Unit) {
        savedDrawings = keyboardService.getSavedDrawings()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (savedDrawings.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "저장된 그림이 없습니다.",
                    color = Color(0x66FFFFFF),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(savedDrawings) { file ->
                    val bitmap = remember(file) {
                        try {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1.3f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF28282E))
                            .border(1.dp, Color(0xFF3A3A42), RoundedCornerShape(6.dp))
                            .clickable {
                                val contentUri = FileProvider.getUriForFile(
                                    keyboardService,
                                    "com.example.customkeyboard.fileprovider",
                                    file
                                )
                                keyboardService.sendUriToApp(contentUri, "Saved Drawing")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Saved Drawing Thumbnail",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "터치 시 즉시 전송",
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )

            // Return to QWERTY
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(34.dp)
                    .background(Color(0xFF3E3E48), RoundedCornerShape(6.dp))
                    .clickable { onReturnToQwerty() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "자판으로 ⌨",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- 5. Symbol datasets ---
fun getMathSymbols(): List<String> {
    return listOf(
        "∑", "√", "π", "∞", "±", "×", "÷", "≠", "≈", "≤", "≥", "°", "∠", "⊥", "∩", "∪", "⊂", "⊃", "⊆", "⊇", "∈", "∉", "∀", "∃", "∴", "∵", "∫", "∬",
        "∭", "∯", "∰", "∱", "∲", "∳", "⇦", "⇧", "⇨", "⇩", "➔", "➘", "➙", "➚", "➛", "➜", "➝", "➞", "➟", "➠", "➡", "➢", "➣", "➤", "➥", "➦", "➧", "➨"
    )
}

fun getCirclesSymbols(): List<String> {
    return listOf(
        "◦", "●", "○", "◎", "☉", "◌", "◯", "∙", "·", "•", "⁃", "◘", "◙", "◜", "◝", "◞", "◟", "◡", "◠", "⊙", "⊚", "⊛", "⊜", "⊝", "⊕", "⊖", "⊗", "⊘",
        "⊹", "⊺", "⊻", "⊼", "⊽", "⊾", "⊿", "⋀", "⋁", "⋂", "⋃", "⋄", "⋅", "⋆", "⋇", "⋈", "⋉", "⋊", "⋋", "⋌", "⋍", "⋎", "⋏", "⋐", "⋑", "⋒", "⋓", "⋔"
    )
}

fun getCurvesSymbols(): List<String> {
    return listOf(
        "∽", "~", "≈", "≋", "≌", "≂", "≃", "≄", "≅", "≆", "≇", "≉", "≊", "≍", "≎", "≏", "≐", "≑", "≒", "≓", "≔", "≕", "≖", "≗", "≘", "≙", "≚", "≜",
        "≝", "≞", "≟", "≠", "≡", "≢", "≣", "≤", "≥", "≦", "≧", "≨", "≩", "≪", "≫", "≬", "≭", "≮", "≯", "≰", "≱", "≲", "≳", "≴", "≵", "≶", "≷"
    )
}

fun getBoxesSymbols(): List<String> {
    return listOf(
        "⊞", "⊟", "⊠", "⊡", "□", "■", "▢", "▣", "▤", "▥", "▦", "▧", "▨", "▩", "▪", "▫", "▬", "▭", "▮", "▯", "▰", "▱", "▲", "▼", "◀", "▶", "◆", "◇",
        "◈", "◉", "◊", "○", "◌", "◍", "◎", "●", "◐", "◑", "◒", "◓", "◔", "◕", "◖", "◗", "◘", "◙", "◚", "◛", "◜", "◝", "◞", "◟", "◠", "◡", "◢"
    )
}

fun getShapesSymbols(): List<String> {
    return listOf(
        "❖", "✦", "✧", "★", "☆", "✪", "✫", "✬", "✭", "✮", "✯", "✰", "❣", "❤", "♡", "♥", "♦", "♢", "♣", "♧", "♠", "♤", "☿", "♀", "♂", "♁", "☿", "♃",
        "♄", "♅", "♆", "♇", "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓", "♔", "♕", "♖", "♗", "♘", "♙", "♚", "♛", "♜", "♝", "♞"
    )
}

fun getWeatherSymbols(): List<String> {
    return listOf(
        "☼", "☀", "☁", "☂", "☃", "☄", "☇", "☈", "☉", "☊", "☋", "☌", "☍", "☎", "☏", "☐", "☑", "☒", "☓", "☠", "☡", "☢", "☣", "☤", "☥", "☦", "☧",
        "☨", "☩", "☪", "☫", "☬", "☭", "☮", "☯", "☰", "☱", "☲", "☳", "☴", "☵", "☶", "☷", "☸", "☹", "☺", "☻", "☼", "☽", "☾", "☿", "♀", "♁"
    )
}
