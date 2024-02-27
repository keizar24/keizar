package org.keizar.android.ui.profile

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.mapLatest
import org.keizar.android.client.SessionManager
import org.keizar.android.client.UserService
import org.keizar.android.ui.foundation.AbstractViewModel
import org.keizar.utils.communication.account.User
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProfileViewModel : KoinComponent, AbstractViewModel() {
    private val sessionManager: SessionManager by inject()
    private val userService: UserService by inject()

    /**
     * Current user's information.
     */
    val self: SharedFlow<User> = sessionManager.token.mapLatest {
        userService.self()
    }.shareInBackground()
}