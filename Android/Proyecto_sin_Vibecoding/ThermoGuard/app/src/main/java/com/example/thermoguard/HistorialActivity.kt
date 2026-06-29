package com.example.thermoguard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Modelo de datos para registros de telemetría. */
data class TelemetryEntry(val dev: String, val temp: Double, val date: String)

/**
 * ACTIVIDAD: HistorialActivity
 * DESCRIPCIÓN: Consulta y lista registros históricos de telemetría desde el servidor API.
 */
class HistorialActivity : AppCompatActivity() {

    private lateinit var pb: ProgressBar
    private lateinit var etIp: TextInputEditText
    private lateinit var etId: TextInputEditText
    private lateinit var etLimit: TextInputEditText
    private lateinit var btn: MaterialButton
    private val dataList = mutableListOf<TelemetryEntry>()
    private lateinit var adapter: HistorialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        pb = findViewById(R.id.progressBar)
        etIp = findViewById(R.id.etApiIp)
        etId = findViewById(R.id.etDeviceId)
        etLimit = findViewById(R.id.etLimit)
        btn = findViewById(R.id.btnRefresh)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        etId.setText(Constants.DEFAULT_DEVICE_ID)
        etIp.setText(Constants.API_BASE_IP)

        val rv = findViewById<RecyclerView>(R.id.rvHistorial)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = HistorialAdapter(dataList)
        rv.adapter = adapter

        btn.setOnClickListener { fetchData() }
        fetchData()
    }

    /** Realiza la petición HTTP GET asíncrona para obtener el JSON de registros. */
    private fun fetchData() {
        val ip = etIp.text.toString().trim()
        val dev = etId.text.toString().trim()
        val limit = etLimit.text.toString().trim()
        if (ip.isEmpty()) return

        pb.visibility = View.VISIBLE
        btn.isEnabled = false
        Thread {
            try {
                var urlS = "http://$ip:1880${Constants.API_HISTORIAL_PATH}?limit=$limit"
                if (dev.isNotEmpty()) urlS += "&device_id=$dev"
                val conn = URL(urlS).openConnection() as HttpURLConnection
                if (conn.responseCode == 200) {
                    val res = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONObject(res).getJSONArray("data")
                    dataList.clear()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        dataList.add(
                            TelemetryEntry(
                                o.getString("device_id"),
                                o.getDouble("temperature"),
                                o.getString("created_at")
                            )
                        )
                    }
                    runOnUiThread {
                        adapter.notifyDataSetChanged()
                        pb.visibility = View.GONE
                        btn.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    pb.visibility = View.GONE
                    btn.isEnabled = true
                }
            }
        }.start()
    }

    /** Adaptador para renderizar los ítems de telemetría en el RecyclerView. */
    private inner class HistorialAdapter(private val list: List<TelemetryEntry>) :
        RecyclerView.Adapter<HistorialAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val t: TextView = v.findViewById(R.id.tvTemp)
            val d: TextView = v.findViewById(R.id.tvDate)
            val id: TextView = v.findViewById(R.id.tvDeviceId)
        }

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
            return VH(LayoutInflater.from(p.context).inflate(R.layout.item_historial, p, false))
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val e = list[pos]
            h.t.text = String.format(Locale.getDefault(), getString(R.string.temp_format), e.temp)
            h.d.text = e.date
            h.id.text = e.dev
        }

        override fun getItemCount(): Int = list.size
    }
}
