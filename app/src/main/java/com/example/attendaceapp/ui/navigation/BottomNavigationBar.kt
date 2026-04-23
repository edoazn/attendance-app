package com.example.attendaceapp.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.attendaceapp.R

@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    currentDestination: NavDestination?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    val fabSize = 56.dp
    val fabLift = 16.dp
    val cutoutPadding = 8.dp
    val barColor = colorResource(id = R.color.white)
    val density = LocalDensity.current
    val cutoutRadiusPx by remember(density, fabSize, cutoutPadding) {
        mutableFloatStateOf(
            with(density) { (fabSize / 2 + cutoutPadding).toPx() }
        )
    }
    val curveControlSmallPx by remember(density) {
        mutableFloatStateOf(with(density) { 2.dp.toPx() })
    }
    val curveControlLargePx by remember(density) {
        mutableFloatStateOf(with(density) { 6.dp.toPx() })
    }
    val notchShoulderPx by remember(density) {
        mutableFloatStateOf(with(density) { 12.dp.toPx() })
    }

    val barShape =
        remember(cutoutRadiusPx, curveControlSmallPx, curveControlLargePx, notchShoulderPx) {
            GenericShape { size, _ ->
                val width = size.width
                val height = size.height
                val radius = cutoutRadiusPx
                val notchCenterX = width / 2f

                moveTo(0f, 0f)
                lineTo(notchCenterX - radius - notchShoulderPx, 0f)
                cubicTo(
                    notchCenterX - radius + curveControlSmallPx,
                    0f,
                    notchCenterX - radius + curveControlLargePx,
                    radius,
                    notchCenterX,
                    radius,
                )
                cubicTo(
                    notchCenterX + radius - curveControlLargePx,
                    radius,
                    notchCenterX + radius - curveControlSmallPx,
                    0f,
                    notchCenterX + radius + notchShoulderPx,
                    0f,
                )
                lineTo(width, 0f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
        }

    val attendanceItem = items.firstOrNull { it.route == BottomNavItem.Attendance.route }

    Box {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                )
                .clip(barShape)
                .background(barColor)
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
            ) {
                val centerIndex = items.size / 2
                items.forEachIndexed { index, item ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == item.route } == true
                    val itemCenterNudge = when {
                        index < centerIndex -> 6.dp
                        index > centerIndex -> (-6).dp
                        else -> 0.dp
                    }

                    if (item.route == BottomNavItem.Attendance.route) {
                        // Placeholder for the FAB cutout - invisible and non-interactive
                        NavigationBarItem(
                            selected = false,
                            onClick = {},
                            enabled = false,
                            icon = { Spacer(modifier = Modifier.size(24.dp)) },
                            label = { Text(text = "", fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                disabledIconColor = barColor,
                                disabledTextColor = barColor,
                            )
                        )
                    } else {
                        val itemIconScale by animateFloatAsState(
                            targetValue = if (selected) 1.06f else 1f,
                            animationSpec = tween(durationMillis = 220),
                            label = "bottom_nav_icon_scale_${item.route}",
                        )
                        val itemLabelAlpha by animateFloatAsState(
                            targetValue = if (selected) 1f else 0.85f,
                            animationSpec = tween(durationMillis = 220),
                            label = "bottom_nav_label_alpha_${item.route}",
                        )

                        NavigationBarItem(
                            modifier = Modifier.offset(x = itemCenterNudge),
                            selected = selected,
                            onClick = { onItemClick(item) },
                            icon = {
                                Icon(
                                    painter = painterResource(id = item.icon),
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            scaleX = itemIconScale
                                            scaleY = itemIconScale
                                        }
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.graphicsLayer { alpha = itemLabelAlpha },
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colorResource(id = R.color.primary_color),
                                selectedTextColor = colorResource(id = R.color.primary_color),
                                unselectedIconColor = colorResource(id = R.color.gray_400),
                                unselectedTextColor = colorResource(id = R.color.gray_400),
                                indicatorColor = colorResource(id = R.color.primary_color).copy(
                                    alpha = 0.1f
                                )
                            )
                        )
                    }
                }
            }
        }

        if (attendanceItem != null) {
            val attendanceSelected =
                currentDestination?.hierarchy?.any { it.route == attendanceItem.route } == true
            val fabScale by animateFloatAsState(
                targetValue = if (attendanceSelected) 1.04f else 1f,
                animationSpec = tween(durationMillis = 220),
                label = "attendance_fab_scale",
            )
            val fabAnimatedOffset by animateDpAsState(
                targetValue = if (attendanceSelected) -(fabLift + 2.dp) else -fabLift,
                animationSpec = tween(durationMillis = 220),
                label = "attendance_fab_offset",
            )
            val fabAnimatedElevation by animateDpAsState(
                targetValue = if (attendanceSelected) 9.dp else 6.dp,
                animationSpec = tween(durationMillis = 220),
                label = "attendance_fab_elevation",
            )

            FloatingActionButton(
                onClick = { onItemClick(attendanceItem) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = fabAnimatedOffset)
                    .size(fabSize)
                    .graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    },
                shape = CircleShape,
                containerColor = colorResource(id = R.color.primary_color),
                contentColor = colorResource(id = R.color.white),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = fabAnimatedElevation,
                    pressedElevation = fabAnimatedElevation + 2.dp,
                )
            ) {
                Icon(
                    painter = painterResource(id = attendanceItem.icon),
                    contentDescription = attendanceItem.title,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}