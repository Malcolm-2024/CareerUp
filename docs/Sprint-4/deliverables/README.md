<h1 align="center">CareerUp</h1>

## Run Sprint 4 (Windows)

Extract the complete Windows download and open **CareerUp.exe**. Java is included. The launcher uses an installed MySQL Server 8 or automatically downloads a verified private MySQL copy on first run (internet needed only for that download). No PowerShell, execution-policy changes or manual Java installation are required. The browser opens automatically.

All demo passwords: **CareerUp4!**

| Role | Email | Working Sprint 4 function |
|---|---|---|
| Student | student@careerup.demo | Browse/search student discovery feed |
| Recruiter | recruiter@careerup.demo | Search by major, skill and graduation year |
| Company Representative | company@careerup.demo | Browse this company's verification requests |
| Career Advisor | advisor@careerup.demo | Assigned student search, profile and portfolio |
| Administrator | admin@careerup.demo | Search/filter/page through users |

The **Team Tools** folder contains **Stop CareerUp.cmd**, **Rebuild CareerUp.cmd**, and **Pull Latest and Run.cmd** for development. These buttons use the native Java launcher, not PowerShell. Source Git checkouts can use **Team Tools/Start CareerUp.cmd** with the JDK downloaded through IntelliJ. Rebuild installs Node/Maven privately if needed. The numbered source comments and EDIT GUIDE.txt support Ctrl+F during the demonstration.

Mac: open **Start CareerUp.command** in the same download. On macOS 15+, it detects Intel or Apple Silicon and downloads private, SHA256-checked Java 21/MySQL 8 components on first use. No Homebrew or manual Java/MySQL installation is required. Internet is needed for those downloads. Mac startup has not been executed on a real Mac here. Mac rebuilds automatically download missing Node/Maven build tools; Mac development shortcuts are also in **Team Tools**. Nothing disables macOS security checks.

For an existing Sprint 3 database: configure `DB_URL`, `DB_USER`, and `DB_PASSWORD`, then run the built backend jar. The default application never imports, drops or truncates tables. The optional `demo` Spring profile adds reserved sample accounts/posts once; do not enable it against a shared production database. `db/sprint4_schema.sql` is the original structure without destructive reset statements; `sprint4_seed.sql` is for a new empty database only. Original Sprint 3 files remain unchanged. `.local` contains machine-specific demo database and credentials and must never be committed.

<p align="center">
  A media-first professional networking platform connecting college students with employers.
</p>

<br>

## Project Description
CareerUp is a media-first professional social networking platform designed to connect college students with employers across multiple academic disciplines.

Instead of relying primarily on traditional text-based resumes, CareerUp allows students to showcase projects, internships, research, creative work, and other accomplishments through short videos, images, descriptions, and skill tags.

Recruiters and employers can discover students based on demonstrated work, review portfolios, communicate with candidates, post job and internship opportunities, and invite students to apply.

## Team Members
- Team Member 1 - Nafee Ahmed
- Team Member 2 - Isaiah Alfred
- Team Member 3 - Jordan Monts
- Team Member 4 - Malcolm Richards

## User Roles
CareerUp includes five primary user roles:

- Student
- Recruiter
- Company Representative
- Career Advisor
- Administrator

## Selected Technology
- Programming Language: Java, HTML/CSS, Javascript
- Framework: Spring Boot (Backend), React (Frontend) 
- Database: mySql
- Development Environment / IDE: Intellij and Visual Studio
- Testing Framework / Mechanism: JUnit (Backend), Jest (Frontend)
- Version Control: Git
- app mokup : figma

## Repository Structure
```text
CareerUp/
├── src/
├── docs/
└── README.md
```

## Basic Setup Instructions
-  Prerequisites: Git, Java, Node.js, and MySQL
- Clone the CareerUp repository to your computer.
- Pull the latest version of the repository before making changes.
- Open the backend project using IntelliJ IDEA.
- Open the frontend project using Visual Studio Code.
- Run the Spring Boot backend and React frontend from their development environments.

## Selected Technology
- Programming Language: Java, HTML/CSS, Javascript
- Framework: Spring Boot (Backend), React (Frontend)
- Database: mySql
- Development Environment / IDE: Intellij and Visual Studio
- Testing Framework / Mechanism: JUnit (Backend), Jest (Frontend)
<<<<<<< HEAD


=======
- Version Control: Git
>>>>>>> b75fdf062aabdf801e7af5dde6f105135aef3697
