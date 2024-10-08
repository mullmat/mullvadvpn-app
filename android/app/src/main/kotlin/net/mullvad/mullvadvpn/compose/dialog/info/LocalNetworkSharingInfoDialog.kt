package net.mullvad.mullvadvpn.compose.dialog.info

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import net.mullvad.mullvadvpn.R
import net.mullvad.mullvadvpn.compose.component.textResource
import net.mullvad.mullvadvpn.lib.theme.AppTheme

@Preview
@Composable
private fun PreviewLocalNetworkSharingInfoDialog() {
    AppTheme { LocalNetworkSharingInfo(EmptyDestinationsNavigator) }
}

@Destination<RootGraph>(style = DestinationStyle.Dialog::class)
@Composable
fun LocalNetworkSharingInfo(navigator: DestinationsNavigator) {
    InfoDialog(
        message = stringResource(id = R.string.local_network_sharing_info),
        additionalInfo =
            buildString {
                appendLine(stringResource(id = R.string.local_network_sharing_additional_info))
                appendLine(textResource(id = R.string.local_network_sharing_ip_ranges))
            },
        onDismiss = dropUnlessResumed { navigator.navigateUp() },
    )
}
