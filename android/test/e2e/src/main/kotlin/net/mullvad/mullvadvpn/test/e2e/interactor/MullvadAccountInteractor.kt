package net.mullvad.mullvadvpn.test.e2e.interactor

import net.mullvad.mullvadvpn.test.e2e.misc.SimpleMullvadHttpClient

class MullvadAccountInteractor(
    private val httpClient: SimpleMullvadHttpClient,
    private val testAccountNumber: String,
) {
    fun cleanupAccount() {
        httpClient.removeAllDevices(testAccountNumber)
    }
}
