package com.example.customkeyboard.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.customkeyboard.data.DefaultDataRepository
import com.example.customkeyboard.theme.CustomKeyboardTheme

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  when (state) {
    MainScreenUiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is MainScreenUiState.Success -> {
      KeyboardSetupScreen(modifier = modifier)
    }
    is MainScreenUiState.Error -> {
      Text("데이터 로딩 중 오류 발생: ${(state as MainScreenUiState.Error).throwable.message}")
    }
  }
}

@Composable
internal fun KeyboardSetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isEnabled by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }

    // Helper functions to check keyboard status
    fun checkStatus() {
        isEnabled = isKeyboardEnabled(context)
        isSelected = isKeyboardSelected(context)
    }

    // Refresh status when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Title Header
        Text(
            text = "그림 & 특수문자 키보드 ⌨️",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "카카오톡 등 메신저에서 손쉽게 특수문자를 스와이프하고,\n나만의 투명 배경 그림을 바로 그려 전송해보세요!",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Step 1 Card
        StepCard(
            stepNumber = 1,
            title = "키보드 활성화",
            description = "시스템 설정의 스크린 키보드 목록에서 '그림 및 특수문자 키보드'를 활성화해주세요.",
            buttonText = "1단계: 설정하러 가기",
            isCompleted = isEnabled,
            onClick = {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2 Card
        StepCard(
            stepNumber = 2,
            title = "기본 키보드로 선택",
            description = "활성화 후, 아래 버튼을 눌러 기본 키보드를 '그림 및 특수문자 키보드'로 전환해주세요.",
            buttonText = "2단계: 키보드 선택하기",
            isCompleted = isSelected,
            onClick = {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info / Features Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 주요 기능 및 사용 팁",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = "• 5줄 쿼티 자판: 한국어(두벌식) 및 영어 자판이 내장되어 있습니다.", fontSize = 13.sp)
                Text(text = "• 특수문자 탭 (◷ ~ ☼): 7x4 배열의 특수문자 영역이 열리며, 좌우로 스와이프하여 탐색할 수 있습니다.", fontSize = 13.sp)
                Text(text = "• 그림판 (✎): 투명 배경 캔버스 위에 자유롭게 그린 후 [저장/전송] 시 즉시 메신저 입력창에 이미지가 전송됩니다.", fontSize = 13.sp)
                Text(text = "• 전송 내역 (⎘): 이전에 전송했던 그림 목록을 확인하고 터치 한 번으로 재전송할 수 있습니다.", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StepCard(
    stepNumber: Int,
    title: String,
    description: String,
    buttonText: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                // Status Badge
                if (isCompleted) {
                    Text(
                        text = "완료",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "설정 대기",
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f) 
                                     else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = buttonText, color = Color.White)
            }
        }
    }
}

// Utility functions to check keyboard activation status
private fun isKeyboardEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabledImes = imm.enabledInputMethodList
    return enabledImes.any { it.packageName == context.packageName }
}

private fun isKeyboardSelected(context: Context): Boolean {
    val currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    return currentIme != null && currentIme.contains(context.packageName)
}

@Preview(showBackground = true)
@Composable
fun KeyboardSetupScreenPreview() {
    CustomKeyboardTheme {
        KeyboardSetupScreen()
    }
}

