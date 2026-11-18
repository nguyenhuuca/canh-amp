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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.canhamp.CanhAmpApp
import com.canhamp.R
import com.canhamp.ui.components.DeckHeader
import com.canhamp.ui.components.DeckNavBar
import com.canhamp.ui.components.MiniPlayerBar
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.eq.EqScreen
import com.canhamp.ui.library.LibraryScreen
import com.canhamp.ui.library.TrackActionSheet
import com.canhamp.ui.library.TrackDetailsSheet
import com.canhamp.ui.library.TrackRow
import com.canhamp.ui.library.playQueueFor
import com.canhamp.ui.library.toQueueEntry
import kotlinx.coroutines.delay
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

private const val TOAST_MS = 2_000L

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
fun AppNavGraph(shell: ShellViewModel = viewModel(factory = ShellViewModel.Factory)) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = DeckDestination.fromRoute(entry?.destination?.route) ?: DeckDestination.start
    val mini by shell.miniPlayer.collectAsState()
    val stereo by shell.stereo.collectAsState()
    val player = (LocalContext.current.applicationContext as CanhAmpApp).container.playerConnection
    var sheetRow by remember { mutableStateOf<TrackRow?>(null) }
    var detailsId by remember { mutableStateOf<Long?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val addedQueue = stringResource(R.string.lib_added_queue)
    val addedNext = stringResource(R.string.lib_added_next)
    val queueFull = stringResource(R.string.queue_full)
    LaunchedEffect(player) {
        player.messages.collect { toast = queueFull }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(TOAST_MS)
            toast = null
        }
    }

    val target = backTarget(current)
    BackHandler(enabled = target != null) { target?.let(nav::switchTab) }

    Box(Modifier.fillMaxSize().background(Deck.colors.surface), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 480.dp)) {
            DeckHeader(subtitle = stringResource(current.subtitleRes), stereo = stereo)
            NavHost(
                navController = nav,
                startDestination = DeckDestination.start.route,
                modifier = Modifier.weight(1f).padding(horizontal = Dimens.margin),
            ) {
                composable(DeckDestination.MAIN.route) {
                    MainScreen(
                        onOpen = { nav.switchTab(DeckDestination.LIBRARY) },
                        onOpenEq = { nav.switchTab(DeckDestination.EQ) },
                    )
                }
                composable(DeckDestination.EQ.route) { EqScreen() }
                composable(DeckDestination.PLAYLIST.route) { PlaylistScreen() }
                composable(DeckDestination.LIBRARY.route) {
                    LibraryScreen(
                        onPlay = { row, snapshot ->
                            val (entries, start) = playQueueFor(row, snapshot)
                            player.setQueue(entries, start)
                        },
                        onMore = { sheetRow = it },
                    )
                }
                composable(DeckDestination.SKINS.route) { SkinsScreen() }
            }
            MiniPlayerBar(
                state = mini,
                onOpen = { nav.switchTab(DeckDestination.MAIN) },
                onPrev = shell::previous,
                onPlayPause = shell::togglePlayPause,
                onNext = shell::next,
            )
            DeckNavBar(current = current, onSelect = nav::switchTab)
        }
        toast?.let { text ->
            RecessedWell(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimens.navHeight + Dimens.miniPlayerHeight + Dimens.spaceXl)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                vfd = true,
            ) {
                VfdText(text, style = Deck.type.labelMd)
            }
        }
    }

    sheetRow?.let { row ->
        TrackActionSheet(
            row = row,
            onAddToQueue = {
                player.addToQueue(listOf(row.toQueueEntry()))
                toast = addedQueue
                sheetRow = null
            },
            onPlayNext = {
                player.playNext(listOf(row.toQueueEntry()))
                toast = addedNext
                sheetRow = null
            },
            onDetails = {
                detailsId = row.id
                sheetRow = null
            },
            onDismiss = { sheetRow = null },
        )
    }
    detailsId?.let { id -> TrackDetailsSheet(trackId = id, onDismiss = { detailsId = null }) }
}
