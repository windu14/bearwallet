package com.example.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.presentation.MainViewModel
import com.example.presentation.UiMessage
import com.example.presentation.screens.*
import com.example.ui.theme.*

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector = Icons.Default.Home) {
  object Home : Screen("home", "Beranda", Icons.Default.Home)
  object Exchange : Screen("exchange", "Pasar", Icons.Default.CurrencyExchange)
  object Banking : Screen("banking", "Bank", Icons.Default.AccountBalance)
  object Activity : Screen("activity", "Aktivitas", Icons.Default.ReceiptLong)
  object Profile : Screen("profile", "Profil", Icons.Default.Person)

  object TransferInput : Screen("transfer_input", "Transfer")
  object TransferConfirm : Screen("transfer_confirm/{recipient}/{amount}/{note}", "Confirm Transfer") {
    fun createRoute(recipient: String, amount: Long, note: String) =
      "transfer_confirm/$recipient/$amount/${if (note.isEmpty()) "-" else note}"
  }
  object TransferReceipt : Screen("transfer_receipt", "Receipt")

  object LoanApply : Screen("loan_apply", "Apply Loan")
  object LoanProcessing : Screen("loan_processing/{appId}", "Loan Processing") {
    fun createRoute(appId: String) = "loan_processing/$appId"
  }

  object AdminCentralBank : Screen("admin_central_bank", "Central Bank")
  object Notifications : Screen("notifications", "Notifications")

  object AuthSignIn : Screen("auth_signin", "Sign In")
  object AuthSignUp : Screen("auth_signup", "Sign Up")
  object AuthForgotPassword : Screen("auth_forgot_password", "Forgot Password")
}

// Streamlined bottom bar (No duplicate icons that are already on Home card)
val bottomNavScreens = listOf(
  Screen.Home,
  Screen.Activity,
  Screen.Profile
)

