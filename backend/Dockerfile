# HYEDA backend — multi-stage image for Render Free / Docker
# Context: HYEDA/backend

FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package \
 && cp target/hyeda-*.jar /app/app.jar

FROM eclipse-temurin:26-jre-noble
WORKDIR /app

RUN groupadd -r hyeda && useradd -r -g hyeda hyeda \
 && mkdir -p /app/storage /app/data \
 && chown -R hyeda:hyeda /app

COPY --from=build /app/app.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=postgres \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport" \
    HYEDA_STORAGE=/app/storage

EXPOSE 8081
USER hyeda

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
