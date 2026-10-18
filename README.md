# Insurance Management System (Java Swing + SQLite)

A small college mini-project that can be imported into Eclipse as a Maven project.

## Features
- Login screen
- Customer registration and listing
- Insurance policy creation and listing
- Premium estimate calculator (coverage amount × rate / 100)
- Claims registration and listing
- Search customers, policies, and claims
- Basic report counts
- Local SQLite database created automatically

## Requirements
- JDK 17 or newer
- Eclipse IDE for Java Developers
- Internet connection on the first Maven build to download the SQLite JDBC dependency

## Demo login
- Username: `admin`
- Password: `admin123`

This is a college demonstration project. Do not use real customer or financial information.

## Import into Eclipse
1. Extract `InsuranceManagementSystem.zip`.
2. Open Eclipse.
3. Select **File → Import**.
4. Expand **Maven → Existing Maven Projects** and click **Next**.
5. Browse to the extracted `InsuranceManagementSystem` folder.
6. Select the `pom.xml` project and click **Finish**.
7. Wait for Maven dependencies to download. This may take a few minutes on a slow connection.
8. In Package Explorer, expand `src/main/java → com.college.insurance`.
9. Right-click `Main.java` → **Run As → Java Application**.

## Run from a terminal (optional)
From the project folder, run:
```bash
mvn clean compile exec:java
```

The database file `insurance.db` is created in the project working directory on first launch.
