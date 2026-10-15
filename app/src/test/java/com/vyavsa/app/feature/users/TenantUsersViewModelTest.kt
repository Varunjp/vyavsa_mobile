package com.vyavsa.app.feature.users

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CreateTenantUserRequest
import com.vyavsa.app.core.model.TenantUser
import com.vyavsa.app.core.model.UpdateTenantUserRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TenantUsersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTenantUsersRepository
    private lateinit var viewModel: TenantUsersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTenantUsersRepository()
        viewModel = TenantUsersViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testNonAdminAccessDeniedOnInitialLoad() = runTest {
        viewModel.adminOverrideForTesting = false
        viewModel.loadUsers()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.usersState.value
        assertTrue("Expected Error state for non-admin, got $state", state is UiState.Error)
        assertEquals(
            "Access Denied: Only administrators can view tenant user accounts",
            (state as UiState.Error).message
        )
    }

    @Test
    fun testAdminLoadUsersSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        viewModel.loadUsers()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.usersState.value
        assertTrue("Expected Success state for admin, got $state", state is UiState.Success)
        val users = (state as UiState.Success).data
        assertEquals(2, users.size)
        assertEquals("admin@example.com", users[0].email)
        assertEquals("staff@example.com", users[1].email)
    }

    @Test
    fun testAdminCreateUserSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createUser(
            name = "Manager One",
            email = "manager1@example.com",
            password = "secretpassword",
            role = "admin"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on user create, got $actionState", actionState is UiState.Success)
        assertEquals(3, fakeRepository.usersList.size)
    }

    @Test
    fun testNonAdminCreateUserBlocked() = runTest {
        viewModel.adminOverrideForTesting = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createUser(
            name = "Manager One",
            email = "manager1@example.com",
            password = "secretpassword",
            role = "admin"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error on non-admin create user, got $actionState", actionState is UiState.Error)
        assertEquals(
            "Access Denied: Administrator role required",
            (actionState as UiState.Error).message
        )
    }

    @Test
    fun testCreateUserShortPasswordValidation() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createUser(
            name = "Manager One",
            email = "manager1@example.com",
            password = "123",
            role = "user"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error on short password, got $actionState", actionState is UiState.Error)
        assertEquals(
            "Password must be at least 6 characters",
            (actionState as UiState.Error).message
        )
    }

    @Test
    fun testAdminUpdateUserSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateUser(
            id = "user-2",
            name = "Updated Staff",
            role = "admin",
            status = "inactive",
            password = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on update, got $actionState", actionState is UiState.Success)
        val updated = fakeRepository.usersList.find { it.id == "user-2" }
        assertEquals("Updated Staff", updated?.name)
        assertEquals("inactive", updated?.status)
        assertEquals("admin", updated?.role)
    }

    @Test
    fun testAdminDeleteUserSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteUser("user-2")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on delete, got $actionState", actionState is UiState.Success)
        assertEquals(1, fakeRepository.usersList.size)
    }
}

class FakeTenantUsersRepository : TenantUsersRepository {

    val usersList = mutableListOf(
        TenantUser(
            id = "user-1",
            tenantId = "tenant-1",
            name = "Main Admin",
            role = "admin",
            email = "admin@example.com",
            status = "active",
            createdAt = "2026-01-01T00:00:00Z"
        ),
        TenantUser(
            id = "user-2",
            tenantId = "tenant-1",
            name = "Sales Staff",
            role = "user",
            email = "staff@example.com",
            status = "active",
            createdAt = "2026-01-02T00:00:00Z"
        )
    )

    override fun listUsers(search: String?, role: String?, status: String?): Flow<Resource<List<TenantUser>>> = flow {
        emit(Resource.Loading)
        val filtered = usersList.filter {
            (search == null || it.name.contains(search, ignoreCase = true) || it.email.contains(search, ignoreCase = true)) &&
            (role == null || it.role.equals(role, ignoreCase = true)) &&
            (status == null || it.status.equals(status, ignoreCase = true))
        }
        emit(Resource.Success(filtered))
    }

    override fun createUser(req: CreateTenantUserRequest): Flow<Resource<TenantUser>> = flow {
        emit(Resource.Loading)
        val newUser = TenantUser(
            id = "user-${usersList.size + 1}",
            tenantId = "tenant-1",
            name = req.name,
            email = req.email,
            role = req.role,
            status = req.status,
            createdAt = "2026-01-03T00:00:00Z"
        )
        usersList.add(newUser)
        emit(Resource.Success(newUser))
    }

    override fun getUserById(id: String): Flow<Resource<TenantUser>> = flow {
        emit(Resource.Loading)
        val user = usersList.find { it.id == id }
        if (user != null) {
            emit(Resource.Success(user))
        } else {
            emit(Resource.Error("User not found"))
        }
    }

    override fun updateUser(id: String, req: UpdateTenantUserRequest): Flow<Resource<TenantUser>> = flow {
        emit(Resource.Loading)
        val index = usersList.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = usersList[index]
            val updated = old.copy(
                name = req.name ?: old.name,
                role = req.role ?: old.role,
                status = req.status ?: old.status
            )
            usersList[index] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("User not found"))
        }
    }

    override fun deleteUser(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        usersList.removeAll { it.id == id }
        emit(Resource.Success(Unit))
    }
}
