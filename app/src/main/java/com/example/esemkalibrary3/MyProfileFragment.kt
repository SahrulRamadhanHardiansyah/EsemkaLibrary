package com.example.esemkalibrary3

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.esemkalibrary3.databinding.CardBorrowingHistoryBinding
import com.example.esemkalibrary3.databinding.FragmentBorrowingDetailBinding
import com.example.esemkalibrary3.databinding.FragmentMyProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class MyProfileFragment : Fragment() {
    private lateinit var binding: FragmentMyProfileBinding

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent())
    { uri: Uri? ->
        if (uri != null) {uploadPhoto(uri)}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyProfileBinding.inflate(inflater)

        val shared = requireContext().getSharedPreferences("token", MODE_PRIVATE)
        val token = shared.getString("token", "")

        loadUserData()

        binding.btnUpload.setOnClickListener { pickImageLauncher.launch("image/*")}

        binding.btnLogout.setOnClickListener { performLogout() }

        return binding.root
    }

    private fun loadUserData() {
        val shared = requireContext().getSharedPreferences("token", MODE_PRIVATE)
        val token = shared.getString("token", "")

        lifecycleScope.launch(Dispatchers.IO) {
            val connMe = URL("http://10.0.2.2:5000/Api/User/Me").openConnection() as HttpURLConnection
            val connBorrow = URL("http://10.0.2.2:5000/Api/Borrowing").openConnection() as HttpURLConnection
            val connPhoto = URL("http://10.0.2.2:5000/Api/User/Me/Photo").openConnection() as HttpURLConnection
            connMe.setRequestProperty("Authorization", "Bearer $token")
            connBorrow.setRequestProperty("Authorization", "Bearer $token")
            connPhoto.setRequestProperty("Authorization", "Bearer $token")

            if (connMe.responseCode == HttpURLConnection.HTTP_OK && connBorrow.responseCode == HttpURLConnection.HTTP_OK) {
                val userData = JSONObject(connMe.getInputStream().bufferedReader().readText())
                val borrowingData = JSONArray(connBorrow.getInputStream().bufferedReader().readText())

                var bitmap: Bitmap? = null
                try {
                    if (connPhoto.responseCode == HttpURLConnection.HTTP_OK) {
                        bitmap = BitmapFactory.decodeStream(connPhoto.inputStream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    binding.tvName.text = userData.getString("name")
                    binding.tvEmail.text = userData.getString("email")

                    if (bitmap != null) {
                        binding.ivImage.setImageBitmap(bitmap)
                    }

                    binding.rvBorrowingHistory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    binding.rvBorrowingHistory.adapter = object : RecyclerView.Adapter<BorrowHistoryViewHolder>() {
                        override fun onCreateViewHolder(
                            parent: ViewGroup,
                            viewType: Int
                        ): BorrowHistoryViewHolder {
                            return BorrowHistoryViewHolder(CardBorrowingHistoryBinding.inflate(layoutInflater, parent, false))
                        }

                        override fun onBindViewHolder(
                            holder: BorrowHistoryViewHolder,
                            position: Int
                        ) {
                            val borrowing = borrowingData.getJSONObject(position)

                            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                            val uiFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                            val startStr = borrowing.getString("startAt")
                            val endStr = borrowing.getString("endAt")

                            try {
                                val startDate = apiFormat.parse(startStr)
                                val endDate = apiFormat.parse(endStr)
                                holder.binding.tvDate.text = "${uiFormat.format(startDate)} - ${uiFormat.format(endDate)}"
                            } catch (e: Exception) {
                                holder.binding.tvDate.text = "$startStr - $endStr"
                            }

                            holder.binding.tvTotalBooks.text = "${borrowing.getString("book")} Books"
                            holder.binding.tvStatus.text = borrowing.getString("status")

                            holder.itemView.setOnClickListener {
                                val fragment = BorrowingDetailFragment()
                                val bundle = Bundle()
                                bundle.putString("borrowingId", borrowing.getString("id"))
                                fragment.arguments = bundle
                                if (activity is MainActivity) (activity as MainActivity).changeView(fragment)
                            }
                        }

                        override fun getItemCount(): Int {
                            return borrowingData.length()
                        }
                    }
                }
            }

        }
    }

    private fun uploadPhoto(fileUri: Uri) {
        val shared = requireContext().getSharedPreferences("token", MODE_PRIVATE)
        val token = shared.getString("token", "")

        Toast.makeText(requireContext(), "Uploading...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            val boundary = "Boundary-${UUID.randomUUID()}}"
            val conn = URL("http://10.0.2.2:5000/Api/User/Me/Photo").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val outputStream = DataOutputStream(conn.outputStream)
            val inputStream = requireContext().contentResolver.openInputStream(fileUri)

            val buffer = ByteArray(1024)
            var bytesRead: Int

            outputStream.writeBytes("--$boundary\r\n")
            outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"profile.jpg\"\r\n")
            outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")

            while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.writeBytes("\r\n")
            outputStream.writeBytes("--$boundary--\r\n")

            inputStream?.close()
            outputStream.flush()
            outputStream.close()

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Upload Success!", Toast.LENGTH_SHORT).show()
                    loadUserData()
                }
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown Error"
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Upload Failed: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performLogout() {
        val shared = requireContext().getSharedPreferences("token", MODE_PRIVATE)
        val editor = shared.edit()

        editor.clear()
        editor.apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    class BorrowHistoryViewHolder(val binding: CardBorrowingHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}