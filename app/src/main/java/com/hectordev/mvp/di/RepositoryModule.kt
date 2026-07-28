package com.hectordev.mvp.di

import com.hectordev.mvp.data.repository.AuthRepositoryDefault
import com.hectordev.mvp.data.repository.EventsRepositoryDefault
import com.hectordev.mvp.data.repository.UserRepositoryDefault
import com.hectordev.mvp.data.util.DiplomaGenerator
import com.hectordev.mvp.domain.repository.AuthRepository
import com.hectordev.mvp.domain.repository.DiplomaRepository
import com.hectordev.mvp.domain.repository.EventsRepository
import com.hectordev.mvp.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryDefault: UserRepositoryDefault
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryDefault: AuthRepositoryDefault
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindEventsRepository(
        eventsRepositoryDefault: EventsRepositoryDefault
    ): EventsRepository

    @Binds
    @Singleton
    abstract fun bindDiplomaRepository(
        diplomaGenerator: DiplomaGenerator
    ): DiplomaRepository
}