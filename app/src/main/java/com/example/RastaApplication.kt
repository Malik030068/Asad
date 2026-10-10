package com.example

import android.app.Application
import com.example.automation.DeviceActionManager
import com.example.data.repository.AiService
import com.example.data.repository.RastaRepository

class RastaApplication : Application() {

    lateinit var repository: RastaRepository
        private set

    lateinit var actionManager: DeviceActionManager
        private set

    lateinit var aiService: AiService
        private set

    override fun onCreate() {
        super.onCreate()
        repository = RastaRepository(this)
        actionManager = DeviceActionManager(this)
        aiService = AiService()
    }
}
