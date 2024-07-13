package com.movietrivia.filmfacts.model

suspend fun <T> makeNetworkCall(requestsDataSource: TooManyRequestsDataSource, networkCall: suspend () -> T): T? {
    return try {
        if (requestsDataSource.requestsAllowed()) {
            networkCall()
        } else {
            null
        }
    } catch (e: Exception) {
        requestsDataSource.processException(e)
        null
    }
}