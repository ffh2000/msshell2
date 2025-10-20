package ru.mstrike.msshell2

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class OptionsStorage(
    val context: Context
) {

    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("msshell2", Context.MODE_PRIVATE)

    val uuid: String
        get() {
            var uuid = sharedPreferences.getString(UUID_KEY, null)
            if (uuid == null) {
                uuid = UUID.randomUUID().toString()
                sharedPreferences.edit().let {
                    it.putString(UUID_KEY, uuid)
                    it.commit()
                }
            }
            return uuid
        }

    companion object {
        const val UUID_KEY = "UUID"
    }

}