package com.canhamp.ui.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.canhamp.R
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** The 3 x 2 key block: + FILE, + DIR, - DEL / MISC, LIST >>, SORT (B61, B62, B64, B65). */
@Composable
fun ActionGrid(
    sortMode: SortMode?,
    hasSelection: Boolean,
    canShowDetails: Boolean,
    onAddFile: () -> Unit,
    onAddDir: () -> Unit,
    onDelete: () -> Unit,
    onMisc: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onClear: () -> Unit,
    onSort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val sortLabel = when (sortMode ?: SortMode.TITLE) {
        SortMode.TITLE -> stringResource(R.string.pl_sort_title)
        SortMode.ARTIST -> stringResource(R.string.pl_sort_artist)
        SortMode.DURATION -> stringResource(R.string.pl_sort_time)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            val cell = Modifier.weight(1f)
            TactileButton(
                stringResource(R.string.pl_add_file),
                onAddFile,
                cell,
                sublabel = stringResource(R.string.pl_sub_file),
            )
            TactileButton(
                stringResource(R.string.pl_add_dir),
                onAddDir,
                cell,
                sublabel = stringResource(R.string.pl_sub_dir),
            )
            TactileButton(
                stringResource(R.string.pl_del),
                onDelete,
                cell,
                enabled = hasSelection,
                sublabel = stringResource(R.string.pl_sub_del),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            val cell = Modifier.weight(1f)
            TactileButton(
                stringResource(R.string.pl_misc),
                onMisc,
                cell,
                enabled = canShowDetails,
                sublabel = stringResource(R.string.pl_sub_misc),
            )
            Box(cell) {
                TactileButton(
                    stringResource(R.string.pl_list),
                    { menuOpen = true },
                    Modifier.fillMaxWidth(),
                    active = menuOpen,
                    sublabel = stringResource(R.string.pl_sub_list),
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = Deck.colors.surfaceContainerHigh,
                ) {
                    MenuItem(R.string.pl_save_m3u) { menuOpen = false; onSave() }
                    MenuItem(R.string.pl_load_m3u) { menuOpen = false; onLoad() }
                    MenuItem(R.string.pl_clear) { menuOpen = false; onClear() }
                }
            }
            TactileButton(
                stringResource(R.string.pl_sort),
                onSort,
                cell,
                sublabel = sortLabel,
            )
        }
    }
}

@Composable
private fun MenuItem(label: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label), style = Deck.type.labelLg, color = Deck.colors.onSurface) },
        onClick = onClick,
    )
}
