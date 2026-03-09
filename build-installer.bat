@echo off
REM Script para gerar instaladores do PDV Desktop - Windows
REM Autor: PDV Systems
REM Data: 2026-03-09

echo ================================================================
echo.
echo      GERADOR DE INSTALADORES - PDV DESKTOP
echo.
echo ================================================================
echo.

echo [1/4] Verificando Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Java nao encontrado!
    echo Instale Java 17+ primeiro.
    pause
    exit /b 1
)
echo [OK] Java encontrado
echo.

echo [2/4] Limpando builds anteriores...
call gradlew.bat clean --no-daemon
if errorlevel 1 (
    echo [ERRO] Falha na limpeza
    pause
    exit /b 1
)
echo [OK] Limpeza concluida
echo.

echo [3/4] Compilando projeto...
call gradlew.bat build --no-daemon
if errorlevel 1 (
    echo [ERRO] Falha na compilacao!
    pause
    exit /b 1
)
echo [OK] Compilacao bem-sucedida
echo.

echo [4/4] Selecione o tipo de instalador:
echo.
echo   1) MSI (Windows Installer)
echo   2) EXE (Executavel Windows)
echo   3) AMBOS (MSI + EXE)
echo   4) Cancelar
echo.
set /p choice="Digite sua escolha [1-4]: "

if "%choice%"=="1" (
    echo.
    echo Gerando instalador .msi...
    call gradlew.bat packageMsi --no-daemon
    set INSTALLER_TYPE=MSI
) else if "%choice%"=="2" (
    echo.
    echo Gerando executavel .exe...
    call gradlew.bat packageExe --no-daemon
    set INSTALLER_TYPE=EXE
) else if "%choice%"=="3" (
    echo.
    echo Gerando MSI e EXE...
    call gradlew.bat packageMsi packageExe --no-daemon
    set INSTALLER_TYPE=AMBOS
) else if "%choice%"=="4" (
    echo Operacao cancelada.
    pause
    exit /b 0
) else (
    echo Opcao invalida!
    pause
    exit /b 1
)

if errorlevel 1 (
    echo.
    echo ================================================================
    echo [ERRO] Falha ao gerar instalador
    echo ================================================================
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================================
echo [OK] INSTALADOR(ES) GERADO(S) COM SUCESSO!
echo ================================================================
echo.

echo Instaladores disponiveis em:
echo   build\compose\binaries\main\msi\
echo   build\compose\binaries\main\exe\
echo.

if exist "build\compose\binaries\main\msi\*.msi" (
    echo [+] Arquivos MSI:
    dir /b build\compose\binaries\main\msi\*.msi
    echo.
)

if exist "build\compose\binaries\main\exe\*.exe" (
    echo [+] Arquivos EXE:
    dir /b build\compose\binaries\main\exe\*.exe
    echo.
)

echo ================================================================
echo.
echo Instalacao:
echo   1. Clique duplo no arquivo .msi ou .exe
echo   2. Siga as instrucoes do instalador
echo   3. Execute "PDV Desktop" no Menu Iniciar
echo.
echo Informacoes:
echo   Nome: PDV Desktop
echo   Versao: 1.0.0
echo   Tamanho: ~50-80 MB
echo.
echo ================================================================
echo.
pause

