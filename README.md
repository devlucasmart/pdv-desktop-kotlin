# 🛒 PDV Desktop - Sistema de Ponto de Venda

Sistema completo de Ponto de Venda (PDV) desenvolvido em **Kotlin + Compose for Desktop + SQLite**.

![Status](https://img.shields.io/badge/status-ready-brightgreen) ![Version](https://img.shields.io/badge/version-1.0.0-blue)

## ✨ Funcionalidades Implementadas

### 🛍️ Tela de Vendas
- ✅ Busca de produtos por SKU ou código de barras
- ✅ Adicionar/remover produtos do carrinho
- ✅ Ajustar quantidade de itens
- ✅ Cálculo automático de totais
- ✅ Sistema de descontos
- ✅ Múltiplas formas de pagamento (Dinheiro, Débito, Crédito, PIX)
- ✅ Finalização de venda com persistência
- ✅ Feedback visual (snackbars e diálogos)

### 📦 Gerenciamento de Produtos
- ✅ Listagem completa de produtos
- ✅ Busca e filtro por nome, SKU ou categoria
- ✅ Cadastro de novos produtos
- ✅ Edição de produtos existentes
- ✅ Exclusão (soft delete)
- ✅ Controle de estoque
- ✅ Alertas de estoque baixo
- ✅ Categorização de produtos

### 📊 Relatórios e Estatísticas
- ✅ Faturamento total e do dia
- ✅ Quantidade de vendas
- ✅ Ticket médio
- ✅ Total de produtos cadastrados
- ✅ Produtos com estoque baixo
- ✅ Visualização em cards coloridos

### ⚙️ Configurações
- ✅ Tela de configurações básicas
- ✅ Informações do sistema
- ✅ Preparado para impressoras e backup

## 🚀 Como Executar

### Requisitos
- **JDK 17 ou superior** ([Download](https://adoptium.net/))
- Sistema operacional: Windows, Linux ou macOS

### 1. Rodar em Modo Desenvolvimento

```bash
./gradlew run
```

A aplicação irá:
1. Criar automaticamente o banco SQLite (`pdv.db`)
2. Popular com 10 produtos de exemplo
3. Abrir a interface gráfica

### 2. Compilar

```bash
./gradlew build
```

### 3. Criar Pacote Distribuível

```bash
# Para o seu sistema operacional
./gradlew packageDistributionForCurrentOS

# Para todos os sistemas
./gradlew package
```

Os pacotes serão gerados em: `build/compose/binaries/main/`

## 📱 Como Usar

### Tela de Vendas
1. Digite o **SKU** do produto no campo de busca (ou use um leitor de código de barras)
2. Clique em **Adicionar** ou pressione Enter
3. Ajuste a **quantidade** usando os botões + e -
4. Clique em **Finalizar Venda**
5. Selecione a **forma de pagamento**
6. Confirme a venda

**SKUs de exemplo disponíveis:**
- `001` - Coca-Cola 2L (R$ 8,50)
- `002` - Pão Francês (R$ 12,00/kg)
- `003` - Arroz Tipo 1 5kg (R$ 25,90)
- `004` - Feijão Preto 1kg (R$ 7,80)
- `005` - Café Torrado 500g (R$ 15,50)
- `006` - Açúcar Cristal 1kg (R$ 4,20)
- `007` - Leite Integral 1L (R$ 5,80)
- `008` - Manteiga 500g (R$ 18,90)
- `009` - Óleo de Soja 900ml (R$ 7,50)
- `010` - Macarrão Espaguete 500g (R$ 4,50)

### Gerenciar Produtos
1. Acesse a aba **Produtos** no menu lateral
2. Use a busca para filtrar produtos
3. Clique em **Novo Produto** para cadastrar
4. Use os ícones de editar ✏️ ou deletar 🗑️ em cada produto

### Ver Relatórios
1. Acesse a aba **Relatórios**
2. Visualize estatísticas em tempo real:
   - Faturamento do dia e total
   - Quantidade de vendas
   - Ticket médio
   - Status do estoque

## 🗂️ Estrutura do Projeto

```
pdv-compose/
├── build.gradle.kts              # Configuração Gradle
├── settings.gradle.kts
├── gradle.properties
├── src/main/
│   ├── kotlin/com/pdv/
│   │   ├── Main.kt              # Aplicação principal e navegação
│   │   ├── data/
│   │   │   ├── Database.kt      # Conexão SQLite e migrations
│   │   │   ├── Models.kt        # Entidades (Product, Sale, SaleItem)
│   │   │   ├── ProductDao.kt    # Operações de produtos
│   │   │   └── SaleDao.kt       # Operações de vendas
│   │   └── ui/screens/
│   │       ├── SalesScreen.kt   # Tela de vendas (PDV)
│   │       ├── ProductsScreen.kt # Gestão de produtos
│   │       └── ReportsScreen.kt  # Relatórios
│   └── resources/
│       └── logback.xml          # Configuração de logs
└── README.md
```

## 🗄️ Banco de Dados

O sistema usa **SQLite** com as seguintes tabelas:

### `product`
- `id` - ID único (auto incremento)
- `sku` - Código do produto (único)
- `name` - Nome do produto
- `price` - Preço unitário
- `stock_quantity` - Quantidade em estoque
- `category` - Categoria (opcional)
- `active` - Status (ativo/inativo)
- `created_at`, `updated_at` - Timestamps

### `sale`
- `id` - ID único
- `date_time` - Data/hora da venda
- `total` - Valor total
- `subtotal` - Subtotal (antes de descontos)
- `discount` - Valor de desconto
- `payment_method` - Forma de pagamento
- `status` - Status da venda
- `operator_name` - Nome do operador

### `sale_item`
- `id` - ID único
- `sale_id` - FK para venda
- `product_id` - FK para produto
- `quantity` - Quantidade vendida
- `unit_price` - Preço unitário na venda
- `total_price` - Preço total do item
- `discount` - Desconto aplicado

## 🛠️ Tecnologias Utilizadas

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Kotlin | 1.9.22 | Linguagem de programação |
| Compose for Desktop | 1.5.12 | Framework UI declarativo |
| SQLite JDBC | 3.45.0.0 | Banco de dados local |
| Coroutines | 1.7.3 | Programação assíncrona |
| Logback | 1.4.14 | Sistema de logs |

## 🎨 Interface

- **Material Design** - Design moderno e responsivo
- **Navegação lateral** - Acesso rápido às funcionalidades
- **Feedback visual** - Snackbars e diálogos informativos
- **Cards coloridos** - Organização visual clara
- **Ícones intuitivos** - Facilita a usabilidade

## 📈 Próximas Funcionalidades

- [ ] Impressão de cupom fiscal
- [ ] Integração com impressora térmica
- [ ] Suporte a leitor de código de barras USB
- [ ] Sistema de backup automático
- [ ] Relatórios gráficos (charts)
- [ ] Exportação para Excel/PDF
- [ ] Gestão de usuários e permissões
- [ ] Integração fiscal (NFC-e, SAT)
- [ ] Sistema de caixa (abertura/fechamento)
- [ ] Sincronização com servidor/nuvem

## 🐛 Troubleshooting

### Erro ao iniciar
```bash
# Limpar cache e recompilar
./gradlew clean build
```

### Banco de dados corrompido
```bash
# Deletar e recriar
rm pdv.db
./gradlew run
```

### Problemas com Java
```bash
# Verificar versão do Java
java -version

# Deve ser 17 ou superior
```

## 📝 Logs

Os logs são salvos em:
- Console (saída padrão)
- Arquivo `pdv.log` no diretório raiz

## 🤝 Contribuindo

Contribuições são bem-vindas! Sinta-se à vontade para:
1. Fazer um fork do projeto
2. Criar uma branch para sua feature
3. Commit suas mudanças
4. Push para a branch
5. Abrir um Pull Request

## 📄 Licença

Este projeto é um sistema de demonstração/educacional.

## 👨‍💻 Autor

Desenvolvido com ❤️ usando Kotlin e Compose for Desktop

---

**Versão:** 1.0.0  
**Data:** Março 2026  
**Status:** ✅ Pronto para uso

Para mais informações, consulte `DOCUMENTATION.md` e `ROADMAP.md`.

