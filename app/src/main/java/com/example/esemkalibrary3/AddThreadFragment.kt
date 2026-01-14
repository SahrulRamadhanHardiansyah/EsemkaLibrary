package com.example.esemkalibrary3

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.example.esemkalibrary3.databinding.FragmentAddThreadBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AddThreadFragment : Fragment() {
    private lateinit var binding: FragmentAddThreadBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddThreadBinding.inflate(inflater)

        val shared = requireContext().getSharedPreferences("token", MODE_PRIVATE)
        val token = shared.getString("token", "")

        binding.btnAdd.setOnClickListener {
            val subject = binding.etSubject.text.toString()
            val body = binding.etBody.text.toString()

            if (subject.isNullOrEmpty() || body.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = JSONObject().apply {
                put("subject", subject)
                put("body", body)
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val conn = URL("http://10.0.2.2:5000/Api/Thread").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("content-type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.getOutputStream().write(data.toString().toByteArray())

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Add Thread Success", Toast.LENGTH_SHORT).show()
                        if (activity is MainActivity) (activity as MainActivity).changeView(ForumFragment())
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), conn.errorStream.bufferedReader().readText(), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return binding.root
    }
}