package com.jobalerts.app.core.di

import android.content.Context
import androidx.room.Room
import com.jobalerts.app.data.local.AppDatabase
import com.jobalerts.app.data.remote.NetworkClient
import com.jobalerts.app.data.repository.JobRepositoryImpl
import com.jobalerts.app.domain.repository.JobRepository

/**
 * Manual Dependency Injection container.
 *
 * Owns all app-level singletons (database, network client, repositories) and
 * exposes them as the single source of truth for the rest of the app.
 *
 * Created once per process in [com.jobalerts.app.JobAlertsApp.onCreate].
 */
class AppContainer(context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "jobalerts.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    private val networkClient = NetworkClient()

    val jobRepository: JobRepository by lazy {
        JobRepositoryImpl(
            apiService = networkClient.apiService,
            reminderDao = database.reminderDao()
        )
    }
}
