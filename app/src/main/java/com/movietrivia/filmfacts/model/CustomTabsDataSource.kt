package com.movietrivia.filmfacts.model

import android.content.ComponentName
import android.content.Context
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class CustomTabsDataSource @Inject constructor(
    private val context: Context
) {

    private val _tabSession = MutableStateFlow<CustomTabsSession?>(null)
    val tabSession: StateFlow<CustomTabsSession?> = _tabSession

    fun startSession(): Boolean {
        val customTabs = CustomTabsClient.getPackageName(context, null)
        customTabs?.let {
            if (tabSession.value == null) {
                CustomTabsClient.bindCustomTabsService(
                    context,
                    it,
                    customTabsServiceConnection
                )
            }
        }
        return customTabs != null
    }

    private val customTabsServiceConnection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            client.warmup(0)
            _tabSession.value = client.newSession(null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _tabSession.value = null
        }
    }

    companion object {
        fun customTabsSupported(context: Context) = CustomTabsClient.getPackageName(context, null) != null
    }
}