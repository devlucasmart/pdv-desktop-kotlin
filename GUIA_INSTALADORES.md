# 📦 GUIA COMPLETO - GERAÇÃO DE INSTALADORES

## 🚀 PDV DESKTOP - INSTALADORES NATIVOS

Este guia explica como gerar instaladores profissionais do PDV Desktop para diferentes sistemas operacionais.

---

## 📋 PRÉ-REQUISITOS

### Para Todos os Sistemas:
- ✅ Java JDK 17 ou superior
- ✅ Projeto compilado com sucesso
- ✅ Conexão com internet (primeira vez)
- ✅ Espaço em disco: ~500 MB livres

### Para Instaladores Específicos:
- **Linux (.deb, .rpm)**: Sistema Linux ou Docker
- **Windows (.msi, .exe)**: Windows 10+ ou Wine
- **macOS (.dmg)**: macOS 10.14+ ou Hackintosh

---

## ⚡ MÉTODO RÁPIDO

### Linux/macOS:
```bash
./build-installer.sh
```

### Windows:
```cmd
build-installer.bat
```

**O script irá:**
1. Verificar Java
2. Limpar builds anteriores
3. Compilar o projeto
4. Permitir escolher o tipo de instalador
5. Gerar o(s) instalador(es)
6. Mostrar localização dos arquivos

---

## 🔧 MÉTODO MANUAL

### 1. Compilar o Projeto
```bash
./gradlew clean build --no-daemon
```

### 2. Gerar Instalador Específico

**Linux Debian/Ubuntu (.deb):**
```bash
./gradlew packageDeb --no-daemon
```
**Saída:** `build/compose/binaries/main/deb/pdv-desktop_1.0.0-1_amd64.deb`

**Linux Fedora/RedHat (.rpm):**
```bash
./gradlew packageRpm --no-daemon
```
**Saída:** `build/compose/binaries/main/rpm/pdv-desktop-1.0.0-1.x86_64.rpm`

**macOS (.dmg):**
```bash
./gradlew packageDmg --no-daemon
```
**Saída:** `build/compose/binaries/main/dmg/PDV Desktop-1.0.0.dmg`

**Windows Installer (.msi):**
```bash
./gradlew packageMsi --no-daemon
```
**Saída:** `build/compose/binaries/main/msi/PDV Desktop-1.0.0.msi`

**Windows Executable (.exe):**
```bash
./gradlew packageExe --no-daemon
```
**Saída:** `build/compose/binaries/main/exe/PDV Desktop-1.0.0.exe`

### 3. Gerar TODOS os Instaladores
```bash
./gradlew packageDeb packageRpm packageDmg packageMsi packageExe --no-daemon
```

---

## 📦 TIPOS DE INSTALADORES

### 1. 📦 .deb (Debian/Ubuntu/Mint)
**Plataforma:** Linux
**Tamanho:** ~60-80 MB
**Instalação:**
```bash
sudo dpkg -i pdv-desktop_1.0.0-1_amd64.deb
```
**Desinstalação:**
```bash
sudo apt remove pdv-desktop
```
**Características:**
- ✅ Instalação simples
- ✅ Integração com APT
- ✅ Atalho no menu automaticamente
- ✅ Atualização fácil

### 2. 📦 .rpm (Fedora/RedHat/CentOS)
**Plataforma:** Linux
**Tamanho:** ~60-80 MB
**Instalação:**
```bash
sudo rpm -i pdv-desktop-1.0.0-1.x86_64.rpm
```
**Desinstalação:**
```bash
sudo rpm -e pdv-desktop
```
**Características:**
- ✅ Para distribuições baseadas em RedHat
- ✅ Integração com YUM/DNF
- ✅ Atalho no menu
- ✅ Dependências gerenciadas

### 3. 🍎 .dmg (macOS)
**Plataforma:** macOS 10.14+
**Tamanho:** ~70-90 MB
**Instalação:**
1. Clique duplo no .dmg
2. Arraste "PDV Desktop" para "Applications"
3. Ejetar o .dmg
**Desinstalação:**
- Arraste para Lixeira em Applications
**Características:**
- ✅ Instalação drag-and-drop
- ✅ Aplicativo assinado (se configurado)
- ✅ Integração com Launchpad
- ✅ Ícone personalizado

### 4. 🪟 .msi (Windows Installer)
**Plataforma:** Windows 7+
**Tamanho:** ~50-70 MB
**Instalação:**
1. Clique duplo no .msi
2. Siga o assistente de instalação
3. Escolha o diretório (padrão: Program Files)
**Desinstalação:**
- Painel de Controle > Programas > Desinstalar
**Características:**
- ✅ Instalador profissional do Windows
- ✅ Registro no sistema
- ✅ Atalho no Menu Iniciar
- ✅ Atualização e remoção limpa
- ✅ Suporte a instalação silenciosa

