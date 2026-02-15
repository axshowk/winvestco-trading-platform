---
description: Starts the entire Winvestco platform locally, including infrastructure, backend microservices, and the frontend terminal.
---

// turbo-all
1. **Start Infrastructure**:
   - Navigate to `backend`.
   - Run `.\start-infra.ps1` to start PostgreSQL, Redis, RabbitMQ, and Kafka.
   - Wait for the script to confirm all services are [OK].
2. **Start Discovery & Gateway**:
   - Start the **Eureka Server**: `./mvnw spring-boot:run -pl eureka-server`.
   - Wait 10 seconds for Eureka to initialize.
   - Start the **API Gateway**: `./mvnw spring-boot:run -pl api-gateway`.
3. **Start Core Services**:
   - Start **User Service**: `./mvnw spring-boot:run -pl user-service`.
   - Start **Market Service**: `./mvnw spring-boot:run -pl market-service`.
4. **Start Frontend Terminal**:
   - Navigate to `frontend`.
   - Run `npm install` (if items are missing).
   - Run `npm run dev`.
5. **Verification**:
   - Open Eureka Dashboard: [http://localhost:8761](http://localhost:8761).
   - Open Winvestco Terminal: [http://localhost:5173](http://localhost:5173).
