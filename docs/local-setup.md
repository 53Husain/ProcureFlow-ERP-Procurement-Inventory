# Run the local demo

Prerequisites: JDK 21 (set JAVA_HOME if needed), Docker with Compose v2 supporting
`up --wait`, and Git. Maven does not need to be installed. The Apache Maven
Wrapper 3.3.4 scripts download Maven 3.9.9 on first use; internet access is needed
for Maven, dependencies and the MySQL image. Linux/macOS need curl or wget and
unzip/tar; Windows uses PowerShell. Run commands from the repository root.

## Start MySQL and the application

Bash (Linux/macOS):

```sh
cp .env.example .env
# Edit .env if you need different local ports/passwords.
docker compose up -d --wait
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

PowerShell (Windows):

```powershell
Copy-Item .env.example .env
# Edit .env if you need different local ports/passwords.
docker compose up -d --wait
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Compose runs **only MySQL**; Java runs on your host. Both bind to loopback in this
setup. The app uses database `procureflow_db`, username `procureflow`, MySQL host port
`3307`, and HTTP `http://localhost:8081`. Different ports allow the three
portfolio demos to run alongside one another. No existing host MySQL is required.

The opt-in `local` Spring profile imports the root `.env` as a properties file.
Compose also reads that file. Use simple unquoted `KEY=value` lines; avoid shell
commands, quotes and backslash escapes. Exported environment variables take
precedence; clear stale overrides if the app connects to an unexpected database.
Without `local`, Spring does not import this file and existing setup instructions
still apply. The example passwords are public, disposable demo values only.

## Build and test

```sh
./mvnw test
./mvnw clean verify
```

On Windows use `.\mvnw.cmd` instead of `./mvnw`. ProcureFlow and Hospital's
full verify phases require Docker for independent Testcontainers databases;
they do not use or erase the Compose database. QueryPilot unit tests mock MySQL
and the AI provider. CI additionally exercises the Compose setup and HTTP smoke
checks in `scripts/local_smoke.py` with a disposable database.

To run a packaged application with the same local profile:

```sh
./mvnw package
java -jar target/procureflow-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## API examples

Import [api.postman_collection.json](api.postman_collection.json) into Postman.
Its `baseUrl` matches this demo's port; change it for the default non-local setup.
See [API walkthrough](api-examples.md) for order, variables and coverage limits.

## Stop, persistence and troubleshooting

Stop Java with Ctrl+C, then run `docker compose down`. The named MySQL volume
retains records. Restart with `docker compose up -d --wait`.

MySQL users/passwords and initialization scripts apply only when the volume is
first created. Editing .env does not change credentials in an existing volume.
Use the original credentials or migrate them deliberately. To erase **only this
demo database**, `docker compose down -v` deletes its named volume permanently;
back up any records you want to keep first.

For startup problems inspect `docker compose ps` and `docker compose logs mysql`.
If ports are occupied, change DB_PORT or SERVER_PORT in .env and update Postman's
baseUrl. A healthy database does not prove the Java application or AI provider is
working. These files provide a local demo, not production deployment settings.
