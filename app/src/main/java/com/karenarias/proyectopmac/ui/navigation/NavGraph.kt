package com.karenarias.proyectopmac.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
    homeViewModel: HomeViewModel = viewModel()
) {
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
            val session = authViewModel.session
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) authViewModel.checkSessionExpiration()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            LaunchedEffect(session) {
                if (session == null || session.isExpired()) {
                    authViewModel.checkSessionExpiration()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            if (session != null && !session.isExpired()) {
            HomeScreen(
                userName = session.displayName,
                cardNumber = homeViewModel.cardNumber,
                cardType = homeViewModel.cardType,
                cardBalance = homeViewModel.cardBalance,
                recharges = homeViewModel.recharges,
                selectedTab = homeViewModel.selectedTab,
                onTabSelected = { homeViewModel.onTabSelected(it) },
                onRechargeClick = {
                    // Acción de recarga (fases posteriores)
                },
                onMyCardsClick = {
                    // Acción de tarjetas (fases posteriores)
                },
                onViewAllRechargesClick = {
                    // Acción de historial (fases posteriores)
                },
                onProfileClick = {
                    // Cierre local: el token del servidor conserva su vencimiento.
                    authViewModel.logout()
                }
            )
            }
        }
    }
}