@Composable
fun BearWalletApp(
  viewModel: MainViewModel = viewModel()
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val snackbarHostState = remember { SnackbarHostState() }
  val uiMessage by viewModel.uiMessage.collectAsState()

  LaunchedEffect(uiMessage) {
    uiMessage?.let {
      when (it) {
        is UiMessage.Success -> snackbarHostState.showSnackbar(it.message)
        is UiMessage.Error -> snackbarHostState.showSnackbar("Error: ${it.message}")
      }
      viewModel.clearUiMessage()
    }
  }

  val showBottomBar = bottomNavScreens.any { it.route == currentRoute }

  val currentUser by viewModel.currentUser.collectAsState()
  val startDestination = if (currentUser == null) Screen.AuthSignIn.route else Screen.Home.route

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      AnimatedVisibility(
        visible = showBottomBar,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
      ) {
        NavigationBar(
          containerColor = PureWhite,
          tonalElevation = 8.dp,
          modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
          bottomNavScreens.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
              icon = {
                Icon(
                  imageVector = screen.icon,
                  contentDescription = screen.title
                )
              },
              label = {
                Text(
                  text = screen.title,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              selected = isSelected,
              onClick = {
                if (currentRoute != screen.route) {
                  navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                      saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                  }
                }
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ForestGreenDark,
                selectedTextColor = ForestGreenPrimary,
                indicatorColor = MintContainer,
                unselectedIconColor = SageMuted,
                unselectedTextColor = SageMuted
              ),
              modifier = Modifier.testTag("nav_tab_${screen.route}")
            )
          }
        }
      }
    },
    containerColor = WarmWhite,
    modifier = Modifier.fillMaxSize()
  ) { paddingValues ->
    NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = Modifier.padding(paddingValues)
    ) {
      // 1. Home
      composable(Screen.Home.route) {
        HomeDashboardScreen(
          viewModel = viewModel,
          onNavigateToTransfer = { navController.navigate(Screen.TransferInput.route) },
          onNavigateToExchange = { navController.navigate(Screen.Exchange.route) },
          onNavigateToBanking = { navController.navigate(Screen.Banking.route) },
          onNavigateToActivity = { navController.navigate(Screen.Activity.route) },
          onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
        )
      }

      // 2. Exchange (Accessible directly via Home Card)
      composable(Screen.Exchange.route) {
        ExchangeMarketScreen(
          viewModel = viewModel,
          onNavigateBack = { navController.popBackStack() }
        )
      }

      // 3. Banking (Accessible directly via Home Card)
      composable(Screen.Banking.route) {
        BankingOverviewScreen(
          viewModel = viewModel,
          onNavigateToApplyLoan = { navController.navigate(Screen.LoanApply.route) },
          onNavigateToProcessing = { appId -> navController.navigate(Screen.LoanProcessing.createRoute(appId)) },
          onNavigateBack = { navController.popBackStack() }
        )
      }

      // 4. Activity
      composable(Screen.Activity.route) {
        ActivityScreen(viewModel = viewModel)
      }

      // 5. Profile
      composable(Screen.Profile.route) {
        ProfileScreen(
          viewModel = viewModel,
          onNavigateToAdmin = { navController.navigate(Screen.AdminCentralBank.route) },
          onNavigateToAuth = {
            navController.navigate(Screen.AuthSignIn.route) {
              popUpTo(0) { inclusive = true }
            }
          }
        )
      }

      // Transfer Flow
      composable(Screen.TransferInput.route) {
        TransferInputScreen(
          viewModel = viewModel,
          onNavigateBack = { navController.popBackStack() },
          onNavigateToConfirmation = { recipient, amount, note ->
            navController.navigate(Screen.TransferConfirm.createRoute(recipient, amount, note))
          }
        )
      }

      composable(
        route = Screen.TransferConfirm.route,
        arguments = listOf(
          navArgument("recipient") { type = NavType.StringType },
          navArgument("amount") { type = NavType.LongType },
          navArgument("note") { type = NavType.StringType }
        )
      ) { backStackEntry ->
        val recipient = backStackEntry.arguments?.getString("recipient") ?: ""
        val amount = backStackEntry.arguments?.getLong("amount") ?: 0L
        val rawNote = backStackEntry.arguments?.getString("note") ?: ""
        val note = if (rawNote == "-") "" else rawNote

        TransferConfirmationScreen(
          viewModel = viewModel,
          recipientHandle = recipient,
          amount = amount,
          note = note,
          onNavigateBack = { navController.popBackStack() },
          onTransferSuccess = {
            navController.navigate(Screen.TransferReceipt.route) {
              popUpTo(Screen.Home.route)
            }
          }
        )
      }

      composable(Screen.TransferReceipt.route) {
        TransferReceiptScreen(
          viewModel = viewModel,
          onDone = {
            navController.navigate(Screen.Home.route) {
              popUpTo(Screen.Home.route) { inclusive = true }
            }
          }
        )
      }

      // Loan Flow
      composable(Screen.LoanApply.route) {
        LoanApplicationScreen(
          viewModel = viewModel,
          onNavigateBack = { navController.popBackStack() },
          onApplicationSubmitted = { appId ->
            navController.navigate(Screen.LoanProcessing.createRoute(appId)) {
              popUpTo(Screen.Banking.route)
            }
          }
        )
      }

      composable(
        route = Screen.LoanProcessing.route,
        arguments = listOf(navArgument("appId") { type = NavType.StringType })
      ) { backStackEntry ->
        val appId = backStackEntry.arguments?.getString("appId") ?: ""
        LoanFourStageProcessingScreen(
          viewModel = viewModel,
          applicationId = appId,
          onNavigateBack = { navController.navigate(Screen.Banking.route) },
          onOfferAccepted = {
            navController.navigate(Screen.Banking.route) {
              popUpTo(Screen.Banking.route) { inclusive = true }
            }
          }
        )
      }

      // Central Bank Admin
      composable(Screen.AdminCentralBank.route) {
        CentralBankAdminScreen(
          viewModel = viewModel,
          onNavigateBack = { navController.popBackStack() }
        )
      }

      // Notifications
      composable(Screen.Notifications.route) {
        NotificationsScreen(
          viewModel = viewModel,
          onNavigateBack = { navController.popBackStack() }
        )
      }

      // Auth Flow
      composable(Screen.AuthSignIn.route) {
        SignInScreen(
          viewModel = viewModel,
          onNavigateToSignUp = { navController.navigate(Screen.AuthSignUp.route) },
          onNavigateToForgotPassword = { navController.navigate(Screen.AuthForgotPassword.route) },
          onSignInSuccess = {
            navController.navigate(Screen.Home.route) {
              popUpTo(Screen.Home.route) { inclusive = true }
            }
          }
        )
      }

      composable(Screen.AuthSignUp.route) {
        SignUpScreen(
          viewModel = viewModel,
          onNavigateToSignIn = { navController.popBackStack() },
          onSignUpSuccess = {
            navController.navigate(Screen.Home.route) {
              popUpTo(Screen.Home.route) { inclusive = true }
            }
          }
        )
      }

      composable(Screen.AuthForgotPassword.route) {
        ForgotPasswordScreen(
          viewModel = viewModel,
          onNavigateBack = { navController.popBackStack() }
        )
      }
    }
  }
}
