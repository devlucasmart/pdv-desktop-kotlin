#!/bin/bash

# Script de execução do PDV Desktop
# Autor: Sistema PDV
# Data: 2026-03-09

echo "=========================================="
echo "   PDV Desktop - Sistema de Vendas"
echo "=========================================="
echo ""

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar se Java está instalado
echo "🔍 Verificando instalação do Java..."
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java não encontrado!${NC}"
    echo ""
    echo "Por favor, instale o Java JDK 17 ou superior:"
    echo ""
    echo "Ubuntu/Debian:"
    echo "  sudo apt update"
    echo "  sudo apt install openjdk-17-jdk"
    echo ""
    echo "Fedora:"
    echo "  sudo dnf install java-17-openjdk"
    echo ""
    echo "Arch Linux:"
    echo "  sudo pacman -S jdk-openjdk"
    echo ""
    exit 1
fi

# Verificar versão do Java
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
echo -e "${GREEN}✅ Java encontrado: versão $JAVA_VERSION${NC}"

if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${YELLOW}⚠️  Aviso: Java 17 ou superior é recomendado${NC}"
    echo -e "${YELLOW}   Sua versão: $JAVA_VERSION${NC}"
    echo ""
fi

# Dar permissão ao gradlew se necessário
if [ ! -x "./gradlew" ]; then
    echo "🔧 Configurando permissões..."
    chmod +x gradlew
fi

echo ""
echo "🚀 Iniciando PDV Desktop..."
echo ""
echo "⏳ Na primeira execução, pode demorar alguns minutos"
echo "   para baixar as dependências..."
echo ""
echo "----------------------------------------"
echo ""

# Executar o projeto
./gradlew run

# Capturar código de saída
EXIT_CODE=$?

echo ""
echo "----------------------------------------"
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ PDV Desktop encerrado com sucesso${NC}"
else
    echo -e "${RED}❌ Erro ao executar o PDV Desktop${NC}"
    echo ""
    echo "Tente executar:"
    echo "  ./gradlew clean build"
    echo ""
    echo "Ou consulte o arquivo INSTALL.md para mais informações"
fi
echo "=========================================="

exit $EXIT_CODE

