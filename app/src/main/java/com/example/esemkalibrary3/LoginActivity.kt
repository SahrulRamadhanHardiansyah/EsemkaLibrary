package com.example.esemkalibrary3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.esemkalibrary3.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val shared = getSharedPreferences("token", MODE_PRIVATE)
        val editor = shared.edit()

        if (shared.getString("token", null) != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.btnSignUp.setOnClickListener { startActivity(Intent(this, SignUpActivity::class.java)) }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val conn = URL("http://10.0.2.2:5000/Api/Auth").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("content-type", "application/json")
                conn.getOutputStream().write(data.toString().toByteArray())

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val token = JSONObject(conn.getInputStream().bufferedReader().readText()).getString("token")

                    editor.putString("token", token)
                    editor.apply()

                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Login Success", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity ,MainActivity::class.java))
                        finish()
                    }
                } else if (conn.responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, conn.errorStream.bufferedReader().readText(), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}