# 📌 COMANDOS ÚTEIS - PDV Desktop

## 🚀 Execução

```bash
# Método mais simples
./run.sh                    # Linux/macOS
run.bat                     # Windows

# Método direto com Gradle
./gradlew run               # Linux/macOS
gradlew.bat run            # Windows
```

---

## 🔨 Build e Compilação

```bash
# Compilar o projeto
./gradlew build

# Compilar sem testes
./gradlew build -x test

# Compilar modo release
./gradlew build --release

# Compilar com logs detalhados
./gradlew build --info

# Compilar com debug
./gradlew build --debug
```

---

## 🧹 Limpeza

```bash
# Limpar arquivos de build
./gradlew clean

# Limpar cache de build
./gradlew cleanBuildCache

# Limpar tudo e recompilar
./gradlew clean build

# Limpar cache do Gradle (mais profundo)
rm -rf ~/.gradle/caches
```

---

## 📦 Geração de Executáveis

```bash
# Gerar pacote .deb (Linux)
./gradlew packageDeb

# Gerar pacote .msi (Windows)
./gradlew packageMsi

# Gerar pacote .dmg (macOS)
./gradlew packageDmg

# Ver onde o pacote foi gerado
ls -la build/compose/binaries/main/
```

---

## 🔄 Atualização de Dependências

```bash
# Atualizar todas as dependências
./gradlew --refresh-dependencies

# Verificar dependências desatualizadas
./gradlew dependencyUpdates

# Baixar dependências sem compilar
./gradlew dependencies
```

---

## 🐛 Debug e Testes

```bash
# Executar com logs de debug
./gradlew run --debug

# Executar com stack trace completo
./gradlew run --stacktrace

# Executar testes
./gradlew test

# Executar testes com relatório
./gradlew test --info

# Ver relatório de testes no navegador
xdg-open build/reports/tests/test/index.html
```

---

## 📊 Informações do Projeto

```bash
# Ver versão do Gradle
./gradlew --version

# Listar todas as tarefas disponíveis
./gradlew tasks

# Listar tarefas com detalhes
./gradlew tasks --all

# Ver informações do projeto
./gradlew properties

# Ver árvore de dependências
./gradlew dependencies
```

---

## 🔍 Análise de Código

```bash
# Verificar qualidade do código
./gradlew check

# Gerar relatório de análise
./gradlew detekt

# Verificar estilo de código
./gradlew ktlintCheck

# Corrigir estilo automaticamente
./gradlew ktlintFormat
```

---

## 🗄️ Banco de Dados

```bash
# Ver banco de dados SQLite
sqlite3 pdv.db "SELECT * FROM product;"

# Backup do banco
cp pdv.db pdv.db.backup

# Restaurar backup
cp pdv.db.backup pdv.db

# Resetar banco (deletar e recriar)
rm pdv.db && ./gradlew run

# Exportar dados para SQL
sqlite3 pdv.db .dump > backup.sql

# Importar dados de SQL
sqlite3 pdv.db < backup.sql
```

---

## 🔧 Configuração

```bash
# Editar configurações do Gradle
nano gradle.properties

# Ver configurações atuais
cat gradle.properties

# Aumentar memória do Gradle
echo "org.gradle.jvmargs=-Xmx2048m" >> gradle.properties

# Habilitar cache
echo "org.gradle.caching=true" >> gradle.properties

# Habilitar compilação paralela
echo "org.gradle.parallel=true" >> gradle.properties
```

---

## 📝 Logs

```bash
# Salvar logs de execução
./gradlew run > logs/app.log 2>&1

# Ver logs em tempo real
tail -f logs/app.log

# Filtrar logs de erro
./gradlew run 2>&1 | grep -i error

# Logs com timestamp
./gradlew run 2>&1 | ts

# Salvar logs de build
./gradlew build --info > build.log 2>&1
```

---

## 🎯 Desenvolvimento

```bash
# Modo contínuo (recompila ao salvar)
./gradlew run --continuous

# Executar em modo offline (sem internet)
./gradlew run --offline

# Forçar atualização de snapshots
./gradlew run --refresh-dependencies

# Executar com profile
./gradlew run --profile
```

---

## 🔒 Permissões

```bash
# Dar permissão aos scripts
chmod +x gradlew run.sh

# Verificar permissões
ls -la | grep -E "gradlew|run.sh"

# Recursivamente para todos
find . -name "*.sh" -exec chmod +x {} \;
```

---

## 📊 Performance

```bash
# Executar com scan de build
./gradlew run --scan

# Analisar tempo de build
./gradlew build --profile

# Ver relatório de performance
xdg-open build/reports/profile/profile-*.html

# Benchmark do build
time ./gradlew clean build
```

---

## 🌐 Rede e Conectividade

```bash
# Testar conexão com repositórios
curl -I https://repo.maven.apache.org/maven2/

# Testar Gradle
curl -I https://services.gradle.org

# Executar com proxy
./gradlew run -Dhttp.proxyHost=proxy.com -Dhttp.proxyPort=8080

# Desabilitar verificações SSL (não recomendado)
./gradlew run -Djavax.net.ssl.trustAll=true
```

---

## 🆘 Recuperação de Emergência

```bash
# Reset completo do projeto
./gradlew clean
rm -rf .gradle build
rm -rf ~/.gradle/caches
./gradlew build --refresh-dependencies

# Matar processos travados
pkill -9 -f pdv-compose
pkill -9 -f gradle

# Verificar processos rodando
ps aux | grep -E "pdv|gradle"

# Liberar porta (se necessário)
fuser -k 8080/tcp
```

---

## 📚 Documentação

```bash
# Gerar documentação do código
./gradlew dokkaHtml

# Abrir documentação
xdg-open build/dokka/html/index.html

# Gerar Javadoc
./gradlew javadoc
```

---

## 🎨 Interface

```bash
# Executar em modo maximizado
./gradlew run -Dcompose.desktop.fullscreen=true

# Executar com DPI específico
./gradlew run -Dsun.java2d.dpiaware=true

# Forçar backend gráfico (Linux)
GDK_BACKEND=x11 ./gradlew run
```

---

## 💾 Instalação

```bash
# Instalar localmente (após gerar .deb)
sudo dpkg -i build/compose/binaries/main/deb/*.deb

# Desinstalar
sudo apt remove pdv-desktop

# Ver informações do pacote
dpkg -l | grep pdv
```

---

## ⚡ Atalhos Combinados

```bash
# Limpar, compilar e executar
./gradlew clean build run

# Build completo com refresh
./gradlew clean build --refresh-dependencies --stacktrace

# Deploy completo
./gradlew clean build packageDeb

# Debug completo
./gradlew clean run --debug --stacktrace > debug.log 2>&1
```

---

**Dica:** Adicione alias no seu `~/.bashrc` ou `~/.zshrc`:

```bash
# Atalhos úteis
alias pdv-run='cd /caminho/para/pdv-compose && ./run.sh'
alias pdv-build='cd /caminho/para/pdv-compose && ./gradlew clean build'
alias pdv-clean='cd /caminho/para/pdv-compose && ./gradlew clean'
```

---

**Última atualização:** 2026-03-09  
**Versão:** 1.0.0