### 5. 🪟 .exe (Windows Executable)
**Plataforma:** Windows 7+
**Tamanho:** ~50-70 MB
**Instalação:**
1. Clique duplo no .exe
2. Siga o instalador
**Características:**
- ✅ Executável auto-contido
- ✅ Mais simples que MSI
- ✅ Pode rodar sem instalação (se configurado)

---

## 📊 COMPARAÇÃO DE FORMATOS

| Formato | SO | Tamanho | Instalação | Recomendado |
|---------|-------|---------|------------|-------------|
| .deb | Linux | ~70 MB | Muito fácil | ✅ Ubuntu/Debian |
| .rpm | Linux | ~70 MB | Muito fácil | ✅ Fedora/RedHat |
| .dmg | macOS | ~80 MB | Fácil | ✅ macOS |
| .msi | Windows | ~60 MB | Fácil | ✅ Windows |
| .exe | Windows | ~60 MB | Fácil | ⚠️ Alternativa |

---

## 🎯 INSTALAÇÃO PARA USUÁRIOS FINAIS

### Linux (Ubuntu/Debian):
```bash
# Download do arquivo
wget https://exemplo.com/pdv-desktop_1.0.0-1_amd64.deb

# Instalar
sudo dpkg -i pdv-desktop_1.0.0-1_amd64.deb

# Se houver dependências faltando
sudo apt install -f

# Executar
pdv-desktop
# Ou procurar "PDV Desktop" no menu
```

### Linux (Fedora):
```bash
# Download do arquivo
wget https://exemplo.com/pdv-desktop-1.0.0-1.x86_64.rpm

# Instalar
sudo dnf install pdv-desktop-1.0.0-1.x86_64.rpm

# Executar
pdv-desktop
```

### macOS:
```bash
1. Baixe o arquivo .dmg
2. Clique duplo para montar
3. Arraste "PDV Desktop.app" para "Applications"
4. Abra Launchpad e procure "PDV Desktop"
5. Na primeira execução, pode precisar:
   - Ir em System Preferences > Security & Privacy
   - Clicar em "Open Anyway"
```

### Windows:
```bash
1. Baixe o arquivo .msi ou .exe
2. Clique duplo para iniciar instalação
3. Siga o assistente:
   - Aceite a licença
   - Escolha o diretório (ou use padrão)
   - Escolha se quer atalho na área de trabalho
   - Clique "Instalar"
4. Após instalação, procure "PDV Desktop" no Menu Iniciar
```

---

## 🔐 ASSINATURA DE CÓDIGO (Opcional)

### Para Distribuição Profissional:

**Windows:**
```bash
# Requer certificado de assinatura de código
signtool sign /f certificado.pfx /p senha /t http://timestamp.server PDV-Desktop-1.0.0.msi
```

**macOS:**
```bash
# Requer Apple Developer Account
codesign --force --sign "Developer ID Application" "PDV Desktop.app"
```

**Linux:**
- .deb e .rpm podem ser assinados com GPG

---

## 📁 ESTRUTURA DOS PACOTES

### O que está incluído:
- ✅ **Aplicativo PDV Desktop** (executável)
- ✅ **Java Runtime** (JRE embarcado)
- ✅ **Bibliotecas necessárias** (Compose, SQLite, etc)
- ✅ **Arquivo de licença** (LICENSE.txt)
- ✅ **Ícones da aplicação**
- ✅ **Scripts de inicialização**
- ✅ **Metadados do pacote**

### O que NÃO está incluído:
- ❌ Banco de dados (criado na primeira execução)
- ❌ Arquivos de configuração do usuário
- ❌ Código fonte (apenas binários)

---

## 🗂️ LOCALIZAÇÃO APÓS INSTALAÇÃO

### Linux (.deb/.rpm):
```
Executável:    /opt/pdv-desktop/bin/pdv-desktop
Dados:         ~/.pdv-desktop/
Atalho:        /usr/share/applications/pdv-desktop.desktop
```

### macOS (.dmg):
```
Aplicativo:    /Applications/PDV Desktop.app
Dados:         ~/Library/Application Support/pdv-desktop/
```

### Windows (.msi):
```
Executável:    C:\Program Files\PDV Desktop\PDV Desktop.exe
Dados:         C:\Users\<usuario>\AppData\Local\pdv-desktop\
Atalho:        Menu Iniciar > PDV Desktop
```

---

## 🚀 DISTRIBUIÇÃO

### Métodos de Distribuição:

**1. Download Direto:**
- Hospede em seu servidor web
- Forneça links diretos de download
- Ex: `https://seusite.com/downloads/pdv-desktop-1.0.0.deb`

**2. Repositórios de Pacotes:**
- **Linux**: Crie repositório APT/YUM
- **Windows**: Microsoft Store (requer submissão)
- **macOS**: Mac App Store (requer conta Developer)

**3. GitHub Releases:**
```bash
# Crie uma release no GitHub
gh release create v1.0.0 \
  build/compose/binaries/main/deb/*.deb \
  build/compose/binaries/main/rpm/*.rpm \
  build/compose/binaries/main/dmg/*.dmg \
  build/compose/binaries/main/msi/*.msi
```

