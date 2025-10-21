package ru.mstrike.msshell2

/**
 * Класс с настройками, которые можно запрашивать через
 * content provider у MS Vision.
 * Сюда попадают только параметры, которые хранятся в
 * OptionsStorage на строне MS Vision.
 */
enum class MSVisionOption(
    val optionName: String
) {
    /**
     * Для запроса panel_uuid
     */
    PANEL_UUID("panel_uuid"),

    /**
     * Для запроса кода панели
     */
    CODE("code"),

    /**
     * Для запроса адреса сервера с которым работать
     */
    SERVER_ADDRESS("server_address"),

    /**
     * Для запроса refresh token сетевых запросов.
     */
    REFRESH_TOKEN("refresh_token"),

    /**
     * Секретный код по которому MS Vision получает
     * refresh token, access token
     */
    CLIENT_SECRET("client_secret"),


    CLIENT_ID("client_id"),

    PASSWORD("password")
}