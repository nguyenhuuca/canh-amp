package com.canhamp.ui.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.canhamp.R
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.theme.Deck
import com.canhamp.util.audioPermission

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Rationale module shown when audio access is missing (B1). The system dialog opens only from GRANT ACCESS;
 * after a "don't ask again" refusal the button opens the app settings instead.
 */
@Composable
fun PermissionPanel(
    permanentlyDenied: Boolean,
    onRequestResult: (granted: Boolean, shouldShowRationale: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permission = audioPermission()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context.findActivity()
        val rationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        onRequestResult(granted, rationale)
    }
    DeckModule(modifier.fillMaxWidth()) {
        Column {
            VfdText(stringResource(R.string.lib_perm_title), style = Deck.type.titleMd)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.lib_perm_body), style = Deck.type.bodyMd, color = Deck.colors.onSurface)
            Spacer(Modifier.height(12.dp))
            if (permanentlyDenied) {
                TactileButton(
                    label = stringResource(R.string.lib_perm_settings),
                    onClick = { context.startActivity(appSettingsIntent(context.packageName)) },
                    active = true,
                )
            } else {
                TactileButton(
                    label = stringResource(R.string.lib_perm_grant),
                    onClick = { launcher.launch(permission) },
                    active = true,
                )
            }
        }
    }
}

private fun appSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
