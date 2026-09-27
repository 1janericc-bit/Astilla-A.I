package com.example.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AstillaTopAppBar
import com.example.ui.components.SupportDeveloperCard
import com.example.ui.theme.AstillaAmberTertiary
import com.example.ui.theme.AstillaCyanPrimary

@Composable
fun SupportDeveloperScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AstillaTopAppBar(
                title = "Support Astilla Softwares",
                subtitle = "Developer Contribution",
                isOffline = true,
                onOpenSupport = {}
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AstillaAmberTertiary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolunteerActivism,
                                    contentDescription = null,
                                    tint = AstillaAmberTertiary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Thank You for Supporting!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Astilla Softwares Independent Project",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "Astilla A.I is designed to be free, private, and fully operable offline with no subscription paywalls or hidden limits. Your kind contribution directly funds research into on-device neural indexing, new summarization heuristics, and ongoing maintenance.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item {
                SupportDeveloperCard()
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Why Support Astilla A.I?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        WhySupportItem(
                            icon = Icons.Default.Lock,
                            title = "100% Privacy Focused",
                            desc = "Your documents, training data, and chat history remain securely stored inside your device's local database."
                        )

                        WhySupportItem(
                            icon = Icons.Default.Bolt,
                            title = "Instant Offline Execution",
                            desc = "No internet required to summarize books, analyze docs, or query trained knowledge."
                        )

                        WhySupportItem(
                            icon = Icons.Default.AllInclusive,
                            title = "Unrestricted Personal Intelligence",
                            desc = "A flexible AI assistant designed to adapt to your style and grow alongside you."
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WhySupportItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AstillaCyanPrimary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
