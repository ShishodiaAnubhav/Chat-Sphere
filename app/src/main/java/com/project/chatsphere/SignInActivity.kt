package com.project.chatsphere

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.project.chatsphere.daos.UserDao
import com.project.chatsphere.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SignInActivity : AppCompatActivity() {

    private val RC_SIGN_IN: Int = 123

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private var hasName = true
    private var hasImg = true
    private lateinit var userName: String

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("507678798326-j6tv94efrnjulp9hqni5b6hpnjv4ae27.apps.googleusercontent.com")
            .requestEmail()
            .build()


        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = Firebase.auth

        findViewById<SignInButton>(R.id.signInButton).setOnClickListener {
            signIn()
        }

        findViewById<Button>(R.id.login).setOnClickListener {
            logIn()
        }
    }


    @DelicateCoroutinesApi
    private fun logIn() {
        val email = findViewById<EditText>(R.id.email_address)
        val password = findViewById<EditText>(R.id.password)

        val emailAddress = email.text.toString()
        val passwordLogIn = password.text.toString()

        if (emailAddress.isEmpty()) {
            Toast.makeText(this, "enter Email Address", Toast.LENGTH_SHORT).show()
        } else if (passwordLogIn.isEmpty()) {
            Toast.makeText(this, "enter Password ", Toast.LENGTH_SHORT).show()
        } else {
            auth.signInWithEmailAndPassword(emailAddress.trim(), passwordLogIn)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val currentUser = auth.currentUser
                        hasImg = false
//                        hasName = false
                        updateUI(currentUser)
                    } else {
                        Toast.makeText(this, "check your email and password", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }

    }


    @OptIn(DelicateCoroutinesApi::class)
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.log_in_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.signUpButton -> {
                val intent = Intent(this, SignUpActivity::class.java)
                startActivity(intent)
                finish()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    @DelicateCoroutinesApi
    private fun handleSignInResult(completeTask: Task<GoogleSignInAccount>) {
        try {
            // Google Sign In was successful, authenticate with Firebase
            val account = completeTask.getResult(ApiException::class.java)!!
            Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Google Sign In failed, update UI appropriately
            Log.w(TAG, "Google sign in failed", e)
        }
    }

    @DelicateCoroutinesApi
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        findViewById<SignInButton>(R.id.signInButton).visibility = View.GONE
        findViewById<EditText>(R.id.email_address).visibility = View.GONE
        findViewById<EditText>(R.id.password).visibility = View.GONE
        findViewById<Button>(R.id.login).visibility = View.GONE
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.IO) {
            val auth = auth.signInWithCredential(credential).await()
            val firebaseUser = auth.user
            withContext(Dispatchers.Main) {
                updateUI(firebaseUser)
            }
        }

    }

    @DelicateCoroutinesApi
    fun updateUI(firebaseUser: FirebaseUser?) {
        if (firebaseUser != null) {
            var name = firebaseUser.displayName
            var imageUrl = firebaseUser.photoUrl.toString()

            if (!hasImg) {
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/chat-sphere-64660.appspot.com/o/account_circle.png?alt=media&token=0b0e9b94-5a64-4f74-8528-9bc3c6a667dc"
            }

            val user = User(firebaseUser.uid, name, imageUrl)
            val userDao = UserDao()
            userDao.addUser(user)


            val mainActivityIntent = Intent(this, MainActivity::class.java)
            startActivity(mainActivityIntent)
            finish()
        } else {
            findViewById<SignInButton>(R.id.signInButton).visibility = View.VISIBLE
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        }
    }

}