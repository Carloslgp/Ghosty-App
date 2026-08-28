package com.utils.calc.core.di

import com.utils.calc.core.data.fake.FakeAlertDispatcher
import com.utils.calc.core.data.fake.FakeEvidenceRepository
import com.utils.calc.core.data.fake.FakeRecordingService
import com.utils.calc.core.data.fake.SimulatedLocationProvider
import com.utils.calc.core.data.fake.SystemTimeSource
import com.utils.calc.core.data.repository.ContactsRepositoryImpl
import com.utils.calc.core.data.repository.SecurityRepositoryImpl
import com.utils.calc.core.data.repository.SessionAwareContactsRepository
import com.utils.calc.core.data.repository.SessionAwareEvidenceRepository
import com.utils.calc.core.data.repository.SessionAwareTriggerRepository
import com.utils.calc.core.data.repository.TriggerRepositoryImpl
import com.utils.calc.core.data.session.VaultSessionImpl
import com.utils.calc.core.domain.repository.ContactsRepository
import com.utils.calc.core.domain.repository.EvidenceRepository
import com.utils.calc.core.domain.repository.SecurityRepository
import com.utils.calc.core.domain.repository.TriggerRepository
import com.utils.calc.core.domain.service.AlertDispatcher
import com.utils.calc.core.domain.service.LocationProvider
import com.utils.calc.core.domain.service.RecordingService
import com.utils.calc.core.domain.service.TimeSource
import com.utils.calc.core.domain.session.VaultSession
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Todo o app conversa com interfaces de core:domain. Trocar o fake por uma
 * implementacao real e' mexer neste arquivo e em mais nada.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindVaultSession(impl: VaultSessionImpl): VaultSession

    @Binds
    @Singleton
    abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository

    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository

    @Binds
    @Singleton
    @SessionAware
    abstract fun bindSessionAwareContacts(impl: SessionAwareContactsRepository): ContactsRepository

    @Binds
    @Singleton
    abstract fun bindTriggerRepository(impl: TriggerRepositoryImpl): TriggerRepository

    @Binds
    @Singleton
    @SessionAware
    abstract fun bindSessionAwareTriggers(impl: SessionAwareTriggerRepository): TriggerRepository

    @Binds
    @Singleton
    abstract fun bindEvidenceRepository(impl: FakeEvidenceRepository): EvidenceRepository

    @Binds
    @Singleton
    @SessionAware
    abstract fun bindSessionAwareEvidence(impl: SessionAwareEvidenceRepository): EvidenceRepository

    @Binds
    @Singleton
    abstract fun bindAlertDispatcher(impl: FakeAlertDispatcher): AlertDispatcher

    @Binds
    @Singleton
    abstract fun bindRecordingService(impl: FakeRecordingService): RecordingService

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: SimulatedLocationProvider): LocationProvider

    @Binds
    @Singleton
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource
}
