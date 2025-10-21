const WebSocket = require('ws');

//клиенты
var clients = [];

const wsMSShellServer = new WebSocket.Server({ port: 8080 }, () => {
    console.log('✅ WebSocket сервер запущен на ws://localhost:8080');
});

// Когда подключается новый клиент
wsMSShellServer.on('connection', (ws) => {
    console.log('👤 Новый клиент подключился');

    // Обработка сообщений от клиента
    ws.on('message', (message) => {
        // console.log(`📨 Получено сообщение: ${message}`);
        // console.log(`${message}`);
        process.stdout.write(message);


        // Отправляем обратно эхо-сообщение
        // ws.send(`Эхо: ${message}`);
    });

    // Когда клиент отключается
    ws.on('close', () => {
        console.log('❌ Клиент отключился');
    });

    ws.send('ls -1 -l --color');
    ws.send('\n');
    // ws.send('sh\n');
    // Отправляем приветственное сообщение
    // ws.send('Привет! Вы подключились к WebSocket серверу.');
    ws.send('exit\n');
});