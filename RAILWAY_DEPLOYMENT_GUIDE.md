# 🚀 Railway Low-RAM Deployment Guide (EnergyPulse)

Bu kılavuz, EnergyPulse projesinin Railway platformunda çökmeler yaşanmadan düşük RAM tüketimiyle (RAM spike / OOM 137 engelleyici) sorunsuz çalışması için hazırlanan yapılandırma rehberidir.

---

## 🛠️ Oluşturulan Dağıtım Dosyaları (Deployment Artifacts)

Proje içerisine tüm servisler için otomatik RAM limitleyici ve optimizasyon dosyaları eklenmiştir:

1. **`energypulse-core`**:
   - `Dockerfile`: Multi-stage build + `JAVA_TOOL_OPTIONS="-Xmx384m -Xms256m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"`
   - `nixpacks.toml`: Nixpacks ile dağıtım için varsayılan JVM bellek limitleri.

2. **`sensor-simulator`**:
   - `Dockerfile`: Multi-stage build + `JAVA_TOOL_OPTIONS="-Xmx256m -Xms128m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"`
   - `nixpacks.toml`: Nixpacks ile dağıtım için varsayılan JVM bellek limitleri.

3. **`energypulse-web` (React Frontend)**:
   - `Dockerfile`: Nginx tabanlı süper hafif dağıtım (~15MB RAM kullanımı).
   - `nginx.conf`: React SPA yönlendirme (try_files) ayarları.
   - `nixpacks.toml`: Static serve ayarları.

---

## ⚙️ Railway Servis Yapılandırmaları ve Değişkenler (Environment Variables)

Railway üzerinde açacağınız her bir servis için aşağıdaki değişkenleri (Variables) ekleyin:

### 1. `energypulse-core` (Spring Boot REST API)
* **Root Directory / Context:** `/energypulse-core`
* **Variables:**
  ```env
  JAVA_TOOL_OPTIONS="-Xmx384m -Xms256m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
  IGNITE_ENABLED=false
  PORT=8080
  POSTGRES_HOST=${{PostgreSQL.RAILWAY_PRIVATE_DOMAIN}}
  POSTGRES_PORT=5432
  POSTGRES_DB=${{PostgreSQL.POSTGRES_DB}}
  POSTGRES_USER=${{PostgreSQL.POSTGRES_USER}}
  POSTGRES_PASSWORD=${{PostgreSQL.POSTGRES_PASSWORD}}
  KAFKA_BOOTSTRAP_SERVERS=${{Kafka.RAILWAY_PRIVATE_DOMAIN}}:9092
  ```

---

### 2. `sensor-simulator` (Spring Boot Simülatör)
* **Root Directory / Context:** `/sensor-simulator`
* **Variables:**
  ```env
  JAVA_TOOL_OPTIONS="-Xmx256m -Xms128m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
  KAFKA_BOOTSTRAP_SERVERS=${{Kafka.RAILWAY_PRIVATE_DOMAIN}}:9092
  SIMULATOR_INTERVAL_MS=3000
  ```

---

### 3. `energypulse-web` (React Frontend)
* **Root Directory / Context:** `/energypulse-web`
* **Variables:**
  ```env
  VITE_API_BASE_URL=https://${{energypulse-core.RAILWAY_PUBLIC_DOMAIN}}
  ```

---

### 4. `Kafka` (Railway Apache Kafka Container)
Eğer Railway Docker Compose veya Kafka kullanıyorsanız:
* **Variables:**
  ```env
  KAFKA_HEAP_OPTS="-Xms256m -Xmx256m"
  ```

---

## ⚡ Beklenen RAM Kullanım Sonuçları

| Servis | Yapılandırma Öncesi RAM | Yapılandırma Sonrası RAM | Durum |
| :--- | :--- | :--- | :--- |
| **energypulse-core** | ~1.5 GB - 2 GB (Çökme) | ~350 MB - 400 MB | ✅ Stabil |
| **sensor-simulator** | ~800 MB - 1.2 GB (Çökme) | ~200 MB - 250 MB | ✅ Stabil |
| **energypulse-web** | ~400 MB (Dev mode) | ~15 MB - 25 MB (Nginx/Dist) | ✅ Stabil |
| **Apache Kafka** | ~1 GB | ~256 MB | ✅ Stabil |
| **TOPLAM TAHMİNİ** | **> 4.5 GB** | **~850 MB** | 🚀 **5 Kat RAM Tasarrufu** |
