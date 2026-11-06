package com.ludo.game

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application class with Hilt setup.
 */
@HiltAndroidApp
class LudoApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Sign in anonymously on app start
        signInAnonymously()
    }
    
    private fun signInAnonymously() {
        CoroutineScope(Dispatchers.IO).launch {
            val auth = FirebaseAuth.getInstance()
            
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener {
                        android.util.Log.d("LudoApp", "Anonymous sign-in successful: ${it.user?.uid}")
                    }
                    .addOnFailureListener {
                        android.util.Log.e("LudoApp", "Anonymous sign-in failed", it)
                    }
            }
        }
    }
}
