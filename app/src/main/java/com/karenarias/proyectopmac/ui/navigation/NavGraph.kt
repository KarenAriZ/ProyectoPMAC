package com.karenarias.proyectopmac.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import com.karenarias.proyectopmac.ui.claims.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import com.karenarias.proyectopmac.ui.components.MetroGoBottomBar
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.karenarias.proyectopmac.ui.auth.*
import com.karenarias.proyectopmac.ui.cards.AccountCardDetailScreen
import com.karenarias.proyectopmac.ui.home.HomeViewModel
import com.karenarias.proyectopmac.ui.map.RechargeMapRoute
import com.karenarias.proyectopmac.ui.cards.ScanCardScreen
import com.karenarias.proyectopmac.ui.cards.MyCardsScreen
import com.karenarias.proyectopmac.ui.recharge.RechargeScreen
import com.karenarias.proyectopmac.ui.recharge.UserRechargesViewModel
import com.karenarias.proyectopmac.ui.recharge.historyRecord
import com.karenarias.proyectopmac.ui.recharge.PaymentCardSummary
import com.karenarias.proyectopmac.ui.support.SupportScreen

/**
 * Grafo de navegación principal para MetroGo utilizando Navigation Compose.
 * Conecta Login, Registro, Recuperación de contraseña y la pantalla principal (Home).
 */
