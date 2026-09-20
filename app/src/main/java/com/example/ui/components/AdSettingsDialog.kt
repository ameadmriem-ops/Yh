package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ads.AdConfig

@Composable
fun AdSettingsDialog(
    isTestAdMode: Boolean,
    onToggleTestAdMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعدادات إعلانات AdMob", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("ad_settings_content")
            ) {
                // تفعيل / تعطيل IS_TEST_AD
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTestAdMode) Color(0xFFFFF9E6) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "وضع الاختبار (IS_TEST_AD)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (isTestAdMode) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BoxBadge("TEST AD")
                                }
                            }
                            Text(
                                text = if (isTestAdMode) {
                                    "يستخدم إعلانات Rewarded Test Ad الرسمية من Google"
                                } else {
                                    "يستخدم الإعلانات ومعرفات AdMob الحقيقية"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isTestAdMode,
                            onCheckedChange = onToggleTestAdMode,
                            modifier = Modifier.testTag("test_ad_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // المعرف الحالي المستخدم
                Text(
                    text = "المعرف النشط حالياً:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AdConfig.getActiveRewardedAdUnitId(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isTestAdMode) Color(0xFF856404) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // تفاصيل المعرفات المحفوظة
                Text(
                    text = "معرفات الاختبار الرسمية (Google Test IDs):",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Test App ID:\n${AdConfig.TEST_ADMOB_APP_ID}\n\nRewarded Test Ad Unit ID:\n${AdConfig.TEST_REWARDED_AD_UNIT_ID}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "المعرفات الحقيقية (Real IDs - إعدادات منفصلة):",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Real App ID:\n${AdConfig.REAL_ADMOB_APP_ID}\n\nReal Ad Unit ID:\n${AdConfig.REAL_AD_UNIT_ID}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_ad_settings_button")
            ) {
                Text("إغلاق")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun BoxBadge(text: String) {
    Text(
        text = text,
        color = Color(0xFF856404),
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFFF3CD))
            .border(1.dp, Color(0xFFFFC107), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}
