package ru.mstrike.msshell2.grpc

import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

class GrpcChannelManager(
    private val serverAddress: String,
    private val useTls: Boolean = false
) {
    private var channel: ManagedChannel? = null

    fun createChannel(): ManagedChannel {
        // Закрыть старый канал если существует
        channel?.shutdownNow()

        Log.d("GrpcChannelManager", "Creating gRPC channel to $serverAddress (TLS: $useTls)")

        val builder = ManagedChannelBuilder.forTarget(serverAddress)
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .idleTimeout(Long.MAX_VALUE, TimeUnit.SECONDS)

        // В production используйте TLS
        if (useTls) {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }

        return builder.build().also {
            channel = it
            Log.d("GrpcChannelManager", "gRPC channel created successfully")
        }
    }

    fun shutdown() {
        Log.d("GrpcChannelManager", "Shutting down gRPC channel")
        channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
        channel = null
    }

    fun isShutdown(): Boolean = channel?.isShutdown ?: true
    fun isTerminated(): Boolean = channel?.isTerminated ?: true
}