package com.example.esemkalibrary3

import android.content.Context.MODE_PRIVATE
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.esemkalibrary3.databinding.FragmentBookDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BookDetailFragment : Fragment() {
    private lateinit var binding: FragmentBookDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBookDetailBinding.inflate(inflater)

        val bookId = arguments?.getString("bookId")

        var shared = requireContext().getSharedPreferences("bookList", MODE_PRIVATE)
        val editor = shared.edit()
        val addedBook = JSONArray(shared.getString("bookList", "[]"))

        var isAdded = false
        for (i in 0 until addedBook.length()) {
            if (addedBook.getJSONObject(i).getString("id") == bookId) {
                isAdded = true
                binding.btnAdd.text = "Remove from My Cart"
                break
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val conn = URL("http://10.0.2.2:5000/Api/Book/${bookId}").openConnection() as HttpURLConnection

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val data = JSONObject(conn.getInputStream().bufferedReader().readText())

                lifecycleScope.launch(Dispatchers.Main) {
                    binding.tvTitle.text = data.getString("name")
                    binding.tvAuthor.text = data.getString("authors")
                    binding.tvIsbn.text = data.getString("isbn")
                    binding.tvPublisher.text = data.getString("publisher")
                    binding.tvAvailable.text = data.getString("available")
                    binding.tvDescription.text = data.getString("description")

                    lifecycleScope.launch(Dispatchers.IO) {
                        val imageURL = URL("http://10.0.2.2:5000/Api/Book/${data.getString("id")}/Photo").openStream()
                        val bitmap = BitmapFactory.decodeStream(imageURL)

                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.ivImage.setImageBitmap(bitmap)
                        }
                    }

                    binding.btnAdd.setOnClickListener {
                        if (isAdded) {
                            for (i in 0 until addedBook.length()) {
                                if (addedBook.getJSONObject(i).getString("id") == bookId) {
                                    addedBook.remove(i)
                                    break
                                }
                            }

                            isAdded = false
                            binding.btnAdd.text = "Add to My Cart"
                        } else {
                            addedBook.put(data)
                            isAdded = true
                            binding.btnAdd.text = "Remove from My Cart"
                        }

                        editor.putString("bookList", addedBook.toString())
                        editor.apply()
                    }
                }
            }
        }

        return binding.root
    }
}