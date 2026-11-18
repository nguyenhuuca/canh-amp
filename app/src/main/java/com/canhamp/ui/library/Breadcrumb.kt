package com.canhamp.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** Drill-down path and file count in a recessed well; tapping it goes up one level. */
@Composable
fun Breadcrumb(path: String, fileCount: Int, onUp: () -> Unit, modifier: Modifier = Modifier) {
    val c = Deck.colors
    val up = stringResource(R.string.lib_cd_up)
    RecessedWell(modifier.fillMaxWidth(), vfd = true, padding = PaddingValues(0.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.minTouch)
                .clickable(onClickLabel = up, onClick = onUp)
                .padding(horizontal = Dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = up, tint = c.primaryContainer)
            Text(
                path,
                style = Deck.type.labelLg,
                color = c.primaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = Dimens.spaceMd),
            )
            Text(stringResource(R.string.lib_files, fileCount), style = Deck.type.labelMd, color = c.onSurfaceVariant, maxLines = 1)
        }
    }
}
