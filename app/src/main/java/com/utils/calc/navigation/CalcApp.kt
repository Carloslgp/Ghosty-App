package com.utils.calc.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.utils.calc.core.designsystem.theme.AppSkin
import com.utils.calc.core.designsystem.theme.CalcTheme
import com.utils.calc.feature.calculator.CalculatorRoute
import com.utils.calc.feature.onboarding.OnboardingRoute
import com.utils.calc.feature.panic.PanicOverlay
import com.utils.calc.feature.vault.contacts.ContactsRoute
import com.utils.calc.feature.vault.evidence.EvidenceRoute
import com.utils.calc.feature.vault.home.VaultHomeRoute
import com.utils.calc.feature.vault.safetest.SafeTestRoute
import com.utils.calc.feature.vault.triggers.TriggersRoute

@Composable
fun CalcApp(panicViewModel: AppPanicViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val panicState by panicViewModel.state.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val skin = if (route != null && route != Routes.CALCULATOR) AppSkin.Vault else AppSkin.Facade

    /**
     * Quando o disfarce entra, a pilha volta para a calculadora por baixo dele.
     * Sem isso, cancelar a emergência com o aparelho na mão de outra pessoa
     * devolveria o cofre aberto na tela.
     */
    LaunchedEffect(panicState.showsOverlay) {
        if (panicState.showsOverlay) {
            panicViewModel.onCloseVault()
            navController.quickExit()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CalcTheme(skin = skin) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AppNavHost(navController = navController)
            }
        }

        if (panicState.showsOverlay) {
            CalcTheme(skin = AppSkin.Facade) {
                PanicOverlay(
                    disguise = panicState.disguise,
                    onCancel = panicViewModel::onCancel,
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.CALCULATOR) {
        composable(Routes.CALCULATOR) {
            CalculatorRoute(
                onOpenOnboarding = { navController.navigate(Routes.ONBOARDING) },
                onOpenVault = { navController.navigate(Routes.VAULT_HOME) },
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingRoute(
                onCompleted = {
                    navController.navigate(Routes.VAULT_HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onAbandoned = { navController.quickExit() },
            )
        }

        composable(Routes.VAULT_HOME) {
            VaultHomeRoute(
                onQuickExit = { navController.quickExit() },
                onOpenContacts = { navController.navigate(Routes.VAULT_CONTACTS) },
                onOpenTriggers = { navController.navigate(Routes.VAULT_TRIGGERS) },
                onOpenEvidence = { navController.navigate(Routes.VAULT_EVIDENCE) },
                onOpenSafeTest = { navController.navigate(Routes.VAULT_SAFE_TEST) },
                onResetCompleted = { navController.quickExit() },
            )
        }

        composable(Routes.VAULT_CONTACTS) {
            ContactsRoute(
                onBack = { navController.popBackStack() },
                onQuickExit = { navController.quickExit() },
            )
        }

        composable(Routes.VAULT_TRIGGERS) {
            TriggersRoute(
                onBack = { navController.popBackStack() },
                onQuickExit = { navController.quickExit() },
            )
        }

        composable(Routes.VAULT_EVIDENCE) {
            EvidenceRoute(
                onBack = { navController.popBackStack() },
                onQuickExit = { navController.quickExit() },
            )
        }

        composable(Routes.VAULT_SAFE_TEST) {
            SafeTestRoute(
                onBack = { navController.popBackStack() },
                onQuickExit = { navController.quickExit() },
            )
        }
    }
}

/** Saída rápida: volta para a fachada e apaga a pilha inteira. */
fun NavHostController.quickExit() {
    navigate(Routes.CALCULATOR) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
