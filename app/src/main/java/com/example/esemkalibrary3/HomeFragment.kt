package com.example.esemkalibrary3

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.esemkalibrary3.databinding.CardBooksBinding
import com.example.esemkalibrary3.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater)

        binding.rvBooks.layoutManager = GridLayoutManager(requireContext(), 2)

        binding.etSearch.doAfterTextChanged { LoadData(inflater, it.toString()) }
        LoadData(inflater)

        return binding.root
    }

    private fun LoadData(inflater: LayoutInflater, search: String = "") {
        lifecycleScope.launch(Dispatchers.IO) {
            val conn = URL("http://10.0.2.2:5000/Api/Book?searchText=${search}").openConnection() as HttpURLConnection

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = JSONArray(conn.getInputStream().bufferedReader().readText())

                val allBooksList = ArrayList<JSONObject>()
                for (i in 0 until response.length()) {
                    val booksArray = response.getJSONObject(i).getJSONArray("books")

                    for (j in 0 until booksArray.length()) {
                        allBooksList.add(booksArray.getJSONObject(j))
                    }
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    binding.rvBooks.adapter = object : RecyclerView.Adapter<BookViewHolder>() {
                        override fun onCreateViewHolder(
                            parent: ViewGroup,
                            viewType: Int
                        ): BookViewHolder {
                            return BookViewHolder(CardBooksBinding.inflate(inflater, parent, false))
                        }

                        override fun onBindViewHolder(
                            holder: BookViewHolder,
                            position: Int
                        ) {
                            val book = allBooksList[position]

                            holder.binding.tvTitle.text = book.getString("name")
                            holder.binding.tvAuthor.text = book.getString("authors")

                            lifecycleScope.launch(Dispatchers.IO) {
                                val imageURL = URL("http://10.0.2.2:5000/Api/Book/${book.getString("id")}/Photo").openStream()
                                val bitmap = BitmapFactory.decodeStream(imageURL)

                                lifecycleScope.launch(Dispatchers.Main) {
                                    holder.binding.ivImage.setImageBitmap(bitmap)
                                }
                            }

                            holder.itemView.setOnClickListener {
                                val fragment = BookDetailFragment()
                                val bundle = Bundle()
                                bundle.putString("bookId", book.getString("id"))
                                fragment.arguments = bundle
                                if (activity is MainActivity) (activity as MainActivity).changeView(fragment)
                            }
                        }

                        override fun getItemCount(): Int {
                            return allBooksList.size
                        }
                    }
                }
            }
        }
    }

    class BookViewHolder(val binding: CardBooksBinding) : RecyclerView.ViewHolder(binding.root)
}