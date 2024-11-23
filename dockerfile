# Usa una imagen base de Java
FROM openjdk:21-jdk-slim

# Crea un directorio para la aplicación
WORKDIR /app

# Copia el archivo JAR en el contenedor
COPY build/libs/umbrella-1.0.jar app.jar

# Define el comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
