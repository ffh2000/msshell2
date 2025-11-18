package ru.mstrike.msshell2.shell

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Менеджер управления сессиями с консольными командами
 */
class ShellSessionManager {
    private val sessions = ConcurrentHashMap<String, AndroidShellSession>()

    fun addSession(session: AndroidShellSession) {
        sessions[session.sessionId] = session
        Log.d(
            this.javaClass.name,
            "Added shell session: ${session.sessionId}. Total sessions: ${sessions.size}"
        )
    }

    fun getSession(sessionId: String): AndroidShellSession? {
        return sessions[sessionId]
    }

    fun removeSession(sessionId: String): AndroidShellSession? {
        val session = sessions.remove(sessionId)
        if (session != null) {
            Log.d(
                this.javaClass.name,
                "Removed shell session: $sessionId. Remaining sessions: ${sessions.size}"
            )
        }
        return session
    }

    fun getAllSessions(): List<AndroidShellSession> {
        return sessions.values.toList()
    }

    fun getSessionCount(): Int = sessions.size

    fun closeAll() {
        Log.i(this.javaClass.name, "Closing all shell sessions (${sessions.size} active)")
        sessions.values.forEach { session ->
            try {
                session.close()
            } catch (e: Exception) {
                Log.e(this.javaClass.name, "Error closing session: ${session.sessionId}\n$e")
            }
        }
        sessions.clear()
    }

    fun cleanupDeadSessions() {
        val deadSessions = sessions.filter { !it.value.isAlive() }
        deadSessions.forEach { (sessionId, session) ->
            Log.i(this.javaClass.name, "Cleaning up dead session: $sessionId")
            session.close()
            sessions.remove(sessionId)
        }
    }
}