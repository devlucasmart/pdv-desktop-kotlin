# 🚀 Guia de Instalação e Execução - PDV Desktop

## 📋 Pré-requisitos

### 1. Instalar Java JDK 17 ou superior

#### Ubuntu/Debian:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

#### Fedora:
```bash
sudo dnf install java-17-openjdk
```

#### Arch Linux:
```bash
sudo pacman -S jdk-openjdk
```

#### Verificar instalação:
```bash
java -version
```
Deve exibir algo como: `openjdk version "17.x.x"` ou superior.

## ▶️ Executar o Projeto

### Método 1: Execução Direta (Mais Rápido)

```bash
# Entre no diretório do projeto
cd /home/lucas_marruda/Documentos/pessoal/gitPessoal/PDV/desktop/pdv-compose

# Dê permissão de execução ao gradlew
chmod +x gradlew

# Execute o projeto
./gradlew run
```

### Método 2: Compilar e Executar

```bash
# Compilar o projeto
./gradlew build

# Executar
./gradlew run
```

### Método 3: Criar Executável Nativo

```bash
# Para Linux (gera arquivo .deb)
./gradlew packageDeb

# Para instalar o .deb gerado:
sudo dpkg -i build/compose/binaries/main/deb/*.deb
```

## 🐛 Resolução de Problemas

### Problema 1: "bash: ./gradlew: Permission denied"
**Solução:**
```bash
chmod +x gradlew
```

### Problema 2: "Java não encontrado" ou "JAVA_HOME not set"
**Solução:**
```bash
# Verificar se Java está instalado
java -version

# Se não estiver, instale conforme instruções acima

# Configurar JAVA_HOME (adicione ao ~/.bashrc ou ~/.zshrc)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### Problema 3: Erro de download do Gradle
**Solução:**
```bash
# Limpar cache e tentar novamente
rm -rf ~/.gradle/wrapper/dists/gradle-8.4-bin
./gradlew --refresh-dependencies
```

### Problema 4: Porta já em uso ou erro de compilação
**Solução:**
```bash
# Limpar build anterior
./gradlew clean

# Recompilar
./gradlew build --refresh-dependencies
```

## 📦 Primeira Execução

Na primeira vez que executar:
1. O Gradle será baixado automaticamente (pode demorar alguns minutos)
2. As dependências serão baixadas
3. O banco de dados SQLite será criado automaticamente
4. 10 produtos de exemplo serão inseridos

## 🎯 Teste Rápido

Após iniciar o sistema:
1. Vá para a tela "Vendas"
2. Digite o SKU: `001` e clique em "Adicionar"
3. O produto "Coca-Cola 2L" será adicionado ao carrinho
4. Clique em "Finalizar Venda"
5. Selecione a forma de pagamento
6. Venda concluída!

## 📊 Dados de Exemplo

O sistema inclui 10 produtos de exemplo:
- SKU 001: Coca-Cola 2L - R$ 8,50
- SKU 002: Pão Francês (kg) - R$ 12,00
- SKU 003: Arroz Tipo 1 5kg - R$ 25,90
- SKU 004: Feijão Preto 1kg - R$ 7,80
- SKU 005: Café Torrado 500g - R$ 15,50
- E mais...

## 💡 Dicas

- Use a tecla `Tab` para navegar entre campos rapidamente
- O campo de SKU aceita Enter para adicionar produtos
- O banco de dados fica salvo em `pdv.db` no diretório do projeto
- Para resetar os dados, basta deletar o arquivo `pdv.db`

## 🔄 Atualizar Dependências

```bash
./gradlew --refresh-dependencies
```

## 🧹 Limpar Cache de Build

```bash
./gradlew clean cleanBuildCache
```

## ⚡ Atalhos Úteis

```bash
# Executar
./gradlew run

# Compilar
./gradlew build

# Limpar
./gradlew clean

# Ver tarefas disponíveis
./gradlew tasks

# Gerar documentação
./gradlew dokkaHtml
```

## 📱 Contato

Em caso de problemas não resolvidos por este guia, verifique:
1. Se o Java 17+ está instalado corretamente
2. Se todas as dependências foram baixadas
3. Se há espaço suficiente em disco (pelo menos 500MB livres)
4. Se não há firewall bloqueando o download de dependências

---

**Versão:** 1.0.0  
**Última atualização:** 2026-03-09

