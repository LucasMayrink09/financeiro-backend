# 1. Usa uma imagem do Java 21
FROM eclipse-temurin:21-jdk

# 2. Define a pasta de trabalho dentro do container
WORKDIR /app

# 3. Copia TODO o seu projeto para dentro do container
COPY . .

# 4. (IMPORTANTE) Dá permissão de execução para o mvnw
# Isso é essencial porque no Windows essa permissão não existe nativamente
RUN chmod +x mvnw

# 5. Roda o build (baixa as dependências e cria o jar)
RUN ./mvnw clean package -DskipTests

# 6. Expõe a porta (O Render vai ignorar isso e injetar a porta dele, 
# mas é boa prática manter)
EXPOSE 8080

# 7. Roda a aplicação pegando o nome exato que vimos no seu pom.xml
CMD ["java", "-jar", "target/financeira-0.0.1-SNAPSHOT.jar"]