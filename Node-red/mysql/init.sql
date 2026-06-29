CREATE TABLE IF NOT EXISTS telemetry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id VARCHAR(100) NOT NULL,
  temperature DECIMAL(10,2) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_device_created (device_id, created_at)
);
