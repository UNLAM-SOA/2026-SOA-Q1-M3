package com.example.thermoguard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

data class TelemetryEntry(
    val id: Int,
    val deviceId: String,
    val temperature: Double,
    val createdAt: String
)

class HistorialActivity : AppCompatActivity() {

    private lateinit var rvHistorial: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val historialList = mutableListOf<TelemetryEntry>()
    private lateinit var adapter: HistorialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        rvHistorial = findViewById(R.id.rvHistorial)
        progressBar = findViewById(R.id.progressBar)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        rvHistorial.layoutManager = LinearLayoutManager(this)
        adapter = HistorialAdapter(historialList)
        rvHistorial.adapter = adapter

        fetchHistorial()
    }

    private fun fetchHistorial() {
        progressBar.visibility = View.VISIBLE
        
        Thread {
            try {
                val urlString = "${Constants.API_HISTORIAL}?device_id=${Constants.DEFAULT_DEVICE_ID}&limit=50"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    val scanner = Scanner(conn.inputStream)
                    val response = StringBuilder()
                    while (scanner.hasNextLine()) {
                        response.append(scanner.nextLine())
                    }
                    scanner.close()

                    val jsonObject = JSONObject(response.toString())
                    val jsonArray = jsonObject.getJSONArray("data")
                    historialList.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        historialList.add(
                            TelemetryEntry(
                                obj.getInt("id"),
                                obj.getString("device_id"),
                                obj.getDouble("temperature"),
                                obj.getString("created_at")
                            )
                        )
                    }

                    runOnUiThread {
                        adapter.notifyDataSetChanged()
                        progressBar.visibility = View.GONE
                    }
                } else {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error: ${conn.responseCode}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("HISTORIAL", "Error fetching data", e)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    class HistorialAdapter(private val items: List<TelemetryEntry>) :
        RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTemp: TextView = view.findViewById(R.id.tvTemp)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvDeviceId: TextView = view.findViewById(R.id.tvDeviceId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_historial, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTemp.text = "${item.temperature}°C"
            holder.tvDate.text = item.createdAt
            holder.tvDeviceId.text = item.deviceId
        }

        override fun getItemCount() = items.size
    }
}
