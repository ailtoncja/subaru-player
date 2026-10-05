package com.subaruplayer.app

import android.app.Application
import com.subaruplayer.app.data.SettingsStore
import com.subaruplayer.app.data.UserStore
import com.subaruplayer.app.player.PlayerEngine

class SubaruPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        UserStore.init(this)
        PlayerEngine.warmUp(this)
    }
}
