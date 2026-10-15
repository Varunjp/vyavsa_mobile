package com.vyavsa.app.core.di

import com.vyavsa.app.feature.auth.AuthRepository
import com.vyavsa.app.feature.auth.AuthRepositoryImpl
import com.vyavsa.app.feature.cash.CashRepository
import com.vyavsa.app.feature.cash.CashRepositoryImpl
import com.vyavsa.app.feature.dashboard.DashboardRepository
import com.vyavsa.app.feature.dashboard.DashboardRepositoryImpl
import com.vyavsa.app.feature.dues.DuesRepository
import com.vyavsa.app.feature.dues.DuesRepositoryImpl
import com.vyavsa.app.feature.staff.StaffRepository
import com.vyavsa.app.feature.staff.StaffRepositoryImpl
import com.vyavsa.app.feature.transactions.TransactionsRepository
import com.vyavsa.app.feature.transactions.TransactionsRepositoryImpl
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindTransactionsRepository(impl: TransactionsRepositoryImpl): TransactionsRepository

    @Binds
    @Singleton
    abstract fun bindDuesRepository(impl: DuesRepositoryImpl): DuesRepository

    @Binds
    @Singleton
    abstract fun bindStaffRepository(impl: StaffRepositoryImpl): StaffRepository

    @Binds
    @Singleton
    abstract fun bindCashRepository(impl: CashRepositoryImpl): CashRepository

    @Binds
    @Singleton
    abstract fun bindBankRepository(impl: com.vyavsa.app.feature.bank.BankRepositoryImpl): com.vyavsa.app.feature.bank.BankRepository

    @Binds
    @Singleton
    abstract fun bindPlansRepository(impl: com.vyavsa.app.feature.plans.PlansRepositoryImpl): com.vyavsa.app.feature.plans.PlansRepository

    @Binds
    @Singleton
    abstract fun bindTenantUsersRepository(impl: com.vyavsa.app.feature.users.TenantUsersRepositoryImpl): com.vyavsa.app.feature.users.TenantUsersRepository
}
