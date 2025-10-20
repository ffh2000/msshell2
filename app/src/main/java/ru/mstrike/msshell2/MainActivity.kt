package ru.mstrike.msshell2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        GlobalScope.launch(Dispatchers.IO) {
            init()
            TerminalService.start(this@MainActivity)
            finish()
        }
    }

    private fun init() {
        (application as Application).let {
            it.optionsStorage = OptionsStorage(this)
        }
    }
}