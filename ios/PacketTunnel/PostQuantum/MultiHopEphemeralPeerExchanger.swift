//
//  MultiHopPostQuantumKeyExchanging.swift
//  PacketTunnel
//
//  Created by Mojgan on 2024-07-15.
//  Copyright © 2024 Mullvad VPN AB. All rights reserved.
//

import MullvadREST
import MullvadRustRuntime
import MullvadSettings
import MullvadTypes
import PacketTunnelCore
import WireGuardKitTypes

final class MultiHopEphemeralPeerExchanger: EphemeralPeerExchangingProtocol {
    let entry: SelectedRelay
    let exit: SelectedRelay
    let keyExchanger: EphemeralPeerExchangeActorProtocol
    let devicePrivateKey: PrivateKey
    let onFinish: () -> Void
    let onUpdateConfiguration: (EphemeralPeerNegotiationState) -> Void
    let enablePostQuantum: Bool
    let enableDaita: Bool

    private var entryPeerKey: EphemeralPeerKey!
    private var exitPeerKey: EphemeralPeerKey!

    private let defaultGatewayAddressRange = [IPAddressRange(from: "\(LocalNetworkIPs.gatewayAddress.rawValue)/32")!]
    private let allTrafficRange = [
        IPAddressRange(from: "\(LocalNetworkIPs.defaultRouteIpV4.rawValue)/0")!,
        IPAddressRange(from: "\(LocalNetworkIPs.defaultRouteIpV6.rawValue)/0")!,
    ]

    private var state: StateMachine = .initial

    enum StateMachine {
        case initial
        case negotiatingWithEntry
        case negotiatingBetweenEntryAndExit
        case makeConnection
    }

    init(
        entry: SelectedRelay,
        exit: SelectedRelay,
        devicePrivateKey: PrivateKey,
        keyExchanger: EphemeralPeerExchangeActorProtocol,
        enablePostQuantum: Bool,
        enableDaita: Bool,
        onUpdateConfiguration: @escaping (EphemeralPeerNegotiationState) -> Void,
        onFinish: @escaping () -> Void
    ) {
        self.entry = entry
        self.exit = exit
        self.devicePrivateKey = devicePrivateKey
        self.keyExchanger = keyExchanger
        self.enablePostQuantum = enablePostQuantum
        self.enableDaita = enableDaita
        self.onUpdateConfiguration = onUpdateConfiguration
        self.onFinish = onFinish
    }

    func start() {
        guard state == .initial else { return }
        negotiateWithEntry()
    }

    public func receiveEphemeralPeerPrivateKey(_ ephemeralPeerPrivateKey: PrivateKey) {
        if state == .negotiatingWithEntry {
            entryPeerKey = EphemeralPeerKey(ephemeralKey: ephemeralPeerPrivateKey)
            negotiateBetweenEntryAndExit()
        } else if state == .negotiatingBetweenEntryAndExit {
            exitPeerKey = EphemeralPeerKey(ephemeralKey: ephemeralPeerPrivateKey)
            makeConnection()
        }
    }

    func receivePostQuantumKey(
        _ preSharedKey: PreSharedKey,
        ephemeralKey: PrivateKey
    ) {
        if state == .negotiatingWithEntry {
            entryPeerKey = EphemeralPeerKey(preSharedKey: preSharedKey, ephemeralKey: ephemeralKey)
            negotiateBetweenEntryAndExit()
        } else if state == .negotiatingBetweenEntryAndExit {
            exitPeerKey = EphemeralPeerKey(preSharedKey: preSharedKey, ephemeralKey: ephemeralKey)
            makeConnection()
        }
    }

    private func negotiateWithEntry() {
        state = .negotiatingWithEntry
        onUpdateConfiguration(.single(EphemeralPeerRelayConfiguration(
            relay: entry,
            configuration: EphemeralPeerConfiguration(
                privateKey: devicePrivateKey,
                allowedIPs: defaultGatewayAddressRange
            )
        )))
        keyExchanger.startNegotiation(
            with: devicePrivateKey,
            enablePostQuantum: enablePostQuantum,
            enableDaita: enableDaita
        )
    }

    private func negotiateBetweenEntryAndExit() {
        state = .negotiatingBetweenEntryAndExit
        onUpdateConfiguration(.multi(
            entry: EphemeralPeerRelayConfiguration(
                relay: entry,
                configuration: EphemeralPeerConfiguration(
                    privateKey: entryPeerKey.ephemeralKey,
                    preSharedKey: entryPeerKey.preSharedKey,
                    allowedIPs: [IPAddressRange(from: "\(exit.endpoint.ipv4Relay.ip)/32")!]
                )
            ),
            exit: EphemeralPeerRelayConfiguration(
                relay: exit,
                configuration: EphemeralPeerConfiguration(
                    privateKey: devicePrivateKey,
                    allowedIPs: defaultGatewayAddressRange
                )
            )
        ))
        // Daita is always disabled when negotiating with the exit peer in the multihop scenarios
        keyExchanger.startNegotiation(
            with: devicePrivateKey,
            enablePostQuantum: enablePostQuantum,
            enableDaita: false
        )
    }

    private func makeConnection() {
        state = .makeConnection
        onUpdateConfiguration(.multi(
            entry: EphemeralPeerRelayConfiguration(
                relay: entry,
                configuration: EphemeralPeerConfiguration(
                    privateKey: entryPeerKey.ephemeralKey,
                    preSharedKey: entryPeerKey.preSharedKey,
                    allowedIPs: [IPAddressRange(from: "\(exit.endpoint.ipv4Relay.ip)/32")!]
                )
            ),
            exit: EphemeralPeerRelayConfiguration(
                relay: exit,
                configuration: EphemeralPeerConfiguration(
                    privateKey: exitPeerKey.ephemeralKey,
                    preSharedKey: exitPeerKey.preSharedKey,
                    allowedIPs: allTrafficRange
                )
            )
        ))
        self.onFinish()
    }
}
