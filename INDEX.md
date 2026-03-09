# 📋 ÍNDICE DE DOCUMENTAÇÃO - PDV Desktop

## 🎯 Para Começar Agora

1. **[QUICKSTART.md](QUICKSTART.md)** ⭐
   - Guia rápido de 2 minutos
   - Como executar em 1 comando
   - Perfeito para iniciantes

## 📚 Documentação Completa

2. **[INSTALL.md](INSTALL.md)**
   - Guia completo de instalação
   - Instalação do Java
   - Métodos de execução
   - Primeira configuração

3. **[README.md](README.md)**
   - Visão geral do projeto
   - Características e funcionalidades
   - Arquitetura do sistema
   - Tecnologias utilizadas

## 🔧 Resolução de Problemas

4. **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)**
   - 13 problemas mais comuns e soluções
   - Como fazer debug
   - Checklist de verificação

## 💻 Referência Técnica

5. **[COMMANDS.md](COMMANDS.md)**
   - Todos os comandos úteis
   - Atalhos e truques
   - Comandos de desenvolvimento

## 🚀 Arquivos de Execução

6. **run.sh** (Linux/macOS)
   - Script de execução automática
   - Verifica Java automaticamente
   - Execute: `./run.sh`

7. **run.bat** (Windows)
   - Script de execução para Windows
   - Duplo clique ou `run.bat`

---

## 📖 Fluxo Recomendado de Leitura

### Para Usuários Finais:
```
QUICKSTART.md → Execute o programa
```

### Para Desenvolvedores:
```
QUICKSTART.md → INSTALL.md → README.md → COMMANDS.md
```

### Se tiver problemas:
```
TROUBLESHOOTING.md → INSTALL.md
```

---

## 🎓 Tutoriais por Objetivo

### "Quero apenas executar o programa"
→ [QUICKSTART.md](QUICKSTART.md)

### "Quero entender como instalar tudo"
→ [INSTALL.md](INSTALL.md)

### "Quero saber todas as funcionalidades"
→ [README.md](README.md)

### "Está dando erro"
→ [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

### "Quero desenvolver/modificar"
→ [COMMANDS.md](COMMANDS.md) + [README.md](README.md)

---

## 📞 Suporte Rápido

### Problema com Java?
```bash
# Ver se está instalado
java -version

# Instalar (Ubuntu/Debian)
sudo apt install openjdk-17-jdk
```

### Problema com permissões?
```bash
chmod +x gradlew run.sh
```

### Problema de dependências?
```bash
./gradlew clean build --refresh-dependencies
```

---

## 📂 Estrutura dos Arquivos

```
pdv-compose/
├── 📖 Documentação
│   ├── INDEX.md               ← Você está aqui!
│   ├── QUICKSTART.md          ← Comece aqui!
│   ├── INSTALL.md             ← Instalação completa
│   ├── README.md              ← Documentação técnica
│   ├── TROUBLESHOOTING.md     ← Resolução de problemas
│   └── COMMANDS.md            ← Referência de comandos
│
├── 🚀 Scripts de Execução
│   ├── run.sh                 ← Linux/macOS
│   └── run.bat                ← Windows
│
├── 🔧 Configuração
│   ├── build.gradle.kts       ← Build do projeto
│   ├── settings.gradle.kts    ← Configurações Gradle
│   └── gradle.properties      ← Propriedades
│
└── 💻 Código Fonte
    └── src/main/kotlin/com/pdv/
        ├── Main.kt            ← Entrada do programa
        ├── data/              ← Camada de dados
        └── ui/screens/        ← Interfaces
```

---

## 🎯 Checklist Rápido

Antes de executar pela primeira vez:

- [ ] Java 17+ instalado (`java -version`)
- [ ] Permissões configuradas (`chmod +x gradlew run.sh`)
- [ ] Leu o QUICKSTART.md
- [ ] Está no diretório correto (`cd pdv-compose`)

Para executar:
```bash
./run.sh
```

---

## 🔍 Pesquisa Rápida

| Preciso de... | Veja o arquivo... |
|---------------|-------------------|
| Executar agora | QUICKSTART.md |
| Instalar Java | INSTALL.md |
| Resolver erro | TROUBLESHOOTING.md |
| Comandos úteis | COMMANDS.md |
| Documentação completa | README.md |
| Estrutura do projeto | README.md |
| Como funciona | README.md |

---

## 💡 Dicas

1. **Primeira vez?** Leia o [QUICKSTART.md](QUICKSTART.md)
2. **Problemas?** Consulte o [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
3. **Desenvolvendo?** Use o [COMMANDS.md](COMMANDS.md)

---

## 📊 Status do Projeto

- ✅ Interface gráfica completa
- ✅ Gerenciamento de produtos
- ✅ Sistema de vendas
- ✅ Relatórios e estatísticas
- ✅ Banco de dados SQLite
- ✅ Dados de exemplo
- ✅ Documentação completa

---

## 🌟 Recursos Principais

1. **Vendas** - PDV completo com carrinho
2. **Produtos** - Gerenciamento de estoque
3. **Relatórios** - Estatísticas em tempo real
4. **Configurações** - Personalização do sistema

---

**Versão:** 1.0.0  
**Última atualização:** 2026-03-09  
**Licença:** Livre para uso educacional e comercial

---

## 🚀 Execução Rápida

```bash
# Linux/macOS
./run.sh

# Windows
run.bat

# Ou diretamente
./gradlew run
```

Pronto para começar? Vá para [QUICKSTART.md](QUICKSTART.md)! 🎉

