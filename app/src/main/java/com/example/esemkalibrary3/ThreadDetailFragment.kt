package com.example.esemkalibrary3

import android.content.Context.MODE_PRIVATE
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.esemkalibrary3.databinding.CardThreadMessagesBinding
import com.example.esemkalibrary3.databinding.FragmentThreadDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class ThreadDetailFragment : Fragment() {
    private lateinit var binding: FragmentThreadDetailBinding
    private var currentUserEmail: String = ""
    private var token: String = ""
    private var threadId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentThreadDetailBinding.inflate(inflater)

        val shared = requireContext().getSharedPreferences("token", MODE_PRIVATE)
        token = shared.getString("token", "") ?: ""
        threadId = arguments?.getString("threadId") ?: ""

        binding.rvThreadMessages.layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL, false)

        loadData()

        binding.btnAddReply.setOnClickListener {
            val content = binding.etReply.text.toString()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val conn = URL("http://10.0.2.2:5000/Api/Thread/$threadId").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("content", content)
                }
                conn.outputStream.write(jsonBody.toString().toByteArray())

                if (conn.responseCode in 200..299) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Reply Added", Toast.LENGTH_SHORT).show()
                        binding.etReply.text?.clear()
                        loadData()
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

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val connMe = URL("http://10.0.2.2:5000/Api/User/Me").openConnection() as HttpURLConnection
            connMe.setRequestProperty("Authorization", "Bearer $token")
            if (connMe.responseCode == 200) {
                val meData = JSONObject(connMe.getInputStream().bufferedReader().readText())
                currentUserEmail = meData.getString("email")
            }

            val connThread = URL("http://10.0.2.2:5000/Api/Thread/$threadId").openConnection() as HttpURLConnection
            connThread.setRequestProperty("Authorization", "Bearer $token")

            if (connThread.responseCode == HttpURLConnection.HTTP_OK) {
                val data = JSONObject(connThread.getInputStream().bufferedReader().readText())

                val creator = data.getJSONObject("createdBy")
                val title = data.getString("title")
                val content = data.getString("content")
                val createdAt = data.getString("createdAt")

                val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val uiFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                var dateStr = createdAt
                try {
                    dateStr = uiFormat.format(apiFormat.parse(createdAt)!!)
                } catch (e: Exception) {}

                val headerText = "${creator.getString("name")} - $dateStr"

                // Update UI Header
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.tvTitle.text = title
                    binding.tvThread.text = content
                    binding.tvAuthorPost.text = headerText
                }

                val creatorEmail = creator.getString("email")
                val connImg = URL("http://10.0.2.2:5000/Api/User/$creatorEmail/Photo").openConnection() as HttpURLConnection
                connImg.setRequestProperty("Authorization", "Bearer $token")
                if (connImg.responseCode == 200) {
                    val bitmap = BitmapFactory.decodeStream(connImg.inputStream)
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.ivAuthorProfile.setImageBitmap(bitmap)
                    }
                }

                val posts = if (data.has("posts") && !data.isNull("posts")) data.getJSONArray("posts") else JSONArray()

                lifecycleScope.launch(Dispatchers.Main) {
                    binding.rvThreadMessages.adapter = object : RecyclerView.Adapter<CardThreadMessageViewHolder>() {
                        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardThreadMessageViewHolder {
                            return CardThreadMessageViewHolder(CardThreadMessagesBinding.inflate(layoutInflater, parent, false))
                        }

                        override fun onBindViewHolder(holder: CardThreadMessageViewHolder, position: Int) {
                            val post = posts.getJSONObject(position)
                            val postCreator = post.getJSONObject("createdBy")
                            val postCreatorEmail = postCreator.getString("email")

                            holder.binding.tvReply.text = post.getString("content")

                            var postDate = post.getString("createdAt")
                            try {
                                postDate = uiFormat.format(apiFormat.parse(postDate)!!)
                            } catch (e: Exception) {}
                            holder.binding.tvAuthorPost.text = "${postCreator.getString("name")} - $postDate"

                            lifecycleScope.launch(Dispatchers.IO) {
                                val connPostImg = URL("http://10.0.2.2:5000/Api/User/$postCreatorEmail/Photo").openConnection() as HttpURLConnection
                                connPostImg.setRequestProperty("Authorization", "Bearer $token")
                                if (connPostImg.responseCode == 200) {
                                    val bmp = BitmapFactory.decodeStream(connPostImg.inputStream)
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        holder.binding.ivReplierProfile.setImageBitmap(bmp)
                                    }
                                }
                            }

                            holder.binding.btnRemove.isVisible = (postCreatorEmail == currentUserEmail)

                            holder.binding.btnRemove.setOnClickListener {
                                val dialog = MaterialAlertDialogBuilder(requireContext())
                                dialog.setTitle("Delete Message")
                                dialog.setMessage("Are you sure you want to delete this message?")
                                dialog.setPositiveButton("Yes") { _, _ ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val postId = post.getString("id")
                                        val urlDel = URL("http://10.0.2.2:5000/Api/Thread/$threadId/Post/$postId")
                                        val connDel = urlDel.openConnection() as HttpURLConnection
                                        connDel.requestMethod = "DELETE"
                                        connDel.setRequestProperty("Authorization", "Bearer $token")

                                        if (connDel.responseCode in 200..299) {
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                Toast.makeText(requireContext(), "Message Deleted", Toast.LENGTH_SHORT).show()
                                                loadData()
                                            }
                                        } else {
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                Toast.makeText(requireContext(), "Delete Failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                dialog.setNegativeButton("No", null)
                                dialog.show()
                            }
                        }

                        override fun getItemCount(): Int = posts.length()
                    }
                }
            }
        }
    }

    class CardThreadMessageViewHolder(val binding: CardThreadMessagesBinding) : RecyclerView.ViewHolder(binding.root)
}