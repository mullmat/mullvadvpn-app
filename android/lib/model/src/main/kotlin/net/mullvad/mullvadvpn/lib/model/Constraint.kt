package net.mullvad.mullvadvpn.lib.model

import arrow.optics.optics

@optics
sealed interface Constraint<out T> {
    data object Any : Constraint<Nothing>

    @optics
    data class Only<T>(val value: T) : Constraint<T> {
        companion object
    }

    fun getOrNull(): T? =
        when (this) {
            Any -> null
            is Only -> value
        }

    companion object
}
