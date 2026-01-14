package com.example.esemkalibrary3

import android.content.Context.MODE_PRIVATE
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Selection
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.esemkalibrary3.databinding.CardCartBinding
import com.example.esemkalibrary3.databinding.FragmentMyCartBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MyCartFragment : Fragment() {
    private lateinit var binding: FragmentMyCartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyCartBinding.inflate(inflater)
        binding.rvCart.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val token = requireContext().getSharedPreferences("token", MODE_PRIVATE)
        val shared = requireContext().getSharedPreferences("bookList", MODE_PRIVATE)
        val addedBook = JSONArray(shared?.getString("bookList", "[]"))
        val editor = shared.edit()

        binding.rvCart.adapter = object : RecyclerView.Adapter<CartViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): CartViewHolder {
                return CartViewHolder(CardCartBinding.inflate(inflater, parent, false))
            }

            override fun onBindViewHolder(
                holder: CartViewHolder,
                position: Int
            ) {
                val book = addedBook.getJSONObject(position)

                holder.binding.tvTitle.text = book.getString("name")
                holder.binding.tvAuthor.text = book.getString("authors")
                holder.binding.tvIsbn.text = book.getString("isbn")
                holder.binding.tvAvailable.text = book.getString("available")

                lifecycleScope.launch(Dispatchers.IO) {
                    val imageURL = URL("http://10.0.2.2:5000/Api/Book/${book.getString("id")}/Photo").openStream()
                    val bitmap = BitmapFactory.decodeStream(imageURL)

                    lifecycleScope.launch(Dispatchers.Main) {
                        holder.binding.ivImage.setImageBitmap(bitmap)
                    }
                }

                holder.binding.btnRemove.setOnClickListener {
                    val actualPosition = holder.bindingAdapterPosition
                    if (actualPosition != RecyclerView.NO_POSITION) {
                        addedBook.remove(actualPosition)
                        editor.putString("bookList", addedBook.toString())
                        editor.apply()
                        notifyItemRemoved(actualPosition)
                        notifyItemRangeChanged(actualPosition, addedBook.length())
                    }
                }
            }

            override fun getItemCount(): Int {
                return addedBook.length()
            }
        }

        var startAtForApi = ""
        var endAtForApi = ""

        binding.etInitialDate.setOnClickListener {
            val dtp = MaterialDatePicker.Builder.datePicker()
            dtp.setTitleText("Select Date")
            val builderDate = dtp.build()

            builderDate.addOnPositiveButtonClickListener { selection ->
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val apiFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                apiFormatter.timeZone = TimeZone.getTimeZone("UTC")

                val initialDate = Date(selection)
                binding.etInitialDate.setText(formatter.format(initialDate))
                startAtForApi = apiFormatter.format(initialDate)

                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection
                calendar.add(Calendar.DAY_OF_YEAR, 3)
                val endDate = calendar.time

                binding.etEndDate.setText(formatter.format(endDate))
                endAtForApi = apiFormatter.format(endDate)
            }
            builderDate.show(requireActivity().supportFragmentManager, "DatePicker")
        }

        binding.btnBooking.setOnClickListener {
            if (startAtForApi.isEmpty() || endAtForApi.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (addedBook.length() == 0) {
                Toast.makeText(requireContext(), "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val initialDate = binding.etInitialDate.text.toString()
            val endDate = binding.etEndDate.text.toString()

            val bookIds = JSONArray().apply {
                for (i in 0 until addedBook.length()) {
                    put(addedBook.getJSONObject(i).getString("id"))
                }
            }

            val data = JSONObject().apply {
                put("startAt", startAtForApi)
                put("endAt", endAtForApi)
                put("bookIds", bookIds)
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val conn = URL("http://10.0.2.2:5000/Api/Borrowing").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer ${token.getString("token", "")}")
                conn.getOutputStream().write(data.toString().toByteArray())

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    editor.putString("bookList", "[]").apply()

                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Booking Success", Toast.LENGTH_SHORT).show()
                        while (addedBook.length() > 0) { addedBook.remove(0) }
                        binding.rvCart.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }

        return binding.root
    }

    class CartViewHolder(val binding: CardCartBinding) : RecyclerView.ViewHolder(binding.root)
}