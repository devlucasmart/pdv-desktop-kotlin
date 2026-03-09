#!/bin/bash

# Script para gerar instaladores do PDV Desktop
# Autor: PDV Systems
# Data: 2026-03-09

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                                                                  ║"
echo "║     🚀 GERADOR DE INSTALADORES - PDV DESKTOP 🚀                 ║"
echo "║                                                                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Verificar Java
echo -e "${BLUE}[1/5] Verificando Java...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java não encontrado!${NC}"
    echo "Instale Java 17+ primeiro."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${YELLOW}⚠️  Java $JAVA_VERSION encontrado. Recomendado: Java 17+${NC}"
fi
echo -e "${GREEN}✓ Java $JAVA_VERSION OK${NC}"
echo ""

# Limpar builds anteriores
echo -e "${BLUE}[2/5] Limpando builds anteriores...${NC}"
./gradlew clean --no-daemon
rm -rf build/compose/binaries
echo -e "${GREEN}✓ Limpeza concluída${NC}"
echo ""

# Compilar projeto
echo -e "${BLUE}[3/5] Compilando projeto...${NC}"
./gradlew build --no-daemon
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Erro na compilação!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Compilação bem-sucedida${NC}"
echo ""

# Menu de seleção
echo -e "${BLUE}[4/5] Selecione o tipo de instalador:${NC}"
echo ""
echo "  1) 📦 .deb (Ubuntu/Debian/Mint)"
echo "  2) 📦 .rpm (Fedora/RedHat/CentOS)"
echo "  3) 🍎 .dmg (macOS)"
echo "  4) 🪟 .msi (Windows Installer)"
echo "  5) 🪟 .exe (Windows Executable)"
echo "  6) 🌍 TODOS os formatos"
echo "  7) ❌ Cancelar"
echo ""
read -p "Digite sua escolha [1-7]: " choice

case $choice in
    1)
        echo -e "\n${BLUE}Gerando instalador .deb...${NC}"
        ./gradlew packageDeb --no-daemon
        INSTALLER_TYPE="DEB"
        ;;
    2)
        echo -e "\n${BLUE}Gerando instalador .rpm...${NC}"
        ./gradlew packageRpm --no-daemon
        INSTALLER_TYPE="RPM"
        ;;
    3)
        echo -e "\n${BLUE}Gerando instalador .dmg...${NC}"
        ./gradlew packageDmg --no-daemon
        INSTALLER_TYPE="DMG"
        ;;
    4)
        echo -e "\n${BLUE}Gerando instalador .msi...${NC}"
        ./gradlew packageMsi --no-daemon
        INSTALLER_TYPE="MSI"
        ;;
    5)
        echo -e "\n${BLUE}Gerando executável .exe...${NC}"
        ./gradlew packageExe --no-daemon
        INSTALLER_TYPE="EXE"
        ;;
    6)
        echo -e "\n${BLUE}Gerando TODOS os instaladores...${NC}"
        echo "Isso pode demorar vários minutos..."
        ./gradlew packageDeb packageRpm packageDmg packageMsi packageExe --no-daemon
        INSTALLER_TYPE="TODOS"
        ;;
    7)
        echo -e "${YELLOW}Operação cancelada.${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}Opção inválida!${NC}"
        exit 1
        ;;
esac

BUILD_STATUS=$?

echo ""
echo "════════════════════════════════════════════════════════════"

if [ $BUILD_STATUS -eq 0 ]; then
    echo -e "${GREEN}✅ INSTALADOR(ES) GERADO(S) COM SUCESSO!${NC}"
    echo ""
    echo -e "${BLUE}[5/5] Localizando arquivos...${NC}"
    echo ""

    # Listar arquivos gerados
    echo "📦 Instaladores disponíveis em:"
    echo ""

    if [ -d "build/compose/binaries/main/deb" ]; then
        echo -e "${GREEN}► DEB (Linux Debian/Ubuntu):${NC}"
        find build/compose/binaries/main/deb -name "*.deb" -exec ls -lh {} \; | awk '{print "  " $9 " (" $5 ")"}'
        echo ""
    fi

    if [ -d "build/compose/binaries/main/rpm" ]; then
        echo -e "${GREEN}► RPM (Linux Fedora/RedHat):${NC}"
        find build/compose/binaries/main/rpm -name "*.rpm" -exec ls -lh {} \; | awk '{print "  " $9 " (" $5 ")"}'
        echo ""
    fi

    if [ -d "build/compose/binaries/main/dmg" ]; then
        echo -e "${GREEN}► DMG (macOS):${NC}"
        find build/compose/binaries/main/dmg -name "*.dmg" -exec ls -lh {} \; | awk '{print "  " $9 " (" $5 ")"}'
        echo ""
    fi

    if [ -d "build/compose/binaries/main/msi" ]; then
        echo -e "${GREEN}► MSI (Windows):${NC}"
        find build/compose/binaries/main/msi -name "*.msi" -exec ls -lh {} \; | awk '{print "  " $9 " (" $5 ")"}'
        echo ""
    fi

    if [ -d "build/compose/binaries/main/exe" ]; then
        echo -e "${GREEN}► EXE (Windows):${NC}"
        find build/compose/binaries/main/exe -name "*.exe" -exec ls -lh {} \; | awk '{print "  " $9 " (" $5 ")"}'
        echo ""
    fi

    echo "────────────────────────────────────────────────────────────"
    echo ""
    echo "💡 Dicas de Instalação:"
    echo ""
    echo "  Linux (DEB):  sudo dpkg -i arquivo.deb"
    echo "  Linux (RPM):  sudo rpm -i arquivo.rpm"
    echo "  macOS:        Abra o .dmg e arraste para Applications"
    echo "  Windows:      Clique duplo no .msi ou .exe"
    echo ""
    echo "────────────────────────────────────────────────────────────"
    echo ""
    echo "📝 Informações do Pacote:"
    echo "  Nome: PDV Desktop"
    echo "  Versão: 1.0.0"
    echo "  Tamanho: ~50-100 MB (varia por plataforma)"
    echo "  Requisitos: Java Runtime incluído"
    echo ""

else
    echo -e "${RED}❌ ERRO AO GERAR INSTALADOR${NC}"
    echo ""
    echo "Possíveis causas:"
    echo "  - Falta de dependências do sistema"
    echo "  - Problemas de permissão"
    echo "  - Sistema operacional não suportado"
    echo ""
    echo "Para mais informações, execute com --stacktrace:"
    echo "  ./gradlew package$INSTALLER_TYPE --stacktrace"
    echo ""
fi

echo "════════════════════════════════════════════════════════════"

exit $BUILD_STATUS

