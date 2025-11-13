# =========================================================================
# ETAPA 1: BUILD (Compilación del código Java)
# =========================================================================
# Utilizamos la imagen oficial de Maven con el JDK 17, que es ideal para compilar.
FROM maven:3.9.5-eclipse-temurin-17 AS builder

# 1. Directorio de Trabajo: Todo el trabajo se hará dentro de este directorio
WORKDIR /app

# 2. Copiar archivos de configuración: Copia el pom.xml primero para la caché de capas.
COPY pom.xml .

# 3. Descarga de Dependencias: Descarga las dependencias del pom.xml.
# Si el pom.xml NO cambia, esta capa se mantiene en caché, acelerando builds futuros.
RUN mvn dependency:go-offline -B

# 4. Copiar el Código Fuente: Copia el código fuente (la carpeta src)
COPY src ./src

# 5. Empaquetado: Compila el código, ejecuta tests (desactivados), y genera el JAR
# El resultado es un JAR en /app/target/
RUN mvn clean package -DskipTests

# =========================================================================
# ETAPA 2: RUNTIME (Ambiente de Ejecución Final) - IMAGEN MINIMALISTA
# =========================================================================
# Usamos una imagen que solo tiene el JRE (Runtime Environment) para correr Java,
# no el JDK completo (lo que reduce drásticamente el tamaño final de la imagen).
FROM eclipse-temurin:17-jre-focal

# 6. Variables de Entorno y Usuario (Seguridad)
# Define la zona horaria del contenedor
ENV TZ=America/Bogota
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Crea un usuario no-root para correr la aplicación (Buena práctica de seguridad)
RUN groupadd -r springboot && useradd -r -g springboot springboot
USER springboot

# 7. Copia del Artifact
# Se copia el JAR generado en la etapa 'builder' a esta imagen de runtime.
# Debes asegurar que el nombre del JAR es correcto. Si tu artifactId es CrudCloud
# y la versión es 0.0.1-SNAPSHOT, este nombre es correcto.
ARG JAR_FILE=target/CrudCloud-0.0.1-SNAPSHOT.jar
COPY --from=builder /app/${JAR_FILE} /app/CrudCloud.jar

# 8. Puerto de Exposición (Documentación)
# Indica el puerto que usa la aplicación, pero NO lo expone a la VPS (solo al Compose Network)
EXPOSE 8080

# 9. Comando de Ejecución (Entrypoint)
# Este es el comando que se ejecuta cuando el contenedor se inicia.
# NOTA: Utilizamos variables de entorno de Spring Boot aquí.
ENTRYPOINT ["java", \
            "-jar", \
            "/app/CrudCloud.jar"]