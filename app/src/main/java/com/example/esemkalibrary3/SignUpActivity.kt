package com.example.esemkalibrary3

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.esemkalibrary3.databinding.ActivitySignUpBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (name.isNullOrEmpty()|| email.isNullOrEmpty() || password.isNullOrEmpty() || confirmPassword.isNullOrEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!password.any{it.isUpperCase()} || !password.any{it.isLowerCase()} || !password.any{it.isDigit()}) {
                Toast.makeText(this, "Password must contain at least 1 uppercase, 1 lowercase, and 1 digit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password and Confirm Password must be the same", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = JSONObject().apply {
                put("name", name)
                put("password", password)
                put("email", email)
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val conn = URL("http://10.0.2.2:5000/Api/Users").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("content-type", "application/json")
                conn.getOutputStream().write(data.toString().toByteArray())

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread {
                        Toast.makeText(this@SignUpActivity, "Sign Up Success", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@SignUpActivity , LoginActivity::class.java))
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SignUpActivity, conn.errorStream.bufferedReader().readText(), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}