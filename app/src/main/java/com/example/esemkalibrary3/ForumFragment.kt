package com.example.esemkalibrary3

import android.app.Activity.RESULT_OK
import android.content.Context.MODE_PRIVATE
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
import com.example.esemkalibrary3.databinding.CardForumBinding
import com.example.esemkalibrary3.databinding.FragmentForumBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

class ForumFragment : Fragment() {
    private lateinit var binding: FragmentForumBinding
    private var currentUserEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentForumBinding.inflate(inflater)

        val shared = requireContext().getSharedPreferences("token", MODE_PRIVATE)
        val token = shared.getString("token", "")

        binding.rvForum.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.btnAdd.setOnClickListener {if (activity is MainActivity) (activity as MainActivity).changeView(AddThreadFragment())}

        lifecycleScope.launch(Dispatchers.IO) {
            val connMe = URL("http://10.0.2.2:5000/Api/User/Me").openConnection() as HttpURLConnection
            connMe.setRequestProperty("Authorization", "Bearer $token")
            if (connMe.responseCode == 200) {
                val meData = JSONObject(connMe.getInputStream().bufferedReader().readText())
                currentUserEmail = meData.getString("email")
            }

            val conn = URL("http://10.0.2.2:5000/Api/Thread").openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")

            if (conn.responseCode == HttpsURLConnection.HTTP_OK) {
                val data = JSONArray(conn.getInputStream().bufferedReader().readText())

                lifecycleScope.launch(Dispatchers.Main) {
                    binding.rvForum.adapter = object : RecyclerView.Adapter<CardForumViewHolder>() {
                        override fun onCreateViewHolder(
                            parent: ViewGroup,
                            viewType: Int
                        ): CardForumViewHolder {
                            return CardForumViewHolder(CardForumBinding.inflate(inflater, parent, false))
                        }

                        override fun onBindViewHolder(
                            holder: CardForumViewHolder,
                            position: Int
                        ) {
                            val thread = data.getJSONObject(position)
                            val authorData = thread.getJSONObject("createdBy")

                            holder.binding.tvTitle.text = thread.getString("title")
                            holder.binding.tvAuthor.text = authorData.getString("name")

                            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                            val uiFormat = SimpleDateFormat("dd/MMM/yyyy", Locale.getDefault())

                            val latestStr = if (thread.has("lastestReply") && !thread.isNull("lastestReply")) {
                                thread.getString("lastestReply")
                            } else {
                                thread.getString("createdAt")
                            }

                            try {
                                val latestDate = apiFormat.parse(latestStr)
                                holder.binding.tvLatestPost.text = "Latest: ${uiFormat.format(latestDate)}"
                            } catch (e: Exception) {
                                holder.binding.tvLatestPost.text = "Latest: -"
                            }

                            val creatorEmail = authorData.getString("email")
                            holder.binding.btnRemove.isVisible = (creatorEmail == currentUserEmail)

                            holder.binding.btnRemove.setOnClickListener {
                                val alert = MaterialAlertDialogBuilder(requireContext())
                                alert.setTitle("Delete Thread")
                                alert.setMessage("Are you sure you want to delete this thread?")
                                alert.setPositiveButton("Yes") {dialog, which ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val conn = URL("http://10.0.2.2:5000/Api/Thread/${thread.getString("id")}").openConnection() as HttpURLConnection
                                        conn.requestMethod = "DELETE"
                                        conn.setRequestProperty("Authorization", "Bearer $token")

                                        if (conn.responseCode in 200..299) {
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                Toast.makeText(requireContext(), "Delete Success",
                                                    Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                Toast.makeText(requireContext(), conn.errorStream.bufferedReader().readText(),
                                                    Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                alert.setNegativeButton("No") {dialog, which ->
                                    dialog.dismiss()
                                }
                                alert.show()
                            }

                            holder.itemView.setOnClickListener {
                                val fragment = ThreadDetailFragment()
                                val bundle = Bundle()
                                bundle.putString("threadId", thread.getString("id"))
                                fragment.arguments = bundle
                                if (activity is MainActivity) (activity as MainActivity).changeView(fragment)
                            }
                        }

                        override fun getItemCount(): Int {
                            return data.length()
                        }
                    }
                }
            }
        }

        return binding.root
    }

    class CardForumViewHolder(val binding: CardForumBinding) : RecyclerView.ViewHolder(binding.root)
}