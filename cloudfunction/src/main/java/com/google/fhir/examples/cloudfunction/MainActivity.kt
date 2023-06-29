/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.fhir.examples.cloudfunction

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.fhir.examples.cloudfunction.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import timber.log.Timber

const val MAX_RESOURCE_COUNT = 20

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var drawerToggle: ActionBarDrawerToggle
  private val viewModel: MainActivityViewModel by viewModels()
  private val signInLauncher =
    registerForActivityResult(
      FirebaseAuthUIActivityResultContract(),
    ) { res -> runBlocking { onSignInResult(res) } }

  override fun onCreate(savedInstanceState: Bundle?) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
      val providers = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())
      val signInIntent =
        AuthUI.getInstance()
          .createSignInIntentBuilder()
          .setAvailableProviders(providers)
          .setAlwaysShowSignInMethodScreen(true)
          .setIsSmartLockEnabled(false)
          .build()
      signInLauncher.launch(signInIntent)
    }
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    initActionBar()
    initNavigationDrawer()
    observeLastSyncTime()
    viewModel.updateLastSyncTimestamp()
  }

  override fun onBackPressed() {
    if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
      binding.drawer.closeDrawer(GravityCompat.START)
      return
    }
    super.onBackPressed()
  }

  fun setDrawerEnabled(enabled: Boolean) {
    val lockMode =
      if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
    binding.drawer.setDrawerLockMode(lockMode)
    drawerToggle.isDrawerIndicatorEnabled = enabled
  }

  fun openNavigationDrawer() {
    binding.drawer.openDrawer(GravityCompat.START)
    viewModel.updateLastSyncTimestamp()
  }

  private fun initActionBar() {
    val toolbar = binding.toolbar
    setSupportActionBar(toolbar)
  }

  private fun initNavigationDrawer() {
    binding.navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected)
    drawerToggle = ActionBarDrawerToggle(this, binding.drawer, R.string.open, R.string.close)
    binding.drawer.addDrawerListener(drawerToggle)
    drawerToggle.syncState()
  }

  private fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_sync -> {
        viewModel.triggerOneTimeSync()
        binding.drawer.closeDrawer(GravityCompat.START)
        return false
      }
    }
    return false
  }

  private fun observeLastSyncTime() {
    viewModel.lastSyncTimestampLiveData.observe(this) {
      binding.navigationView.getHeaderView(0).findViewById<TextView>(R.id.last_sync_tv).text = it
    }
  }

  private suspend fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
    val response = result.idpResponse
    if (result.resultCode == RESULT_OK) {
      FhirApplication.dataStore(applicationContext).saveUserIdToken(response?.idpToken!!)
      viewModel.authState.emit("Authenticated")
    } else {
      if (response == null) {
        Timber.i("User cancelled")
      }
      response!!.error?.errorCode
    }
  }
}
