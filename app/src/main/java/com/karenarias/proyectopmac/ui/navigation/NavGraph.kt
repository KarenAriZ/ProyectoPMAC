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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.karenarias.proyectopmac.ui.auth.*
import com.karenarias.proyectopmac.ui.home.HomeViewModel

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
    claimsViewModel: ClaimsViewModel = viewModel()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val session = authViewModel.session
    LaunchedEffect(session?.user?.id) { claimsViewModel.bindUser(session?.user?.id) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) authViewModel.checkSessionExpiration()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(session, currentRoute) {
        if (currentRoute in listOf("home", "claims", "claims/{claimId}", "history", "recharge/{rechargeId}", "new_claim/{rechargeId}") &&
            (session == null || session.isExpired())) {
            authViewModel.checkSessionExpiration()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
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
            if (session != null && !session.isExpired()) {
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
                recharges = homeViewModel.recharges,
                selectedTab = homeViewModel.selectedTab,
                onTabSelected = { if (it == 2) navController.navigate("history") { launchSingleTop = true } else homeViewModel.onTabSelected(it) },
                onRechargeClick = {
                    // Acción de recarga (fases posteriores)
                },
                onMyCardsClick = {
                    // Acción de tarjetas (fases posteriores)
                },
                onViewAllRechargesClick = {
                    navController.navigate("history") { launchSingleTop = true }
                },
                onProfileClick = { showProfile = true },
                onContactClick = {
                    try {
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
            if (session != null && !session.isExpired()) {
                val selectedCard by claimsViewModel.selectedCard.collectAsState()
                val selectedFilter by claimsViewModel.selectedFilter.collectAsState()
                ClaimsScreen(
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
            if (session != null && !session.isExpired()) {
                ClaimDetailScreen(
                    onHistory = { navController.navigate("history") { popUpTo("home"); launchSingleTop = true } },
                    claim = claimsViewModel.findClaim(entry.arguments?.getString("claimId")),
                    onBack = { navController.popBackStack() },
                    onHome = { navController.popBackStack("home", false) },
                    onClaims = { navController.popBackStack("claims", false) }
                )
            }
        }
        composable("history") {
            if (session != null && !session.isExpired()) {
                var showProfile by remember { mutableStateOf(false) }
                var showContact by remember { mutableStateOf(false) }
                val context = LocalContext.current
                if (showProfile) UserProfileDialog(session.displayName, session.user.correoElectronico.orEmpty()) { showProfile = false }
                if (showContact) ContactUnavailableDialog { showContact = false }
                RechargeHistoryScreen(
                    records = RechargeHistorySamples.records,
                    onRecharge = { navController.navigate("recharge/$it") { launchSingleTop = true } },
                    onBack = { navController.popBackStack() },
                    onHome = { homeViewModel.onTabSelected(0); navController.popBackStack("home", false) },
                    onProfile = { showProfile = true },
                    onContact = {
                        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+5112039001"))) }
                        catch (_: ActivityNotFoundException) { showContact = true }
                        catch (_: SecurityException) { showContact = true }
                    },
                    onClaims = { navController.navigate("claims") { launchSingleTop = true } },
                    onLogout = { authViewModel.logout() }
                )
            }
        }
        composable("recharge/{rechargeId}", arguments = listOf(navArgument("rechargeId") { type = NavType.StringType })) { entry ->
            if (session != null && !session.isExpired()) {
                val id = entry.arguments?.getString("rechargeId")
                val existingClaim = id?.let { claimsViewModel.claimForRecharge(it) }
                RechargeDetailScreen(
                    record = RechargeHistorySamples.find(id), existingClaim = existingClaim,
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
            if (session != null && !session.isExpired()) {
                val id = entry.arguments?.getString("rechargeId")
                RegisterClaimScreen(
                    record = RechargeHistorySamples.find(id),
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
    }
}
