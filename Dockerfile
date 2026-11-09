# ビルドステージ
FROM gradle:8.10-jdk25 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle bootJar --no-daemon
# JavaFX依存関係をコピー（Docker内はLinux環境のためlinux.jarを使用）
# macOSでローカル実行する場合はbuild.gradle.ktsのbootRunタスクが自動的にプラットフォームに応じたJavaFXを使用
RUN mkdir -p /app/javafx && cp $(find ~/.gradle/caches -name "javafx-*-linux.jar" | tr '\n' ' ') /app/javafx/ 2>/dev/null || true

# 実行ステージ
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/javafx/ ./javafx/
EXPOSE 8080

# JavaFX用のmodule-path設定
ENV JAVA_OPTS="--module-path ./javafx --add-modules javafx.controls,javafx.fxml"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
