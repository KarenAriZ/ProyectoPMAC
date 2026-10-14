
package com.karenarias.proyectopmac.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.ui.theme.*

@Composable
fun MetroGoBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = CardWhite,
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp
        ),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 12.dp,
                    horizontal = 16.dp
                ),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetroGoNavItem(
                icon = Icons.Default.Home,
                label = "Inicio",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )

            MetroGoNavItem(
                icon = Icons.Default.CreditCard,
                label = "Mis tarjetas",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )

            MetroGoNavItem(
                icon = Icons.Default.History,
                label = "Historial",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )

            MetroGoNavItem(
                icon = Icons.Default.FlashOn,
                label = "Recargar",
                isSelected = selectedTab == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
private fun MetroGoNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected)
                PurplePrimary
            else PurpleDark.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected)
                PurplePrimary
            else PurpleDark.copy(alpha = 0.5f),
            fontWeight = if (isSelected)
                FontWeight.Bold
            else FontWeight.Normal
        )
    }
}