# ---------- Étape 1 : Build du projet avec Maven ----------
FROM eclipse-temurin:21-jdk-alpine AS build

# Installer les outils nécessaires pour Maven wrapper
RUN apk add --no-cache bash curl

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers Maven wrapper et config
COPY mvnw .
COPY .mvn .mvn

# Copier le fichier pom.xml et les sources
COPY pom.xml .
COPY src ./src

# Rendre le wrapper Maven exécutable
RUN chmod +x mvnw

# Build du projet (sans les tests pour accélérer)
RUN ./mvnw clean package -DskipTests

# Debug : afficher le contenu du répertoire target
RUN echo "[DEBUG] Contenu de /app/target :" && ls -lh /app/target


# ---------- Étape 2 : Image finale pour l'exécution ----------
FROM eclipse-temurin:21-jdk-alpine

# Installer bash et netcat (nc) pour le script wait-for
RUN apk add --no-cache bash netcat-openbsd

# Définir le répertoire de travail
WORKDIR /app

# Copier le .jar depuis l’étape de build
COPY --from=build /app/target/xpertcash-0.0.1-SNAPSHOT.jar /app/xpertcash-0.0.1-SNAPSHOT.jar

# Copier le script wait-for.sh (nouvelle version)
COPY wait-for-it.sh /app/wait-for.sh
RUN chmod +x /app/wait-for.sh

# Exposer le port de l’application Spring Boot
EXPOSE 8080

# Commande de démarrage : attendre MySQL avant de lancer l'app
ENTRYPOINT ["/app/wait-for.sh", "mysql-db:3306", "-t", "60", "--", "java", "-jar", "/app/xpertcash-0.0.1-SNAPSHOT.jar"]
