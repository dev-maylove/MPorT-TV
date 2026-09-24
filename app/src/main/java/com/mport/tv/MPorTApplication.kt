package com.mport.tv

import android.app.Application
import com.mport.tv.data.local.AppDatabase

class MPorTApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
}
