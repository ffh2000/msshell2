const WebSocket = require('ws');

//клиенты
var clients = [];

const wsMSShellServer = new WebSocket.Server({ port: 8080 }, () => {
    console.log('✅ WebSocket сервер для MS Shell 2 запущен на ws://localhost:8080');
});
var wsMSShellSocket;
var wsWebSocket;

const webTerminalServer = new WebSocket.Server({ port: 8081 }, () => {
    console.log('✅ WebSocket сервер для web на ws://localhost:8081');
});

// Когда подключается новый клиент
wsMSShellServer.on('connection', (ws) => {
    console.log('👤 Новый клиент MS Shell 2 подключился');
    wsMSShellSocket = ws;

    // Обработка сообщений от клиента
    ws.on('message', (message) => {
        // console.log(`📨 Получено сообщение: ${message}`);
        // console.log(`${message}`);
        process.stdout.write(message);
        if (wsWebSocket)
            wsWebSocket.send(message.toString());

        // Отправляем обратно эхо-сообщение
        // ws.send(`Эхо: ${message}`);
    });

    // Когда клиент отключается
    ws.on('close', () => {
        console.log('❌ MS Shell 2 клиент отключился');
    });

    ws.send('ls -1 -l --color\n');
});

webTerminalServer.on('connection', (ws) => {
    console.log('👤 Новый web-клиент подключился');
    wsWebSocket = ws

    // Обработка сообщений от клиента
    ws.on('message', (message) => {
        // console.log(`📨 Получено сообщение: ${message}`);
        // console.log(`${message}`);
        process.stdout.write("WEB: " + message);
        if (wsMSShellSocket)
            wsMSShellSocket.send(message.toString());

        // Отправляем обратно эхо-сообщение
        // ws.send(`Эхо: ${message}`);
    });

    // Когда клиент отключается
    ws.on('close', () => {
        console.log('❌  Web-клиент отключился');
    });

    // ws.send('ls -1 -l --color\n');
});