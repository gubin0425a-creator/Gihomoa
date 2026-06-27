package com.example.customkeyboard

import android.content.ClipDescription
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.widget.Toast
import android.graphics.Bitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.customkeyboard.data.DataStoreHelper
import com.example.customkeyboard.theme.CustomKeyboardTheme
import com.example.customkeyboard.ui.keyboard.KeyboardScreen
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class KeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val mViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = mViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    lateinit var dataStoreHelper: DataStoreHelper
    val hangulEngine = HangulEngine()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate()
        dataStoreHelper = DataStoreHelper(applicationContext)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        
        // Bind Compose Owners
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        
        composeView.setContent {
            CustomKeyboardTheme {
                KeyboardScreen(keyboardService = this)
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        hangulEngine.clear()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        finalizeHangulComposition()
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        serviceScope.cancel()
    }

    // --- Typing Input Operations ---

    fun updateComposing() {
        val inputConnection = currentInputConnection ?: return
        val composing = hangulEngine.getComposingString()
        // Setting composing text to empty string deletes it in the editor
        inputConnection.setComposingText(composing, 1)
    }

    fun finalizeHangulComposition() {
        val inputConnection = currentInputConnection ?: return
        val composing = hangulEngine.getComposingString()
        if (composing.isNotEmpty()) {
            inputConnection.commitText(composing, 1)
            hangulEngine.clear()
        }
    }

    fun handleJamoInput(jamo: Char) {
        val completed = hangulEngine.inputJamo(jamo)
        val inputConnection = currentInputConnection ?: return
        if (completed != null) {
            inputConnection.commitText(completed, 1)
        }
        updateComposing()
    }

    fun handleEnglishInput(char: Char) {
        finalizeHangulComposition()
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(char.toString(), 1)
    }

    fun handleBackspace() {
        val inputConnection = currentInputConnection ?: return
        if (!hangulEngine.isEmpty()) {
            val (_, changed) = hangulEngine.backspace()
            if (changed) {
                updateComposing()
            }
        } else {
            // Send backspace key events
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    fun handleSpace() {
        finalizeHangulComposition()
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(" ", 1)
    }

    fun handleEnterInput() {
        finalizeHangulComposition()
        val inputConnection = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo
        val imeAction = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        
        if (imeAction != null && imeAction != EditorInfo.IME_ACTION_NONE && imeAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
            inputConnection.performEditorAction(imeAction)
        } else {
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    fun handleSymbolInput(symbol: String) {
        finalizeHangulComposition()
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(symbol, 1)
        
        serviceScope.launch {
            dataStoreHelper.addRecentSymbol(symbol)
        }
    }

    // --- Rich Media Drawing Transfer Operations ---

    fun sendImageToApp(bitmap: Bitmap) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Ensure parent directory exists
                val drawingsDir = File(cacheDir, "drawings")
                if (!drawingsDir.exists()) {
                    drawingsDir.mkdirs()
                }

                val file = File(drawingsDir, "drawing_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                // Content URI via FileProvider
                val contentUri = FileProvider.getUriForFile(
                    this@KeyboardService,
                    "com.example.customkeyboard.fileprovider",
                    file
                )
                
                sendUriToApp(contentUri, "Custom Drawing")
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@KeyboardService, "그림 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun sendUriToApp(contentUri: android.net.Uri, label: String) {
        val inputConnection = currentInputConnection ?: return
        
        try {
            val description = ClipDescription(label, arrayOf("image/png"))
            val inputContentInfo = InputContentInfo(contentUri, description, null)
            
            val flags = InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            val success = inputConnection.commitContent(inputContentInfo, flags, null)
            
            serviceScope.launch(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@KeyboardService, "그림을 전송했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@KeyboardService, "이 입력창은 이미지 직접 전송을 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(this@KeyboardService, "전송 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getSavedDrawings(): List<File> {
        val drawingsDir = File(cacheDir, "drawings")
        if (!drawingsDir.exists()) return emptyList()
        return drawingsDir.listFiles { _, name -> name.startsWith("drawing_") && name.endsWith(".png") }
            ?.sortedByDescending { it.lastModified() }
            ?.take(28) ?: emptyList()
    }
}
