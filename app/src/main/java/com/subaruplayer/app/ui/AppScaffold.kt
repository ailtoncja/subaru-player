package com.subaruplayer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subaruplayer.app.AppViewModel
import com.subaruplayer.app.R
import com.subaruplayer.app.Tab
import com.subaruplayer.app.ui.components.IconBtn
import com.subaruplayer.app.ui.history.HistoryScreen
import com.subaruplayer.app.ui.library.LibraryScreen
import com.subaruplayer.app.ui.player.PlayerScreen
import com.subaruplayer.app.ui.playlists.PlaylistsScreen
import com.subaruplayer.app.ui.settings.SettingsScreen
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T

private val tabIcons: Map<Tab, ImageVector> = mapOf(
    Tab.LIBRARY to Icons.Outlined.VideoLibrary,
    Tab.PLAYLISTS to Icons.AutoMirrored.Outlined.PlaylistPlay,
    Tab.HISTORY to Icons.Outlined.History,
    Tab.SETTINGS to Icons.Outlined.Settings,
)

@Composable
fun AppRoot(vm: AppViewModel, inPip: Boolean) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.toasts.collect { snackbar.showSnackbar(it) } }

    // "Voltar" desfaz um passo da navegação: busca, pasta, playlist aberta, aba. Na tela inicial, sai do app.
    val canGoBack = vm.queue == null && (vm.tab != Tab.LIBRARY || vm.searchOpen || vm.folderPath.isNotEmpty())
    BackHandler(enabled = canGoBack) {
        when {
            vm.tab == Tab.LIBRARY && vm.searchOpen -> vm.searchOpen = false
            vm.tab == Tab.LIBRARY && vm.folderPath.isNotEmpty() -> vm.upFolder()
            vm.tab == Tab.PLAYLISTS && vm.openPlaylistId != null -> vm.openPlaylistId = null
            vm.tab != Tab.LIBRARY -> vm.tab = Tab.LIBRARY
        }
    }

    Box(Modifier.fillMaxSize().background(C.Surface)) {
        if (vm.queue != null) PlayerScreen(vm, inPip) else MainTabs(vm)
        if (!inPip) {
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = if (vm.queue == null) 84.dp else 16.dp),
            ) { data ->
                Snackbar(data, containerColor = C.CardHighest, contentColor = C.Text, actionColor = C.Gold, shape = RoundedCornerShape(14.dp))
            }
        }
    }
}

@Composable
private fun MainTabs(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Header(vm)
        Box(Modifier.weight(1f)) {
            when (vm.tab) {
                Tab.LIBRARY -> LibraryScreen(vm)
                Tab.PLAYLISTS -> PlaylistsScreen(vm)
                Tab.HISTORY -> HistoryScreen(vm)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
        NavigationBar(containerColor = C.Container, tonalElevation = 0.dp) {
            Tab.entries.forEach { t ->
                NavigationBarItem(
                    selected = t == vm.tab,
                    onClick = { vm.tab = t },
                    icon = { Icon(tabIcons.getValue(t), t.label) },
                    label = { Text(t.label, style = T.labelMd.copy(fontWeight = FontWeight.SemiBold)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = C.Gold, selectedTextColor = C.Gold, indicatorColor = Color(0x33FACC15),
                        unselectedIconColor = C.Muted, unselectedTextColor = C.Muted,
                    ),
                )
            }
        }
    }
}

/** Barra de cima: marca, nome da tela (na biblioteca, a pasta aberta) e, na biblioteca, busca e atualizar. */
@Composable
private fun Header(vm: AppViewModel) {
    val subtitle = when {
        vm.tab == Tab.LIBRARY && vm.searchOpen -> "Buscar"
        vm.tab == Tab.LIBRARY && vm.folderPath.isNotEmpty() -> vm.folderPath.trimEnd('/').substringAfterLast('/')
        else -> vm.tab.label
    }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_logo), null, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Subaru Player", style = T.headlineSm, maxLines = 1)
            Text(subtitle, style = T.labelMd, maxLines = 1)
        }
        if (vm.tab == Tab.LIBRARY && vm.hasPermission) {
            IconBtn(Icons.Outlined.Search, { vm.searchOpen = !vm.searchOpen; if (!vm.searchOpen) vm.searchQuery = "" }, tint = if (vm.searchOpen) C.Gold else C.BluePale, description = "Buscar")
            Spacer(Modifier.width(8.dp))
            IconBtn(Icons.Outlined.Refresh, { vm.refresh() }, tint = C.BluePale, description = "Atualizar")
        }
    }
}