**4. Plataformas de Software:**
- SnapCraft (Linux)
- Flatpak (Linux)
- Chocolatey (Windows)
- Homebrew Cask (macOS)

---

## 🔄 ATUALIZAÇÕES

### Preparar Nova Versão:

1. **Atualizar versão no build.gradle.kts:**
```kotlin
version = "1.0.1"  // ou "1.1.0", "2.0.0"
```

2. **Recompilar e gerar instaladores:**
```bash
./build-installer.sh
```

3. **Distribuir nova versão:**
- Disponibilize os novos instaladores
- Notifique os usuários
- Forneça changelog

### Instalação de Atualizações:

**Linux (.deb):**
```bash
sudo dpkg -i pdv-desktop_1.0.1-1_amd64.deb
# Atualiza automaticamente
```

**Windows (.msi):**
- Execute novo instalador
- Ele detecta versão anterior
- Oferece atualização ou reinstalação

**macOS (.dmg):**
- Substitua a versão antiga em Applications
- Ou use ferramentas de atualização automática

---

## 🧪 TESTAR INSTALADORES

### Antes de Distribuir:

**1. Teste em VM limpa:**
```bash
# Use VirtualBox, VMware ou Docker
# Teste em SO limpo sem Java instalado
# Verifique se instala corretamente
# Teste se executa sem erros
```

**2. Verifique tamanho:**
```bash
ls -lh build/compose/binaries/main/*/*.{deb,rpm,dmg,msi,exe}
```

**3. Teste desinstalação:**
```bash
# Instale e depois desinstale
# Verifique se remove tudo corretamente
# Verifique se não deixa lixo no sistema
```

**4. Teste atualização:**
```bash
# Instale versão 1.0.0
# Depois instale versão 1.0.1
# Verifique se atualiza corretamente
```

---

## ⚠️ PROBLEMAS COMUNS

### Erro: "Java not found"
**Solução:** O Java JRE está embarcado no instalador, não deveria acontecer.
Se ocorrer, reinstale o aplicativo.

### Erro: "Permission denied" (Linux)
**Solução:**
```bash
chmod +x /opt/pdv-desktop/bin/pdv-desktop
```

### Erro: "App is damaged" (macOS)
**Solução:**
```bash
xattr -cr "/Applications/PDV Desktop.app"
```

### Erro: "Unknown publisher" (Windows)
**Solução:** Normal se não for assinado. Clique em "More info" e "Run anyway".

---

## 📊 CHECKLIST DE DISTRIBUIÇÃO

Antes de distribuir, verifique:

- [ ] Compilação sem erros
- [ ] Todos os testes passando
- [ ] Versão atualizada no build.gradle.kts
- [ ] LICENSE.txt atualizado
- [ ] README.md incluído
- [ ] Instalador testado em VM limpa
- [ ] Tamanho do instalador aceitável
- [ ] Ícones incluídos
- [ ] Atalhos funcionando
- [ ] Desinstalação funcionando
- [ ] Banco de dados criado corretamente
- [ ] Login funcionando
- [ ] Todas as funcionalidades testadas

---

## 💡 DICAS PROFISSIONAIS

### Para Distribuição Comercial:
1. ✅ Assine o código (Windows/macOS)
2. ✅ Use ícone personalizado profissional
3. ✅ Inclua documentação PDF
4. ✅ Crie página de download profissional
5. ✅ Forneça checksums (MD5/SHA256)
6. ✅ Ofereça suporte técnico
7. ✅ Mantenha changelog atualizado

### Para Distribuição Interna:
1. ✅ Hospede em servidor interno
2. ✅ Configure atualização automática
3. ✅ Documente processo de instalação
4. ✅ Treine usuários
5. ✅ Monitore uso e problemas

---

## 📝 RESUMO DOS COMANDOS

```bash
# Gerar instalador DEB (Linux Ubuntu/Debian)
./gradlew packageDeb

# Gerar instalador RPM (Linux Fedora/RedHat)
./gradlew packageRpm

# Gerar instalador DMG (macOS)
./gradlew packageDmg

# Gerar instalador MSI (Windows)
./gradlew packageMsi

# Gerar executável EXE (Windows)
./gradlew packageExe

# Gerar TODOS
./gradlew packageDeb packageRpm packageDmg packageMsi packageExe

# Script automatizado
./build-installer.sh     # Linux/macOS
build-installer.bat      # Windows
```

---

## 🎉 PRONTO PARA DISTRIBUIR!

Seu sistema PDV Desktop está pronto para ser distribuído profissionalmente!

**Próximos passos:**
1. Escolha o(s) formato(s) de instalador
2. Execute o script de build
3. Teste o instalador
4. Distribua aos usuários
5. Forneça suporte e atualizações

---

**Versão:** 1.0.0  
**Data:** 2026-03-09  
**Documentação completa de geração de instaladores**

Para dúvidas ou problemas, consulte:
- TROUBLESHOOTING.md
- README.md
- Documentação do Compose for Desktop

