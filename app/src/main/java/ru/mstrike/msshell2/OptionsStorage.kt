package ru.mstrike.msshell2

import android.content.Context
import android.content.SharedPreferences

class OptionsStorage(
    val context: Context
) {

    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("msshell2", Context.MODE_PRIVATE)

    var uuid: String
        get() {
            return sharedPreferences.getString(UUID_KEY, "") ?: ""
        }
        set(value) {
            sharedPreferences.edit().apply {
                putString(UUID_KEY, value)
                commit()
            }
        }

    companion object {
        const val UUID_KEY = "UUID"
    }

}