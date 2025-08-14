Job Application Portal

A simple Job Application Portal built with Spring Boot (Java) for the backend and HTML, CSS, and JavaScript for the frontend.
This application allows job seekers to view available jobs and apply, and enables admins to post new jobs and manage applications.

🚀 Features

View Jobs – Browse all available job listings.

Apply for Jobs – Submit applications online.

Admin Dashboard – Post jobs and view/manage applications.

REST API – Backend endpoints for jobs and applications.

Spring Boot Backend – Fast, secure, and scalable backend using Java.

Frontend in HTML/CSS/JS – Lightweight, responsive UI.

🛠 Installation & Setup

Prerequisites

Java 17+

Maven 3+

Any IDE (IntelliJ / Eclipse / VS Code with Java support)


Steps

1. Clone the repository

git clone https://github.com/yourusername/job-application-portal.git
cd job-application-portal


2. Build & Run

mvn spring-boot:run


3. Access the App

Frontend: http://localhost:8080

API Endpoints: http://localhost:8080/api/...




🔌 API Endpoints (Sample)

GET /jobs – Get all jobs

POST /jobs – Add a new job (Admin)

POST /applications – Submit a job application


📌 In Development

User Authentication – Login & signup for both applicants and admins.

Company Module – Manage company profiles and posted jobs.

Application Status Tracking – Let applicants see their application progress.

Search & Filter Jobs – Advanced search options for job seekers.

Database Migration – From in-memory/H2 to MySQL or PostgreSQL.

Responsive Design Upgrade – Make the UI fully mobile-friendly.


📜 License

This project is open-source and available under the MIT License.