@Composable
fun MetroGoNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    claimsViewModel: ClaimsViewModel = viewModel(),
    rechargesViewModel: UserRechargesViewModel = viewModel()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val session = authViewModel.session
    var paymentCard by remember(session?.user?.id) { mutableStateOf<PaymentCardSummary?>(null) }
    fun navigateTab(index: Int) {
        val route = when (index) {
            0 -> "home"
            1 -> "my_cards"
            2 -> "history"
            3 -> "recharge_card"
            else -> return
        }
        if (currentRoute == route) return
        homeViewModel.onTabSelected(index)
        navController.navigate(route) {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }

    LaunchedEffect(session) {
        homeViewModel.bindSession(session)
        claimsViewModel.bindUser(session?.user?.id)
        rechargesViewModel.bindSession(session)
    }
    LaunchedEffect(rechargesViewModel.items) {
        claimsViewModel.replaceRecharges(session?.user?.id, rechargesViewModel.items.map { it.historyRecord() })
    }
    LaunchedEffect(rechargesViewModel.requiresLogin) {
        if (rechargesViewModel.requiresLogin) authViewModel.invalidateSession()
    }
    LaunchedEffect(homeViewModel.requiresLogin) {
        if (homeViewModel.requiresLogin) authViewModel.invalidateSession()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authViewModel.checkSessionExpiration()
                rechargesViewModel.refresh()
                homeViewModel.refreshData() // HU-004: saldo actualizado al volver a la app
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(session, currentRoute) {
        if (currentRoute in listOf("home", "claims", "claims/{claimId}", "history", "recharge/{rechargeId}", "new_claim/{rechargeId}", "scan_card", "recharge_card", "add_payment_card", "support", "card_status", "recharge_points", "my_cards") &&
            (session == null || session.isExpired())) {
            authViewModel.checkSessionExpiration()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val needsBottomBar = session != null && currentRoute in listOf(
        "scan_card", "my_cards", "card_status", "recharge_points", "support",
        "recharge/{rechargeId}", "new_claim/{rechargeId}", "add_payment_card"
    )
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (needsBottomBar) MetroGoBottomBar(
                selectedTab = when (currentRoute) {
                    "my_cards", "card_status", "scan_card" -> 1
                    "recharge/{rechargeId}", "new_claim/{rechargeId}" -> 2
                    "add_payment_card" -> 3
                    else -> -1
                },
                onTabSelected = ::navigateTab
            )
        }
    ) { navigationPadding ->
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier.padding(navigationPadding)
    ) {
        // Pantalla de Login
        composable("login") {
            DisposableEffect(Unit) { onDispose { authViewModel.cancelLogin() } }
            LoginScreen(
                email = authViewModel.loginEmail,
                onEmailChange = { authViewModel.onLoginEmailChanged(it) },
                password = authViewModel.loginPassword,
                onPasswordChange = { authViewModel.onLoginPasswordChanged(it) },
                onLoginClick = {
                    authViewModel.login(
                        onSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onNavigateToForgotPassword = {
                    navController.navigate("forgot_password")
                },
                isLoading = authViewModel.loginIsLoading,
                errorMessage = authViewModel.loginErrorMessage
            )
        }

        // Pantalla de Registro
        composable("register") {
            RegisterScreen(
                firstName = authViewModel.registerFirstName,
                onFirstNameChange = { authViewModel.onRegisterFirstNameChanged(it) },
                lastName = authViewModel.registerLastName,
                onLastNameChange = { authViewModel.onRegisterLastNameChanged(it) },
                email = authViewModel.registerEmail,
                onEmailChange = { authViewModel.onRegisterEmailChanged(it) },
                password = authViewModel.registerPassword,
                onPasswordChange = { authViewModel.onRegisterPasswordChanged(it) },
                confirmPassword = authViewModel.registerConfirmPassword,
                onConfirmPasswordChange = { authViewModel.onRegisterConfirmPasswordChanged(it) },
                onRegisterClick = { authViewModel.register() },
                registrationSucceeded = authViewModel.registerSucceeded,
                onRegistrationConfirmed = {
                    authViewModel.acknowledgeRegistration()
                    navController.popBackStack("login", false)
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                isLoading = authViewModel.registerIsLoading,
                errorMessage = authViewModel.registerErrorMessage
            )
        }

        // Pantalla de Recuperación de Contraseña
        composable("forgot_password") {
            ForgotPasswordScreen(
                email = authViewModel.forgotEmail,
                onEmailChange = { authViewModel.onForgotEmailChanged(it) },
                onSubmitClick = {
                    authViewModel.sendRecoveryEmail()
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                isLoading = authViewModel.forgotIsLoading,
                errorMessage = authViewModel.forgotErrorMessage,
                successMessage = authViewModel.forgotSuccessMessage
            )
        }

        // Pantalla Principal (Home)
        composable("home") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
            // HU-004: al volver a Inicio (por ejemplo desde "Mi tarjeta") se vuelve a leer el saldo.
            LaunchedEffect(Unit) { homeViewModel.refreshData() }
            var showProfile by remember { mutableStateOf(false) }
            val context = LocalContext.current
            var showContact by remember { mutableStateOf(false) }
            if (showProfile) {
                UserProfileDialog(session.displayName, session.user.correoElectronico.orEmpty()) {
                    showProfile = false
                }
            }
            if (showContact) ContactUnavailableDialog { showContact = false }
            HomeScreen(
                userName = session.displayName,
                cardNumber = homeViewModel.cardNumber,
                cardType = homeViewModel.cardType,
                cardBalance = homeViewModel.cardBalance,
                recharges = rechargesViewModel.items.map { item ->
                    val record = item.historyRecord()
                    RechargeUiModel(record.id, record.amount, record.date, record.status, true)
                },
                selectedTab = homeViewModel.selectedTab,
                isLowBalance = homeViewModel.isLowBalance,
                cardsLoading = homeViewModel.isLoading,
                cardsError = homeViewModel.errorMessage,
                onRetryCards = homeViewModel::refreshData,
                onAddCard = { navController.navigate("scan_card") { launchSingleTop = true } },
                onBalanceClick = { navController.navigate("card_status") { launchSingleTop = true } }, // HU-004
                onRechargePointsClick = { navController.navigate("recharge_points") { launchSingleTop = true } }, // HU-006
                onTabSelected = ::navigateTab,
                onRechargeClick = {
                    navigateTab(3)
                },
                onMyCardsClick = {
                    navController.navigate("my_cards") { launchSingleTop = true }
                },
                onViewAllRechargesClick = {
                    navController.navigate("history") { launchSingleTop = true }
                },
                onProfileClick = { showProfile = true },
                onContactClick = {
                    try {
                        navController.navigate("support") { launchSingleTop = true }
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+5112039001")))
                    } catch (_: ActivityNotFoundException) {
                        showContact = true
                    } catch (_: SecurityException) {
                        showContact = true
                    }
                },
                onClaimsClick = { navController.navigate("claims") { launchSingleTop = true } },
                onLogoutClick = {
                    homeViewModel.onTabSelected(0)
                    authViewModel.logout()
                }
            )
            }
        }
        composable("claims") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                val selectedCard by claimsViewModel.selectedCard.collectAsState()
                val selectedFilter by claimsViewModel.selectedFilter.collectAsState()
                ClaimsScreen(
                    onNavigate = ::navigateTab,
                    onHistory = { navController.navigate("history") { popUpTo("home"); launchSingleTop = true } },
                    claims = claimsViewModel.claims,
                    cards = claimsViewModel.cards,
                    selectedCard = selectedCard,
                    selectedFilter = ClaimFilter.valueOf(selectedFilter),
                    onCardSelected = claimsViewModel::selectCard,
                    onFilterSelected = claimsViewModel::selectFilter,
                    onClaimClick = { navController.navigate("claims/$it") },
                    onBack = { navController.popBackStack() },
                    onHome = { navController.popBackStack("home", false) }
                )
            }
        }
        composable("claims/{claimId}", arguments = listOf(navArgument("claimId") { type = NavType.StringType })) { entry ->
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                ClaimDetailScreen(
                    onNavigate = ::navigateTab,
                    onHistory = { navController.navigate("history") { popUpTo("home"); launchSingleTop = true } },
                    claim = claimsViewModel.findClaim(entry.arguments?.getString("claimId")),
                    onBack = { navController.popBackStack() },
                    onHome = { navController.popBackStack("home", false) },
                    onClaims = { navController.popBackStack("claims", false) }
                )
            }
        }
        composable("history") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                var showProfile by remember { mutableStateOf(false) }
                var showContact by remember { mutableStateOf(false) }
                val context = LocalContext.current
                if (showProfile) UserProfileDialog(session.displayName, session.user.correoElectronico.orEmpty()) { showProfile = false }
                if (showContact) ContactUnavailableDialog { showContact = false }
                LaunchedEffect(Unit) { rechargesViewModel.refresh() }
                RechargeHistoryScreen(
                    isLoading = rechargesViewModel.isLoading,
                    errorMessage = rechargesViewModel.historyError,
                    onRefresh = rechargesViewModel::refresh,
                    onNavigate = ::navigateTab,
                    records = claimsViewModel.rechargeRecords,
                    onRecharge = { navController.navigate("recharge/$it") { launchSingleTop = true } },
                    onBack = { navController.popBackStack() },
                    onHome = { homeViewModel.onTabSelected(0); navController.popBackStack("home", false) },
                    onProfile = { showProfile = true },
                    onContact = {
                        try { navController.navigate("support") { launchSingleTop = true }
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+5112039001"))) }
                        catch (_: ActivityNotFoundException) { showContact = true }
                        catch (_: SecurityException) { showContact = true }
                    },
                    onClaims = { navController.navigate("claims") { launchSingleTop = true } },
                    onLogout = { authViewModel.logout() }
                )
            }
        }
        composable("recharge/{rechargeId}", arguments = listOf(navArgument("rechargeId") { type = NavType.StringType })) { entry ->
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                val id = entry.arguments?.getString("rechargeId")
                val existingClaim = id?.let { claimsViewModel.claimForRecharge(it) }
                RechargeDetailScreen(
                    record = claimsViewModel.findRecharge(id), existingClaim = existingClaim,
                    onBack = { navController.popBackStack() },
                    onClaim = {
                        if (existingClaim != null) {
                            navController.navigate("claims") { launchSingleTop = true }
                            navController.navigate("claims/$existingClaim")
                        } else if (id != null) navController.navigate("new_claim/$id") { launchSingleTop = true }
                    }
                )
            }
        }
        composable("new_claim/{rechargeId}", arguments = listOf(navArgument("rechargeId") { type = NavType.StringType })) { entry ->
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                val id = entry.arguments?.getString("rechargeId")
                RegisterClaimScreen(
                    record = claimsViewModel.findRecharge(id),
                    onBack = { navController.popBackStack() },
                    onSubmit = { reason, description ->
                        if (id != null && authViewModel.session?.isExpired() == false)
                            claimsViewModel.registerClaim(id, reason, description) else null
                    },
                    onViewClaims = {
                        navController.navigate("claims") {
                            popUpTo("history") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        composable("scan_card") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                ScanCardScreen(
                    isSaving = homeViewModel.isSaving,
                    saveError = homeViewModel.errorMessage,
                    onSaveCard = { card, onSaved -> homeViewModel.addCard(card.number, card.alias, card.type, onSaved) },
                    onBack = { navController.popBackStack() },
                    onCardSaved = { card ->
                        homeViewModel.onTabSelected(0)
                        navController.popBackStack("home", false)
                    }
                )
            }
        }
        composable("support") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                SupportScreen(onBack = { navController.popBackStack() })
            }
        }
        composable("add_payment_card") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                var showProfile by remember { mutableStateOf(false) }
                if (showProfile) UserProfileDialog(session.displayName, session.user.correoElectronico.orEmpty()) { showProfile = false }
                AddPaymentCardScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { summary ->
                        paymentCard = summary
                        navController.popBackStack()
                    },
                    onCards = { navigateTab(1) },
                    onProfile = { showProfile = true }
                )
            }
        }
        composable("recharge_card") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                var showProfile by remember { mutableStateOf(false) }
                if (showProfile) UserProfileDialog(session.displayName, session.user.correoElectronico.orEmpty()) { showProfile = false }
                RechargeScreen(
                    paymentCard = paymentCard,
                    onAddPaymentCard = { navController.navigate("add_payment_card") { launchSingleTop = true } },
                    onProfileClick = { showProfile = true },
                    cardNumber = homeViewModel.cardNumber,
                    allowRecharge = true,
                    isSavingRecharge = rechargesViewModel.isSaving,
                    saveError = rechargesViewModel.saveError,
                    pending = rechargesViewModel.pending,
                    receipt = rechargesViewModel.receipt,
                    onSubmit = { number, amount, method ->
                        rechargesViewModel.submit(number, amount, method) { homeViewModel.refreshData() }
                    },
                    cards = homeViewModel.cards,
                    onCardSelected = homeViewModel::selectCard,
                    onCardsClick = { navController.navigate("my_cards") { launchSingleTop = true } },
                    onTabSelected = ::navigateTab
                )
            }
        }

        // HU-004: saldo y estado de la tarjeta + alerta de saldo bajo
        composable("card_status") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                AccountCardDetailScreen(
                    card = homeViewModel.selectedCard,
                    isLoading = homeViewModel.isLoading,
                    errorMessage = homeViewModel.errorMessage,
                    onBack = { navController.popBackStack() },
                    onCards = { navController.navigate("my_cards") { launchSingleTop = true } },
                    onMap = { navController.navigate("recharge_points") { launchSingleTop = true } }
                )
            }
        }

        // HU-006: mapa de puntos de recarga física + alertas de paraderos próximos
        composable("recharge_points") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                RechargeMapRoute(
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("my_cards") {
            if (session != null && !session.isExpired() && homeViewModel.sessionUserId == session.user.id) {
                LaunchedEffect(Unit) { homeViewModel.refreshData() }
                MyCardsScreen(
                    cards = homeViewModel.cards,
                    isLoading = homeViewModel.isLoading,
                    isSaving = homeViewModel.isSaving,
                    errorMessage = homeViewModel.errorMessage,
                    onRefresh = homeViewModel::refreshData,
                    onSelectCard = { number ->
                        homeViewModel.selectCard(number)
                        navController.navigate("card_status") { launchSingleTop = true }
                    },
                    onChangeState = homeViewModel::changeCardState,
                    onBack = {
                        homeViewModel.refreshData()
                        navController.popBackStack()
                    },
                    onAddCard = { navController.navigate("scan_card") { launchSingleTop = true } }
                )
            }
        }
    }
    }
}
