package net.mullvad.mullvadvpn.compose.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import io.mockk.coVerify
import io.mockk.mockk
import net.mullvad.mullvadvpn.compose.createEdgeToEdgeComposeExtension
import net.mullvad.mullvadvpn.compose.setContentWithTheme
import net.mullvad.mullvadvpn.compose.state.Udp2TcpSettingsState
import net.mullvad.mullvadvpn.compose.test.UDP_OVER_TCP_PORT_ITEM_X_TEST_TAG
import net.mullvad.mullvadvpn.lib.model.Constraint
import net.mullvad.mullvadvpn.lib.model.Port
import net.mullvad.mullvadvpn.onNodeWithTagAndText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
class Udp2TcpSettingsScreenTest {
    @JvmField @RegisterExtension val composeExtension = createEdgeToEdgeComposeExtension()

    @Test
    fun testSelectTcpOverUdpPortOption() =
        composeExtension.use {
            // Arrange
            val onObfuscationPortSelected: (Constraint<Port>) -> Unit = mockk(relaxed = true)
            setContentWithTheme {
                Udp2TcpSettingsScreen(
                    state = Udp2TcpSettingsState(port = Constraint.Any),
                    onObfuscationPortSelected = onObfuscationPortSelected,
                )
            }

            // Act
            onNodeWithTagAndText(
                    testTag = String.format(UDP_OVER_TCP_PORT_ITEM_X_TEST_TAG, 5001),
                    text = "5001",
                )
                .assertExists()
                .performClick()

            // Assert
            coVerify(exactly = 1) { onObfuscationPortSelected.invoke(Constraint.Only(Port(5001))) }
        }
}