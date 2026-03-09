#!/bin/bash

# Script de verificação do ambiente PDV Desktop
# Verifica se tudo está pronto para executar o sistema

echo ""
echo "════════════════════════════════════════════════════════"
echo "  VERIFICAÇÃO DO AMBIENTE - PDV Desktop"
echo "════════════════════════════════════════════════════════"
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Contadores
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNING_CHECKS=0

# Função para verificação
check() {
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -n "[$TOTAL_CHECKS] $1... "
}

pass() {
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    echo -e "${GREEN}✓ OK${NC}"
    if [ ! -z "$1" ]; then
        echo "    └─ $1"
    fi
}

fail() {
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
    echo -e "${RED}✗ FALHOU${NC}"
    if [ ! -z "$1" ]; then
        echo "    └─ $1"
    fi
}

warn() {
    WARNING_CHECKS=$((WARNING_CHECKS + 1))
    echo -e "${YELLOW}⚠ AVISO${NC}"
    if [ ! -z "$1" ]; then
        echo "    └─ $1"
    fi
}

echo "Verificando pré-requisitos..."
echo ""

# 1. Verificar Java
check "Java instalado"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        pass "Java $JAVA_VERSION encontrado"
    else
        warn "Java $JAVA_VERSION encontrado (recomendado: 17+)"
    fi
else
    fail "Java não encontrado. Instale: sudo apt install openjdk-17-jdk"
fi

# 2. Verificar gradlew
check "Script Gradle (gradlew)"
if [ -f "./gradlew" ]; then
    if [ -x "./gradlew" ]; then
        pass "Arquivo existe e tem permissão de execução"
    else
        warn "Arquivo existe mas sem permissão. Execute: chmod +x gradlew"
    fi
else
    fail "Arquivo gradlew não encontrado no diretório atual"
fi

# 3. Verificar run.sh
check "Script de execução (run.sh)"
if [ -f "./run.sh" ]; then
    if [ -x "./run.sh" ]; then
        pass "Arquivo existe e tem permissão de execução"
    else
        warn "Arquivo existe mas sem permissão. Execute: chmod +x run.sh"
    fi
else
    fail "Arquivo run.sh não encontrado"
fi

# 4. Verificar build.gradle.kts
check "Arquivo de configuração (build.gradle.kts)"
if [ -f "./build.gradle.kts" ]; then
    pass "Arquivo de build encontrado"
else
    fail "Arquivo build.gradle.kts não encontrado"
fi

# 5. Verificar código fonte
check "Código fonte (src/main/kotlin)"
if [ -d "./src/main/kotlin" ]; then
    FILE_COUNT=$(find ./src/main/kotlin -name "*.kt" | wc -l)
    pass "$FILE_COUNT arquivos Kotlin encontrados"
else
    fail "Diretório de código fonte não encontrado"
fi

# 6. Verificar conexão com internet
check "Conexão com internet"
if ping -c 1 8.8.8.8 &> /dev/null; then
    pass "Conectado à internet"
else
    warn "Sem conexão com internet (necessário para primeira execução)"
fi

# 7. Verificar espaço em disco
check "Espaço em disco"
AVAILABLE_SPACE=$(df -BM . | tail -1 | awk '{print $4}' | sed 's/M//')
if [ "$AVAILABLE_SPACE" -gt 500 ]; then
    pass "${AVAILABLE_SPACE}MB disponíveis"
else
    warn "Pouco espaço em disco: ${AVAILABLE_SPACE}MB (recomendado: 500MB+)"
fi

# 8. Verificar se há instância rodando
check "Instâncias em execução"
if pgrep -f "pdv-compose" > /dev/null; then
    warn "Há uma instância do PDV já rodando"
else
    pass "Nenhuma instância em execução"
fi

# 9. Verificar banco de dados
check "Banco de dados (pdv.db)"
if [ -f "./pdv.db" ]; then
    SIZE=$(du -h pdv.db | cut -f1)
    pass "Banco de dados existe (${SIZE})"
else
    pass "Será criado na primeira execução"
fi

# 10. Verificar cache do Gradle
check "Cache do Gradle"
if [ -d "$HOME/.gradle" ]; then
    CACHE_SIZE=$(du -sh $HOME/.gradle 2>/dev/null | cut -f1)
    pass "Cache existe (${CACHE_SIZE})"
else
    pass "Será criado na primeira execução"
fi

# 11. Verificar JAVA_HOME
check "Variável JAVA_HOME"
if [ -z "$JAVA_HOME" ]; then
    warn "JAVA_HOME não configurado (opcional)"
else
    pass "$JAVA_HOME"
fi

# 12. Verificar documentação
check "Arquivos de documentação"
DOC_FILES=0
[ -f "README.md" ] && DOC_FILES=$((DOC_FILES + 1))
[ -f "INSTALL.md" ] && DOC_FILES=$((DOC_FILES + 1))
[ -f "QUICKSTART.md" ] && DOC_FILES=$((DOC_FILES + 1))
[ -f "TROUBLESHOOTING.md" ] && DOC_FILES=$((DOC_FILES + 1))
if [ "$DOC_FILES" -gt 0 ]; then
    pass "$DOC_FILES arquivos de documentação encontrados"
else
    warn "Arquivos de documentação não encontrados"
fi

echo ""
echo "════════════════════════════════════════════════════════"
echo "  RESULTADO DA VERIFICAÇÃO"
echo "════════════════════════════════════════════════════════"
echo ""
echo -e "Total de verificações: ${BLUE}$TOTAL_CHECKS${NC}"
echo -e "Passou:                ${GREEN}$PASSED_CHECKS${NC}"
echo -e "Avisos:                ${YELLOW}$WARNING_CHECKS${NC}"
echo -e "Falhou:                ${RED}$FAILED_CHECKS${NC}"
echo ""

# Determinar status final
if [ $FAILED_CHECKS -eq 0 ]; then
    if [ $WARNING_CHECKS -eq 0 ]; then
        echo -e "${GREEN}✓ SISTEMA PRONTO PARA EXECUTAR!${NC}"
        echo ""
        echo "Para iniciar o PDV Desktop, execute:"
        echo ""
        echo "    ./run.sh"
        echo ""
        echo "ou"
        echo ""
        echo "    ./gradlew run"
        echo ""
        exit 0
    else
        echo -e "${YELLOW}⚠ SISTEMA PODE SER EXECUTADO COM AVISOS${NC}"
        echo ""
        echo "Há alguns avisos, mas você pode tentar executar:"
        echo ""
        echo "    ./run.sh"
        echo ""
        exit 0
    fi
else
    echo -e "${RED}✗ SISTEMA NÃO ESTÁ PRONTO${NC}"
    echo ""
    echo "Corrija os problemas encontrados antes de executar."
    echo "Consulte o arquivo TROUBLESHOOTING.md para ajuda."
    echo ""
    exit 1
fi

