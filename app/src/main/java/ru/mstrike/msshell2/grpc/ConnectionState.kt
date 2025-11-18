package ru.mstrike.msshell2.grpc

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val connectedAt: Long = System.currentTimeMillis()) : ConnectionState()
    data class Error(val message: String, val attempt: Int) : ConnectionState()
}