package com.example.safesense.di

import com.example.safesense.domain.usecase.DetectCollisionUseCase
import com.example.safesense.domain.usecase.DetectFallUseCase
import com.example.safesense.sensor.fusion.SensorFusionEngine
import com.example.safesense.sensor.processor.GPSTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideSensorFusionEngine(
        @ApplicationScope scope: CoroutineScope,
        detectFallUseCase: DetectFallUseCase,
        detectCollisionUseCase: DetectCollisionUseCase,
        gpsTracker: GPSTracker
    ): SensorFusionEngine {
        return SensorFusionEngine(
            scope = scope,
            detectFallUseCase = detectFallUseCase,
            detectCollisionUseCase = detectCollisionUseCase,
            gpsTracker = gpsTracker
        )
    }
}
