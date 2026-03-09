#!/bin/bash

# Script de Resolução Completa - PDV Desktop
# Este script resolve todos os problemas de compilação

echo "╔════════════════════════════════════════════════════════════╗"
echo "║  SCRIPT DE RESOLUÇÃO COMPLETA - PDV Desktop               ║"
echo "╔════════════════════════════════════════════════════════════╗"
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Passo 1: Parando todos os daemons do Gradle${NC}"
./gradlew --stop 2>/dev/null || true
pkill -f "GradleDaemon" 2>/dev/null || true
pkill -f "KotlinCompileDaemon" 2>/dev/null || true
echo -e "${GREEN}✓ Daemons parados${NC}"
echo ""

echo -e "${BLUE}Passo 2: Limpando caches locais corrompidos${NC}"
rm -rf build
rm -rf .gradle
echo -e "${GREEN}✓ Caches locais removidos${NC}"
echo ""

echo -e "${BLUE}Passo 3: Limpando caches globais do Gradle${NC}"
rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/daemon/
rm -rf ~/.kotlin/
echo -e "${GREEN}✓ Caches globais removidos${NC}"
echo ""

echo -e "${BLUE}Passo 4: Verificando arquivos do Gradle Wrapper${NC}"
if [ ! -f "gradlew" ] || [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo -e "${YELLOW}⚠ Arquivos do wrapper ausentes ou corrompidos${NC}"
    echo "Recriando Gradle Wrapper..."

    mkdir -p gradle/wrapper

    # Download gradle-wrapper.jar
    curl -L -o gradle/wrapper/gradle-wrapper.jar \
        https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar

    # Download gradlew
    curl -L -o gradlew \
        https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradlew
    chmod +x gradlew

    # Download gradlew.bat
    curl -L -o gradlew.bat \
        https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradlew.bat

    echo -e "${GREEN}✓ Gradle Wrapper recriado${NC}"
else
    echo -e "${GREEN}✓ Gradle Wrapper OK${NC}"
fi
echo ""

echo -e "${BLUE}Passo 5: Compilando o projeto (pode demorar alguns minutos)${NC}"
echo "Por favor, aguarde..."
echo ""

./gradlew clean build --no-daemon --refresh-dependencies

BUILD_STATUS=$?

echo ""
echo "════════════════════════════════════════════════════════════"

if [ $BUILD_STATUS -eq 0 ]; then
    echo -e "${GREEN}✅ COMPILAÇÃO BEM-SUCEDIDA!${NC}"
    echo ""
    echo "O projeto foi compilado com sucesso!"
    echo ""
    echo "Para executar o PDV Desktop:"
    echo -e "  ${BLUE}./gradlew run${NC}"
    echo ""
    echo "Ou use o script:"
    echo -e "  ${BLUE}./run.sh${NC}"
    echo ""
else
    echo -e "${RED}❌ ERRO NA COMPILAÇÃO${NC}"
    echo ""
    echo "O build falhou. Possíveis soluções:"
    echo ""
    echo "1. Verifique se o Java 17+ está instalado:"
    echo "   java -version"
    echo ""
    echo "2. Verifique a conexão com internet"
    echo ""
    echo "3. Tente novamente:"
    echo "   ./gradlew clean build --refresh-dependencies"
    echo ""
    echo "4. Se persistir, veja os logs acima para mais detalhes"
    echo ""
fi

echo "════════════════════════════════════════════════════════════"

exit $BUILD_STATUS

