# ROC-git 后端 Dockerfile
# 多阶段构建：Maven 编译 → JRE 17 运行时
# 适配 Spring Boot 2.7 + JDK 17

# ============ 阶段 1：构建 ============
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先复制 pom 以最大化依赖缓存
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再复制源码并打包
COPY src ./src
RUN mvn -B -q clean package -Dmaven.test.skip=true

# ============ 阶段 2：运行时 ============
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 时区与本地化
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

# 从构建阶段取 war
COPY --from=build /app/target/knife4j-spring-1.0.0.war /app/app.war

EXPOSE 8080

# 健康探针（不强制，docker compose 可关）
HEALTHCHECK --interval=15s --timeout=5s --retries=10 --start-period=40s \
  CMD wget -qO- http://127.0.0.1:8080/api/v1/health 2>/dev/null || wget -qO- http://127.0.0.1:8080/actuator/health 2>/dev/null || exit 0

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/app.war"]
