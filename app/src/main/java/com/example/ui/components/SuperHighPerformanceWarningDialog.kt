package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.extreme.ExtremeWarningSpec
import com.example.ui.theme.CrimsonTurbo
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SuperHighPerformanceWarningDialog(
    visible: Boolean,
    showContinueAction: Boolean = true,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    if (!visible) return

    val accent = LocalTurboAccent.current.accent
    val dialogShape = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp, topEnd = 6.dp, bottomStart = 6.dp)

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .testTag("super_perf_warning_dialog")
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(dialogShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MidnightSurface, ObsidianVoid)
                    )
                )
                .border(1.4.dp, CyberAmber.copy(alpha = 0.85f), dialogShape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberAmber.copy(alpha = 0.16f))
                        .border(1.dp, CyberAmber.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = CyberAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = ExtremeWarningSpec.TITLE,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        letterSpacing = 1.sp
                    ),
                    color = CyberAmber
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = ExtremeWarningSpec.INTRO,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberElevated.copy(alpha = 0.88f))
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "POTENTIAL EFFECTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = accent
                    )
                    ExtremeWarningSpec.EFFECTS.forEach { effect ->
                        Text(
                            text = effect,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldBright.copy(alpha = 0.10f))
                        .border(1.dp, EmeraldBright.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = ExtremeWarningSpec.IMPORTANT_NOTICE,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("super_perf_warning_cancel_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text(
                        text = ExtremeWarningSpec.BUTTON_CANCEL,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }

                if (showContinueAction) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .weight(1.45f)
                            .minimumInteractiveComponentSize()
                            .testTag("super_perf_warning_continue_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonTurbo,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = ExtremeWarningSpec.BUTTON_CONTINUE,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
