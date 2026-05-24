package com.vic.recompo

import android.app.Application
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.db.RecompoDatabase

class App : Application() {
    val database by lazy { RecompoDatabase.getInstance(this) }
    val userSettingsStore by lazy { UserSettingsStore(this) }
}
