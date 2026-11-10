# warehouse-system

Warehouse Management System: a JavaFX desktop application with Hibernate and PostgreSQL.

## Requirements

- JDK 21 or newer (`java -version` must work in the terminal)
- PostgreSQL with an empty database called `warehouse_db`
- Maven, only for building (IntelliJ includes it)

## Database settings

Copy `db.properties.example` to `db.properties` in the folder you start the program from, and enter your own PostgreSQL settings:

```
db.url=jdbc:postgresql://localhost:5432/warehouse_db
db.username=postgres
db.password=your-password
```

`db.properties` is ignored by Git, so each person keeps their own password. Without it, the values in `hibernate.cfg.xml` are used. The tables are created automatically on the first start.

## Build

```
mvn package
```

This creates `target/warehouse-system.jar`, a single file that contains the program and all libraries. In IntelliJ: Maven panel → Lifecycle → package.

## Run without IntelliJ

From the project folder:

```
java -jar target/warehouse-system.jar
```

Or double-click `run.bat` on Windows, or run `./run.sh` on macOS/Linux.

To run it on another computer, copy `warehouse-system.jar` and `db.properties` into one folder and run `java -jar warehouse-system.jar` there. Build the JAR on the same operating system it will run on, because JavaFX includes platform-specific files.

The first start creates the administrator account `admin` / `admin`.
