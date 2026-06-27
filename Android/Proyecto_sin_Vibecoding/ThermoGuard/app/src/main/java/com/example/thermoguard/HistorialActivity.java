package com.example.thermoguard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class TelemetryEntry {
    private final int id;
    private final String deviceId;
    private final double temperature;
    private final String createdAt;

    public TelemetryEntry(int id, String deviceId, double temperature, String createdAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public double getTemperature() { return temperature; }
    public String getCreatedAt() { return createdAt; }
}

public class HistorialActivity extends AppCompatActivity {

    private RecyclerView rvHistorial;
    private ProgressBar progressBar;
    private TextInputEditText etApiIp;
    private TextInputEditText etDeviceId;
    private TextInputEditText etLimit;
    private MaterialButton btnRefresh;

    private List<TelemetryEntry> historialList = new ArrayList<>();
    private HistorialAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        rvHistorial = findViewById(R.id.rvHistorial);
        progressBar = findViewById(R.id.progressBar);
        etApiIp     = findViewById(R.id.etApiIp);
        etDeviceId  = findViewById(R.id.etDeviceId);
        etLimit     = findViewById(R.id.etLimit);
        btnRefresh  = findViewById(R.id.btnRefresh);
        MaterialButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Cargar valores iniciales de constantes
        etDeviceId.setText(Constants.DEFAULT_DEVICE_ID);
        
        // Valor por defecto de la IP
        etApiIp.setText("192.168.1.93");

        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistorialAdapter(historialList);
        rvHistorial.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> fetchHistorial());

        // Carga inicial
        fetchHistorial();
    }

    private void fetchHistorial() {
        String ip = etApiIp.getText().toString().trim();
        String deviceId = etDeviceId.getText().toString().trim();
        String limitStr = etLimit.getText().toString().trim();
        
        if (ip.isEmpty()) {
            etApiIp.setError("Ingresa la IP del servidor");
            return;
        }
        
        int limit;
        try {
            limit = Integer.parseInt(limitStr);
        } catch (NumberFormatException e) {
            limit = 50;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);
        
        final int finalLimit = limit;
        new Thread(() -> {
            try {
                // Construir la URL base
                String baseUrl = "http://" + ip + ":1880/api/historial";
                
                // Construir Query Parameters
                List<String> queryParams = new ArrayList<>();
                queryParams.add("limit=" + finalLimit);
                if (!deviceId.isEmpty()) {
                    queryParams.add("device_id=" + deviceId);
                }
                
                StringBuilder urlBuilder = new StringBuilder(baseUrl);
                urlBuilder.append("?");
                for (int i = 0; i < queryParams.size(); i++) {
                    urlBuilder.append(queryParams.get(i));
                    if (i < queryParams.size() - 1) {
                        urlBuilder.append("&");
                    }
                }
                
                String urlString = urlBuilder.toString();
                URL url = new URL(urlString);
                Log.d("HISTORIAL", "Fetching from: " + urlString);
                
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        response.append(scanner.nextLine());
                    }
                    scanner.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray jsonArray = jsonObject.getJSONArray("data");
                    
                    historialList.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        historialList.add(new TelemetryEntry(
                            obj.getInt("id"),
                            obj.getString("device_id"),
                            obj.getDouble("temperature"),
                            obj.getString("created_at")
                        ));
                    }

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                        btnRefresh.setEnabled(true);
                    });
                } else {
                    final int responseCode = conn.getResponseCode();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnRefresh.setEnabled(true);
                        Toast.makeText(HistorialActivity.this, "Error: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("HISTORIAL", "Error fetching data", e);
                String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                    Toast.makeText(HistorialActivity.this, "Error de red: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private static class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {

        private final List<TelemetryEntry> items;

        public HistorialAdapter(List<TelemetryEntry> items) {
            this.items = items;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvTemp;
            final TextView tvDate;
            final TextView tvDeviceId;

            public ViewHolder(View view) {
                super(view);
                tvTemp = view.findViewById(R.id.tvTemp);
                tvDate = view.findViewById(R.id.tvDate);
                tvDeviceId = view.findViewById(R.id.tvDeviceId);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_historial, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TelemetryEntry item = items.get(position);
            holder.tvTemp.setText(item.getTemperature() + "°C");
            holder.tvDate.setText(item.getCreatedAt());
            holder.tvDeviceId.setText(item.getDeviceId());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
