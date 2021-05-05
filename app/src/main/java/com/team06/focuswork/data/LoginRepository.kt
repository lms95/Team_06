package com.team06.focuswork.data

import com.team06.focuswork.model.LoggedInUser

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

object LoginRepository {
    private val dataSource: LoginDataSource = LoginDataSource()

    // in-memory cache of the loggedInUser object
    private var user: LoggedInUser? = null
    fun getUser(): LoggedInUser? = user

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = null
    }

    fun logout() {
        user = null
        dataSource.logout()
    }

    fun login(username: String, password: String): Result<LoggedInUser> {
        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    fun register(firstname: String, lastname: String, username: String, password: String): Result<LoggedInUser> {
        val result = dataSource.register(firstname, lastname, username, password)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }
}