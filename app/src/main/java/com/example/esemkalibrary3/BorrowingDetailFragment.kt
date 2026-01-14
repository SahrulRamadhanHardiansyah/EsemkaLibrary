package com.example.esemkalibrary3

import android.content.Context.MODE_PRIVATE
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.esemkalibrary3.databinding.CardBorrowingDetailBinding
import com.example.esemkalibrary3.databinding.FragmentBorrowingDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class BorrowingDetailFragment : Fragment() {
    private lateinit var binding: FragmentBorrowingDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBorrowingDetailBinding.inflate(inflater)

        val borrowingId = arguments?.getString("borrowingId")
        val shared = requireContext().getSharedPreferences("token", MODE_PRIVATE)
        val token = shared.getString("token", "")

        lifecycleScope.launch(Dispatchers.IO) {
            val conn = URL("http://10.0.2.2:5000/Api/Borrowing/$borrowingId").openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val data = JSONObject(conn.getInputStream().bufferedReader().readText())
                val bookBorrowings = data.getJSONArray("bookBorrowings")

                lifecycleScope.launch(Dispatchers.Main) {
                    val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val uiFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val startStr = data.getString("startAt")
                    val endStr = data.getString("endAt")

                    try {
                        val startDate = apiFormat.parse(startStr)
                        val endDate = apiFormat.parse(endStr)
                        binding.tvDate.text = "${uiFormat.format(startDate)} - ${uiFormat.format(endDate)}"
                    } catch (e: Exception) {
                        binding.tvDate.text = "$startStr - $endStr"
                    }

                    binding.tvStatus.text = data.getString("status")

                    binding.rvBorrowingDetail.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    binding.rvBorrowingDetail.adapter = object :RecyclerView.Adapter<BorrowingDetailViewHolder>() {
                        override fun onCreateViewHolder(
                            parent: ViewGroup,
                            viewType: Int
                        ): BorrowingDetailViewHolder {
                            return BorrowingDetailViewHolder(CardBorrowingDetailBinding.inflate(layoutInflater, parent, false))
                        }

                        override fun onBindViewHolder(
                            holder: BorrowingDetailViewHolder,
                            position: Int
                        ) {
                            val bookHeader = bookBorrowings.getJSONObject(position)
                            val book = bookHeader.getJSONObject("book")

                            holder.binding.tvTitle.text = book.getString("name")
                            holder.binding.tvAuthor.text = book.getString("authors")
                            holder.binding.tvIsbn.text = book.getString("isbn")

                            lifecycleScope.launch(Dispatchers.IO) {
                                val imageURL = URL("http://10.0.2.2:5000/Api/Book/${bookHeader.getString("bookId")}/Photo").openStream()
                                val bitmap = BitmapFactory.decodeStream(imageURL)

                                lifecycleScope.launch(Dispatchers.Main) {
                                    holder.binding.ivImage.setImageBitmap(bitmap)
                                }
                            }
                        }

                        override fun getItemCount(): Int {
                            return bookBorrowings.length()
                        }
                    }
                }
            }
        }

        return binding.root
    }

    class BorrowingDetailViewHolder(val binding: CardBorrowingDetailBinding) : RecyclerView.ViewHolder(binding.root)

}