package com.canhamp.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.canhamp.R
import com.canhamp.ui.components.DeckHeader
import com.canhamp.ui.components.DeckNavBar
import com.canhamp.ui.components.MiniPlayerBar
import com.canhamp.ui.eq.EqScreen
import com.canhamp.ui.library.LibraryScreen
import com.canhamp.ui.main.MainScreen
import com.canhamp.ui.playlist.PlaylistScreen
import com.canhamp.ui.skins.SkinsScreen
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

enum class DeckDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
) {
    MAIN("main", R.string.nav_main, R.string.subtitle_main, Icons.Filled.Radio),
    EQ("eq", R.string.nav_eq, R.string.subtitle_eq, Icons.Filled.Equalizer),
    PLAYLIST("playlist", R.string.nav_playlist, R.string.subtitle_playlist, Icons.Filled.QueueMusic),
    LIBRARY("library", R.string.nav_library, R.string.subtitle_library, Icons.Filled.FolderOpen),
    SKINS("skins", R.string.nav_skins, R.string.subtitle_skins, Icons.Filled.Palette),
    ;

    companion object {
        val start: DeckDestination = MAIN

        fun fromRoute(route: String?): DeckDestination? = entries.firstOrNull { it.route == route }
    }
}

/** Where system back goes from [current]: MAIN for any other tab, null on MAIN (the app exits). */
fun backTarget(current: DeckDestination): DeckDestination? =
    if (current == DeckDestination.start) null else DeckDestination.start

private fun NavController.switchTab(dest: DeckDestination) {
    navigate(dest.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavGraph(shell: ShellViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = DeckDestination.fromRoute(entry?.destination?.route) ?: DeckDestination.start
    val mini by shell.miniPlayer.collectAsState()

    val target = backTarget(current)
    BackHandler(enabled = target != null) { target?.let(nav::switchTab) }

    Box(Modifier.fillMaxSize().background(Deck.colors.surface), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 480.dp)) {
            DeckHeader(subtitle = stringResource(current.subtitleRes), stereo = null)
            NavHost(
                navController = nav,
                startDestination = DeckDestination.start.route,
                modifier = Modifier.weight(1f).padding(horizontal = Dimens.margin),
            ) {
                composable(DeckDestination.MAIN.route) { MainScreen() }
                composable(DeckDestination.EQ.route) { EqScreen() }
                composable(DeckDestination.PLAYLIST.route) { PlaylistScreen() }
                composable(DeckDestination.LIBRARY.route) { LibraryScreen() }
                composable(DeckDestination.SKINS.route) { SkinsScreen() }
            }
            MiniPlayerBar(
                state = mini,
                onOpen = { nav.switchTab(DeckDestination.MAIN) },
                onPrev = {},
                onPlayPause = {},
                onNext = {},
            )
            DeckNavBar(current = current, onSelect = nav::switchTab)
        }
    }
}
