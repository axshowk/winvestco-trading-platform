---
description: Automatically scans the codebase and updates the root README.md with the latest architecture, tech stack, and setup instructions.
---

// turbo-all

***Very Important***
Foremost you have to analyse the exsisting README.md and compare what has been changed 
 
1. **Analyze Project Structure**:
   - Scan `backend` for services (Spring Boot, Java, etc.).
   - Scan `frontend` for tech stack (React, Next.js, etc.).
   - Scan `observability` and `infrastructure` folders.
2. **Collect Tech Stack Details**:
   - Identify versions of Java, Spring Boot, Node.js.
   - List databases (PostgreSQL, Redis) and messaging (Kafka, RabbitMQ) from `start-infra.ps1` or configs.
3. **Update README**:
***Very Important***
Foremost you have to analyse the exsisting README.md and compare what has been changed 
   - Update `README.md` with:
     - Project Overview
     - Tech Stack (with versions)
     - Project Structure
     - Setup & Installation (Local Infrastructure)
     - Service Endpoints (if known)

4. **Finalize**:
   - Inform user.