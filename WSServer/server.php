<?php
$address = '0.0.0.0';
$port = 8080;
$server = stream_socket_server("tcp://$address:$port", $errno, $errstr);

if (!$server) {
    die("Ошибка: $errstr ($errno)\n");
}

echo "✅ Сервер слушает $address:$port...\n";

while ($conn = stream_socket_accept($server)) {
    echo "Кто-то подключается к серверу\n";
    $headers = fread($conn, 1024);
    // Выполняем WebSocket handshake
    if (preg_match("/Sec-WebSocket-Key: (.*)\r\n/", $headers, $match)) {
        $key = trim($match[1]);
        $accept = base64_encode(pack(
            'H*',
            sha1($key . '258EAFA5-E914-47DA-95CA-C5AB0DC85B11')
        ));

        $upgrade = "HTTP/1.1 101 Switching Protocols\r\n" .
                   "Upgrade: websocket\r\n" .
                   "Connection: Upgrade\r\n" .
                   "Sec-WebSocket-Accept: $accept\r\n\r\n";

        fwrite($conn, $upgrade);
        echo "🔗 Клиент подключен\n\n";
    }

    fclose($conn);
}
