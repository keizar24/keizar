package org.keizar.client

/**
 * A provider for access token.
 *
 * It is designed to provide access token for [Client],
 * not intended to be used via dependency injection in view models.
 */
interface AccessTokenProvider {
    /**
     * Get the current access token.
     * If the token is not available, e.g. user not logged in, return `null`.
     */
    suspend fun getAccessToken(): String?

    /**
     * Invalidate the current access token if any.
     */
    suspend fun invalidateToken()
}