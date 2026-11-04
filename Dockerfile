# Image générique : le même Dockerfile sert pour les 7 modules.
# Le jar est produit par Maven, l'image ne fait que l'embarquer.
FROM eclipse-temurin:21-jre

RUN useradd --system --uid 1001 spring
WORKDIR /app

ARG JAR_FILE
COPY ${JAR_FILE} app.jar

USER spring
ENTRYPOINT ["java", "-jar", "app.jar"]