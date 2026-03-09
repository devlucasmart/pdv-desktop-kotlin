@echo off
REM Script de execução do PDV Desktop para Windows
REM Autor: Sistema PDV
REM Data: 2026-03-09

echo ==========================================
echo    PDV Desktop - Sistema de Vendas
echo ==========================================
echo.

echo Verificando instalacao do Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Java nao encontrado!
    echo.
    echo Por favor, instale o Java JDK 17 ou superior de:
    echo https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo [OK] Java encontrado
echo.
echo Iniciando PDV Desktop...
echo.
echo Na primeira execucao, pode demorar alguns minutos
echo para baixar as dependencias...
echo.
echo ----------------------------------------
echo.

gradlew.bat run

echo.
echo ----------------------------------------
if errorlevel 1 (
    echo [ERRO] Erro ao executar o PDV Desktop
    echo.
    echo Tente executar:
    echo   gradlew.bat clean build
    echo.
) else (
    echo [OK] PDV Desktop encerrado com sucesso
)
echo ==========================================
echo.
pause

