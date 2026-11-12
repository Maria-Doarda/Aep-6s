# Imagem de execucao do AEP6S (API REST + tela web).
# Build local:  docker build -t aep6s .
# Execucao:     docker run -p 8081:8081 -e MONGO_URI="mongodb://host.docker.internal:27017/poc_doacoes" -e SPRING_PROFILES_ACTIVE=prod,demo aep6s

# ---------- estagio 1: compilacao ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copia so o pom primeiro para aproveitar o cache de dependencias entre builds
COPY aep/aep/pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY aep/aep/src ./src
RUN mvn -B -q -Dmaven.test.skip=true clean package

# ---------- estagio 2: execucao ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/aep-0.0.1-SNAPSHOT.jar app.jar

# Render free tem 512 MB: limita a heap a 70% da memoria do container e usa um GC leve
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
