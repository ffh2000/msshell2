package ru.mstrike.msshell2

/**
 * Класс для получения дополнительной информации об утсройстве
 */
interface DeviceInfo {

    /**
     * Функция должна вернуть информацию об устройстве для которого сделана реализация.
     *
     * Поскольку паарметров у устройства может быть несколько, то формат
     * возвращаемого значения <Ключ, Значение>.
     * @return Информация об утсройстве в виде пары значений: ключ, значение
     */
    fun getInfo(): Map<String, String>

    companion object {
        const val IP_KEY = "IP"
        const val MAC_KEY = "MAC"
        const val PROMPT_KEY = "PROMPT"
        const val CPU_TEMPERATURE_KEY = "cpu_temperature"
    }

}