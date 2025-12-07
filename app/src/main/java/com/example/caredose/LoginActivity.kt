package com.example.caredose

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.PrePopulateHelper
import com.example.caredose.database.entities.User
import com.example.caredose.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var database: AppDatabase
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        database = AppDatabase.getDatabase(this)
        supportActionBar?.hide()

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            if (isSignUpMode) {
                handleSignup()
            } else {
                handleLogin()
            }
        }

        binding.tvToggleMode.setOnClickListener {
            isSignUpMode = !isSignUpMode
            updateUI()
        }
    }

    private fun updateUI() {
        if (isSignUpMode) {
            binding.tvTitle.text = "Sign Up"
            binding.btnLogin.text = "Sign Up"
            binding.tvToggleMode.text = "Already have an account? Login"
            binding.tilName.visibility = android.view.View.VISIBLE
            binding.tilPhone.visibility = android.view.View.VISIBLE
        } else {
            binding.tvTitle.text = "Login"
            binding.btnLogin.text = "Login"
            binding.tvToggleMode.text = "Don't have an account? Sign Up"
            binding.tilName.visibility = android.view.View.GONE
            binding.tilPhone.visibility = android.view.View.VISIBLE
        }
    }

    private fun handleLogin() {
        val phone = binding.etPhone.text.toString().trim()

        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone is required"
            return
        }

        if (phone.length < 10) {
            binding.etPhone.error = "Invalid phone number"
            return
        }

        lifecycleScope.launch {
            try {
                val user = database.userDao().getUserByPhone(phone)

                if (user != null) {
                    // Login successful
                    sessionManager.saveSession(user.userId)
                    Toast.makeText(this@LoginActivity, "Welcome back, ${user.name}!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this@LoginActivity, "Phone number not found. Please sign up.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignup() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }

        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone is required"
            return
        }

        if (phone.length < 10) {
            binding.etPhone.error = "Invalid phone number"
            return
        }

        lifecycleScope.launch {
            try {
                // Check if phone already exists
                val exists = database.userDao().isPhoneExists(phone)
                if (exists) {
                    Toast.makeText(this@LoginActivity, "Phone number already registered. Please login.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create new user
                val newUser = User(name = name, phone = phone)
                val userId = database.userDao().insert(newUser)

                // Pre-populate master lists for new user
                PrePopulateHelper.initializeUserMasterLists(database, userId)

                // Save session
                sessionManager.saveSession(userId)
                Toast.makeText(this@LoginActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Signup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}