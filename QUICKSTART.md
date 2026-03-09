# 🎯 INÍCIO RÁPIDO - PDV Desktop

## Para Usuários Linux/macOS

### Opção mais simples:
```bash
./run.sh
```

### Ou manualmente:
```bash
./gradlew run
```

## Para Usuários Windows

### Duplo clique em:
```
run.bat
```

### Ou via terminal:
```cmd
gradlew.bat run
```

---

## ⚠️ Se der erro de "Java não encontrado"

### 1. Verificar se Java está instalado:
```bash
java -version
```

### 2. Se não estiver instalado:

**Ubuntu/Debian:**
```bash
sudo apt update && sudo apt install openjdk-17-jdk
```

**Fedora:**
```bash
sudo dnf install java-17-openjdk
```

**Arch Linux:**
```bash
sudo pacman -S jdk-openjdk
```

**Windows:**
- Baixe de: https://adoptium.net/
- Instale e reinicie o terminal

### 3. Após instalar o Java:
```bash
./run.sh
```

---

## 📱 Primeira Vez?

1. Execute o programa
2. Aguarde o download das dependências (primeira vez demora ~5 minutos)
3. O sistema abrirá automaticamente
4. Teste com os produtos de exemplo:
   - Digite SKU: `001` na tela de Vendas
   - Clique em "Adicionar"
   - Finalize a venda

---

## 🆘 Precisa de Ajuda?

Consulte os arquivos:
- **INSTALL.md** - Guia completo de instalação
- **README.md** - Documentação completa do sistema

---

## 🚀 Estrutura Simples

```
pdv-compose/
├── run.sh          ← Execute este arquivo! (Linux/macOS)
├── run.bat         ← Execute este arquivo! (Windows)
├── INSTALL.md      ← Guia de instalação
└── README.md       ← Documentação completa
```

---

**Nota:** Na primeira execução, o Gradle será baixado automaticamente.
Isso pode levar alguns minutos dependendo da sua conexão.

