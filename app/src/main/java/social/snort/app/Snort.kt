package social.snort.app

import android.app.Application

class Snort : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: Snort
            private set
    }
}