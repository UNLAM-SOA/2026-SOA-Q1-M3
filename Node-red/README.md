# Gateway IoT Local con Docker

Arquitectura basada en tu imagen:
- ESP32 publica telemetria en MQTT: `grupoM3/sensor/temperatura`
- Node-RED toma ese dato y lo guarda en MySQL
- App movil envia comando por API REST a Node-RED
- Node-RED publica comando por MQTT en `grupoM3/comandos`
- API REST permite consultar historial en MySQL

Nota de compatibilidad con el firmware actual del ESP32:
- El ESP32 se conecta a tu broker local `192.168.0.11:1883` (Mosquitto en Docker).
- Node-RED usa ese mismo broker local para recibir telemetria y enviar comandos.
- Los comandos MQTT que entiende el ESP32 son texto plano: `ENCENDER` y `APAGAR`.

## 1) Puertos y acceso local

Estos puertos quedan publicados desde Docker:
- `1883` MQTT TCP (ESP32)
- `9001` MQTT WebSocket (opcional app/web)
- `1880` Node-RED y API REST
- `3306` MySQL
- `8080` phpMyAdmin

Variable importante en `.env`:
- `BIND_IP=0.0.0.0` permite acceso desde otros dispositivos de tu red (ESP32 y smartphone).
- `BIND_IP=127.0.0.1` solo acceso desde tu PC.

Si tu ESP32 esta en la misma red WiFi que tu PC, en el firmware usa la IP local de tu PC (ejemplo `192.168.1.25`) y puerto `1883`.

## 2) Levantar el entorno

1. Copia `.env.example` a `.env` y ajusta contrasenas.
2. Ejecuta:

```bash
docker compose up -d --build
```

3. Verifica servicios:

```bash
docker compose ps
```

## 3) Endpoints REST

Node-RED expone:

- POST `http://TU_PC:1880/api/comando`

Body JSON ejemplo:

```json
{
  "device_id": "esp32-01",
  "comando": "ENCENDER"
}
```

- GET `http://TU_PC:1880/api/historial?device_id=esp32-01&limit=50`

### 3.1) Ejemplos para la app (Android/iOS)

#### POST comando (encender)

Request:

```http
POST /api/comando HTTP/1.1
Host: 192.168.0.11:1880
Content-Type: application/json

{"device_id":"esp32-01","comando":"ENCENDER"}
```

Response esperada:

```json
{
  "ok": true,
  "enviado": true,
  "device_id": "esp32-01",
  "comando": "ENCENDER"
}
```

#### POST comando (apagar)

Ejemplo:

```json
{"device_id":"esp32-01","comando":"APAGAR"}
```

Response esperada:

```json
{
  "ok": true,
  "enviado": true,
  "device_id": "esp32-01",
  "comando": "APAGAR"
}
```

#### GET historial

Request:

```http
GET /api/historial?device_id=esp32-01&limit=10 HTTP/1.1
Host: 192.168.0.11:1880
```

Response esperada:

```json
{
  "ok": true,
  "device_id": "esp32-01",
  "total": 2,
  "data": [
    {
      "id": 3,
      "device_id": "esp32-01",
      "temperature": 28.7,
      "created_at": "2026-06-14T00:26:07.000Z"
    }
  ]
}
```

#### Errores comunes

- Si `comando` viene vacio, responde `400`.
- Si `comando` no es `ENCENDER` o `APAGAR`, responde `400`.

## 3.2) Ver base de datos en navegador (phpMyAdmin)

- URL: `http://TU_PC:8080`
- Servidor: `mysql`
- Usuario: `userGrupoM3Soa`
- Password: `passGrupoM3Soa`
- Base de datos: `iot`

## 4) Formato recomendado para telemetria MQTT

Publicar en topic `grupoM3/sensor/temperatura`:

```json
24.5
```

Si el ESP32 envia solo un numero, Node-RED guarda `device_id = esp32-01` por defecto.

## 5) Pruebas rapidas desde PC

### Enviar comando desde API:

```bash
curl -X POST http://localhost:1880/api/comando \
  -H "Content-Type: application/json" \
  -d "{\"device_id\":\"esp32-01\",\"comando\":\"ENCENDER\"}"
```

### Consultar historial:

```bash
curl "http://localhost:1880/api/historial?device_id=esp32-01&limit=10"
```

### PowerShell

Enviar comando:

```powershell
$body = @{ device_id = 'esp32-01'; comando = 'ENCENDER' } | ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri "http://localhost:1880/api/comando" -ContentType "application/json" -Body $body
```

Consultar historial:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:1880/api/historial?device_id=esp32-01&limit=10"
```

## 6) Configuracion ESP32

- Broker MQTT: IP local de tu PC
- Puerto MQTT: `1883`
- Topic publish: `grupoM3/sensor/temperatura`
- Topic subscribe: `grupoM3/comandos`

## 7) Firewall en Windows

Si no conecta desde ESP32 o celular:
- Habilita reglas de entrada para puertos `1883`, `1880`, `9001`.
- Verifica que tu red de Windows este como Privada.
