package com.movietrivia.filmfacts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.movietrivia.filmfacts.api.getAuthUri
import com.movietrivia.filmfacts.model.CustomTabMetaData
import com.movietrivia.filmfacts.model.CustomTabsDataSource
import com.movietrivia.filmfacts.viewmodel.AuthenticationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthenticationActivity : ComponentActivity() {

    private val authenticationViewModel: AuthenticationViewModel by viewModels()

    private var tabStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sometimes we get the response here instead of in the onNewIntent() method
        // Activity backstack is all messed up, so need to manually call main activity and wipe
        // the auth activity from the backstack
        if (intent.data != null) {
            processReturnIntent(intent)
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        } else {
            val metaData = authenticationViewModel.consumeAuthRequest()

            if (CustomTabsDataSource.customTabsSupported(this)) {
                launchCustomTab(metaData)
            } else {
                launchBrowser(metaData)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If the user navigates away from the auth page via pressing the 'x' close button or
        // using the system navigation, the auth activity will resume.  We have nothing to do here,
        // so best just finish.  In the case of an actual approve/deny, the onNewIntent() method
        // will be called before the regular onResume() lifecycle and we just finish there.
        if (tabStarted) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        tabStarted = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processReturnIntent(intent)
        finish()
    }

    private fun processReturnIntent(intent: Intent) {
        val requestToken = intent.data?.getQueryParameter("request_token") ?: ""
        val approved = intent.data?.getQueryParameter("approved").toBoolean()
        if (approved) {
            tabStarted = false
            authenticationViewModel.setPendingSession(requestToken)
        }
    }

    private fun launchCustomTab(metaData: CustomTabMetaData?) {
        if (metaData?.session != null) {
            CustomTabsIntent.Builder()
                .setSession(metaData.session)
                .build()
                .launchUrl(this@AuthenticationActivity, metaData.uri)
        } else {
            lifecycleScope.launch {
                val authResponse = authenticationViewModel.getNewAuthenticationToken()
                authResponse?.let { response ->
                    if (response.success) {
                        CustomTabsIntent.Builder().build().launchUrl(
                            this@AuthenticationActivity,
                            getAuthUri(response.requestToken)
                        )
                    }
                }
            }
        }
    }

    private fun launchBrowser(metaData: CustomTabMetaData?) {
        if (metaData != null) {
            startActivity(Intent(Intent.ACTION_VIEW, metaData.uri))
        } else {
            lifecycleScope.launch {
                val authResponse = authenticationViewModel.getNewAuthenticationToken()
                authResponse?.let { response ->
                    if (response.success) {
                        startActivity(Intent(Intent.ACTION_VIEW, getAuthUri(response.requestToken)))
                    }
                }
            }
        }
    }
}