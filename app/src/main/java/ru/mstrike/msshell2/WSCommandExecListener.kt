package ru.mstrike.msshell2

class WSCommandExecListener(
    val terminal: WebSocketTerminal
) : CommandExecListener {
    override fun onOutput(message: String) {
        terminal.send(message)
    }

    override fun onErrorOutput(message: String) {
        terminal.send(message)
    }

    override fun onExit(code: Int) {
        if (code != 0) terminal.send("exit code:$code")
    }
}