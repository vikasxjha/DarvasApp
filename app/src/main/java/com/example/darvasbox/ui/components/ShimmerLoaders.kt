package com.example.darvasbox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return this.background(Color.Gray.copy(alpha = alpha))
}

@Composable
fun StockAnalysisShimmerLoader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Search section shimmer
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search field shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Button shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Analysis result shimmer
        StockAnalysisCardShimmer()

        Spacer(modifier = Modifier.height(16.dp))

        // Recent stocks shimmer
        repeat(3) {
            StockItemShimmer()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun StockAnalysisCardShimmer(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with symbol and signal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Price and box info section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Current price label
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Price value
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Change percentage
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Darvas box label
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Box high
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Box low
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Range
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Signal explanation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Multiple explanation lines
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (it == 0) 1f else 0.7f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Volume info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun StockItemShimmer(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }

            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
fun AnalysisResultsShimmerLoader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Header shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary cards shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stock list shimmer
        repeat(5) {
            StockItemShimmer()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
