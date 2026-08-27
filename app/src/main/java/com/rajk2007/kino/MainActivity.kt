package com.rajk2007.kino

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rajk2007.kino.data.DetailsViewModel
import com.rajk2007.kino.data.HomeViewModel
import com.rajk2007.kino.downloads.AppContextHolder
import com.rajk2007.kino.ui.DetailsScreen
import com.rajk2007.kino.ui.KinoTheme
import com.rajk2007.kino.ui.LibraryScreen
import com.rajk2007.kino.ui.SearchScreen
import com.rajk2007.kino.ui.home.KinoHomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextHolder.context = applicationContext
        setContent { KinoTheme { KinoApp() } }
    }
}

@Composable
private fun KinoApp() {
    val nav = rememberNavController()
    fun openDetails(url: String, apiName: String) {
        nav.navigate("details?url=${Uri.encode(url)}&apiName=${Uri.encode(apiName)}")
    }
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            KinoHomeScreen(
                sections = state.sections,
                isLoading = state.loading,
                error = state.error,
                onMovieClick = { item -> openDetails(item.url, item.apiName) },
                onSearchClick = { nav.navigate("search") },
                onLibraryClick = { nav.navigate("library") },
                onRefresh = vm::refresh
            )
        }
        composable("search") {
            SearchScreen(onBack = { nav.popBackStack() }, onOpen = { item -> openDetails(item.url, item.apiName) })
        }
        composable("library") {
            LibraryScreen(onBack = { nav.popBackStack() }, onOpen = { item -> openDetails(item.url, item.apiName) })
        }
        composable(
            "details?url={url}&apiName={apiName}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("apiName") { type = NavType.StringType }
            )
        ) { entry ->
            DetailsScreen(
                url = entry.arguments?.getString("url").orEmpty(),
                apiName = entry.arguments?.getString("apiName").orEmpty(),
                onBack = { nav.popBackStack() },
                vm = viewModel<DetailsViewModel>()
            )
        }
    }
}
