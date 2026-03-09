# 🔧 RESOLUÇÃO DE PROBLEMAS - PDV Desktop

## Problema 1: "Java não encontrado" ou "java: command not found"

### Causa:
Java não está instalado ou não está no PATH do sistema.

### Solução:

#### Linux:
```bash
# Instalar Java
sudo apt install openjdk-17-jdk   # Ubuntu/Debian
sudo dnf install java-17-openjdk  # Fedora
sudo pacman -S jdk-openjdk        # Arch

# Verificar instalação
java -version
```

#### Windows:
1. Baixe Java de: https://adoptium.net/
2. Instale seguindo o assistente
3. Reinicie o terminal/prompt
4. Teste: `java -version`

---

## Problema 2: "Permission denied: ./gradlew"

### Causa:
Falta permissão de execução no arquivo gradlew.

### Solução:
```bash
chmod +x gradlew run.sh
./run.sh
```

---

## Problema 3: "Could not resolve dependencies"

### Causa:
Problema de conexão com internet ou cache corrompido.

### Solução:
```bash
# Limpar cache
rm -rf ~/.gradle/caches

# Tentar novamente com refresh
./gradlew clean build --refresh-dependencies
```

---

## Problema 4: Tela preta ou não abre

### Causa:
Problema com bibliotecas gráficas ou driver de vídeo.

### Solução:

#### Linux:
```bash
# Instalar bibliotecas necessárias
sudo apt install libgl1-mesa-glx libxi6 libxrender1 libxtst6

# Ou para sistemas baseados em Wayland
export GDK_BACKEND=x11
./run.sh
```

---

## Problema 5: "Unable to find a valid JDK"

### Causa:
Versão do Java incompatível ou JAVA_HOME não configurado.

### Solução:
```bash
# Verificar versão (precisa ser 17+)
java -version

# Se versão < 17, atualizar
sudo apt install openjdk-17-jdk

# Configurar JAVA_HOME (adicione ao ~/.bashrc ou ~/.zshrc)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Recarregar configuração
source ~/.bashrc
```

---

## Problema 6: Erro "Address already in use"

### Causa:
Outra instância do programa já está rodando.

### Solução:
```bash
# Encontrar processo
ps aux | grep pdv

# Matar processo (substitua PID pelo número encontrado)
kill -9 PID

# Ou matar todos
pkill -f "pdv-compose"
```

---

## Problema 7: "OutOfMemoryError"

### Causa:
Pouca memória RAM disponível.

### Solução:
```bash
# Aumentar memória para o Gradle
export GRADLE_OPTS="-Xmx2048m"
./gradlew run
```

Ou edite o arquivo `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

---

## Problema 8: Banco de dados corrompido

### Sintomas:
- Erros ao salvar/ler dados
- Produtos não aparecem
- Vendas não são registradas

### Solução:
```bash
# Fazer backup do banco atual
cp pdv.db pdv.db.backup

# Deletar banco corrompido
rm pdv.db

# Executar novamente (criará banco novo)
./run.sh
```

---

## Problema 9: Dependências não baixam

### Causa:
Firewall ou proxy bloqueando conexões.

### Solução:

#### Com proxy:
```bash
# Configurar proxy no gradle.properties
echo "systemProp.http.proxyHost=seu.proxy.com" >> gradle.properties
echo "systemProp.http.proxyPort=8080" >> gradle.properties
echo "systemProp.https.proxyHost=seu.proxy.com" >> gradle.properties
echo "systemProp.https.proxyPort=8080" >> gradle.properties
```

#### Sem proxy (verificar firewall):
```bash
# Testar conectividade
curl -I https://services.gradle.org

# Se falhar, verificar firewall
sudo ufw status
```

---

## Problema 10: Compilação muito lenta

### Causa:
Cache não configurado ou pouca memória.

### Solução:
```bash
# Habilitar daemon do Gradle
echo "org.gradle.daemon=true" >> gradle.properties
echo "org.gradle.parallel=true" >> gradle.properties
echo "org.gradle.caching=true" >> gradle.properties

# Limpar e recompilar
./gradlew clean build --build-cache
```

---

## Problema 11: Erro "Unsupported class file major version"

### Causa:
Versão do Java incompatível com o código compilado.

### Solução:
```bash
# Verificar versão do Java
java -version

# Deve ser 17 ou superior
# Se não for, atualize seguindo Problema 1
```

---

## Problema 12: Interface não responde

### Causa:
Possível deadlock ou operação bloqueante na UI.

### Solução:
```bash
# Forçar encerramento
pkill -9 -f pdv-compose

# Executar novamente
./run.sh
```

---

## Problema 13: Caracteres estranhos na interface

### Causa:
Problema de encoding ou fonte não suportada.

### Solução:
```bash
# Definir locale UTF-8
export LC_ALL=pt_BR.UTF-8
export LANG=pt_BR.UTF-8

# Executar
./run.sh
```

---

## 🆘 Ainda com problemas?

### Logs de Debug:

```bash
# Executar com logs detalhados
./gradlew run --debug > debug.log 2>&1

# Verificar o arquivo debug.log
cat debug.log
```

### Informações úteis para suporte:

```bash
# Sistema operacional
uname -a

# Versão do Java
java -version

# Versão do Gradle
./gradlew --version

# Variáveis de ambiente
env | grep -i java
```

---

## ✅ Checklist de Verificação

Antes de reportar um problema, verifique:

- [ ] Java 17+ está instalado (`java -version`)
- [ ] Permissão de execução está configurada (`chmod +x gradlew`)
- [ ] Conexão com internet está funcionando
- [ ] Espaço em disco suficiente (mínimo 500MB livres)
- [ ] Nenhuma outra instância do programa rodando
- [ ] Tentou limpar o cache (`./gradlew clean`)
- [ ] Tentou atualizar dependências (`--refresh-dependencies`)

---

**Última atualização:** 2026-03-09  
**Versão:** 1.0.0

