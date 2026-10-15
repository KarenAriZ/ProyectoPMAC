package com.karenarias.proyectopmac.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.ui.theme.*

/**
 * Modelo de datos simple para representar una recarga en la lista de últimas recargas.
 */
data class RechargeUiModel(
    val id: String,
    val amount: String,
    val date: String,
    val status: String,
    val isSuccess: Boolean
)

/**
 * Pantalla principal (Home) stateless para MetroGo.
 * Contiene TopBar, tarjeta de saldo, accesos rápidos, últimas recargas y BottomBar de navegación.
 */
@Composable
fun HomeScreen(
    userName: String,
    cardNumber: String,
    cardType: String,
    cardBalance: String,
    recharges: List<RechargeUiModel>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRechargeClick: () -> Unit,
    onMyCardsClick: () -> Unit,
    onViewAllRechargesClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var balanceVisible by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCream,
        topBar = {
            // TopBar superior limpia
            Surface(
                color = BackgroundCream,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "Logo MetroGo",
                            tint = PurplePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "MetroGo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleDark
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextButton(onClick = onMyCardsClick) {
                            Text(
                                text = "Tarjetas",
                                color = PurpleDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Botón de perfil circular
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MauveAccent)
                                .clickable { onProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = CardWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // BottomBar inferior con esquinas redondeadas superiores
            Surface(
                color = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavTab(
                        icon = Icons.Default.Home,
                        label = "Inicio",
                        isSelected = selectedTab == 0,
                        onClick = { onTabSelected(0) }
                    )
                    BottomNavTab(
                        icon = Icons.Default.CreditCard,
                        label = "Mis tarjetas",
                        isSelected = selectedTab == 1,
                        onClick = { onTabSelected(1) }
                    )
                    BottomNavTab(
                        icon = Icons.Default.History,
                        label = "Historial",
                        isSelected = selectedTab == 2,
                        onClick = { onTabSelected(2) }
                    )
                    BottomNavTab(
                        icon = Icons.Default.FlashOn,
                        label = "Recargar",
                        isSelected = selectedTab == 3,
                        onClick = { onTabSelected(3) }
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Saludo personalizado
                Text(
                    text = "Hola, $userName 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleDark
                )
                Text(
                    text = "Tu tarjeta del Metropolitano",
                    fontSize = 14.sp,
                    color = PurpleDark.copy(alpha = 0.7f)
                )
            }

            // Tarjeta de saldo principal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PurpleDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = MauveAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = cardNumber,
                                    color = CardWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Surface(
                                color = PurplePrimary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = cardType,
                                    color = CardWhite,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Saldo disponible",
                                    color = MauveAccent,
                                    fontSize = 13.sp
                                )
                                IconButton(
                                    onClick = { balanceVisible = !balanceVisible },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (balanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Alternar visibilidad de saldo",
                                        tint = MauveAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (balanceVisible) cardBalance else "S/. •••••",
                                color = CardWhite,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Botones de acción rápida
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickActionCard(
                        title = "Recargar tarjeta",
                        imageVector = Icons.Default.AddCard,
                        modifier = Modifier.weight(1f),
                        onClick = onRechargeClick
                    )
                    QuickActionCard(
                        title = "Mis tarjetas",
                        imageVector = Icons.Default.CreditCard,
                        modifier = Modifier.weight(1f),
                        onClick = onMyCardsClick
                    )
                }
            }

            // Sección Últimas recargas
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Últimas recargas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleDark
                    )
                    TextButton(onClick = onViewAllRechargesClick) {
                        Text(
                            text = "Ver todas >",
                            color = PurplePrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(recharges) { recharge ->
                RechargeItemCard(recharge = recharge)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundCream),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleDark
            )
        }
    }
}

@Composable
fun RechargeItemCard(
    recharge: RechargeUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (recharge.isSuccess) MauveAccent.copy(alpha = 0.3f) else ErrorWine.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (recharge.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (recharge.isSuccess) PurplePrimary else ErrorWine,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Recarga MetroGo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleDark
                    )
                    Text(
                        text = recharge.date,
                        fontSize = 12.sp,
                        color = PurpleDark.copy(alpha = 0.6f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recharge.amount,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleDark
                )
                Surface(
                    color = if (recharge.isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = recharge.status,
                        fontSize = 11.sp,
                        color = if (recharge.isSuccess) Color(0xFF2E7D32) else ErrorWine,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavTab(
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
            tint = if (isSelected) PurplePrimary else PurpleDark.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) PurplePrimary else PurpleDark.copy(alpha = 0.5f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ProyectoPMACTheme {
        val sampleRecharges = listOf(
            RechargeUiModel("1", "+ S/. 20.00", "Hoy, 14:30", "Recarga exitosa", true),
            RechargeUiModel("2", "+ S/. 10.00", "Ayer, 09:15", "Recarga exitosa", true),
            RechargeUiModel("3", "+ S/. 15.00", "12 Set, 18:45", "Error en recarga", false),
            RechargeUiModel("4", "+ S/. 8.00", "09 Set, 10:27", "Recarga exitosa", true)
        )

        var selectedTab by remember { mutableStateOf(0) }

        HomeScreen(
            userName = "Karen Arias",
            cardNumber = "8754785478",
            cardType = "Estudiante",
            cardBalance = "S/. 17.30",
            recharges = sampleRecharges,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onRechargeClick = {},
            onMyCardsClick = {},
            onViewAllRechargesClick = {},
            onProfileClick = {}
        )
    }
}
