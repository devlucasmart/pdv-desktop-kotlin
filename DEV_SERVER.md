DEV_SERVER — Documentação do servidor embutido
===============================================

Visão geral
----------
Este documento descreve a implementação do servidor embutido adicionada ao módulo `pdv-compose` (arquivos principais: `EmbeddedServer.kt`, `SyncService.kt`, `OutboxDao.kt`, `Config.kt`). O objetivo é fornecer informação prática para desenvolvedores: como ativar e configurar o servidor local, como os endpoints funcionam, como funciona a replicação (outbox + SyncService), como habilitar TLS, testes comuns e troubleshooting.

Sumário Rápido
--------------
- Localização do código relevante
  - `src/main/kotlin/com/pdv/server/EmbeddedServer.kt` — servidor HTTP/HTTPS e endpoints.
  - `src/main/kotlin/com/pdv/sync/SyncService.kt` — processador da fila outbox, retry/backoff.
  - `src/main/kotlin/com/pdv/data/OutboxDao.kt` — DAO com fields: payload, status, attempts, last_error, last_attempt_at.
  - `src/main/kotlin/com/pdv/data/Config.kt` — getters/setters e persistência de configurações.
  - `src/main/kotlin/com/pdv/Main.kt` — inicialização: chama `EmbeddedServer.startFromConfig()` e `SyncService.start()`.

Checklist do que este documento cobre
-----------------------------------
- [x] Configurações (properties / env vars / system props)
- [x] TLS (geração/uso de keystore para HTTPS)
- [x] Endpoints expostos (métodos, payloads, respostas)
- [x] Modelo e comportamento do Outbox (colunas, estados, methods)
- [x] SyncService: retry/backoff/limites
- [x] Como testar (curl + queries sqlite)
- [x] Troubleshooting e próximas melhorias

1) Como ativar o servidor
-------------------------
Opções de ativação:
- Pela UI: aba Configurações -> "Servidor e Sincronização" -> marcar modo servidor e "Salvar e Aplicar".
- Via environment / system properties ao iniciar:

```bash
# via env
PDV_SERVER=1 PDV_SERVER_PORT=8080 ./gradlew run

# via system properties (Gradle)
./gradlew run -Dpdv.server.enabled=true -Dpdv.server.port=8080 -Dpdv.server.bind=127.0.0.1
```

Configurações importantes (persistidas via `Config` ou via UI):
- `server.enabled` (boolean)
- `server.bind` (string) — host a bindar, ex: `127.0.0.1` ou `0.0.0.0`
- `server.port` (int) — ex: `8080`
- `server.token` (string) — token Bearer para autenticação (opcional)
- `server.ssl.enabled` (boolean) — habilita HTTPS
- `server.ssl.keystore.path` (string) — caminho absoluto do JKS
- `server.ssl.keystore.password` (string)
- `remote.url` (string) — URL remota para replicação
- `remote.token` (string) — token para autenticar com a remote

2) TLS (HTTPS)
--------------
Para habilitar HTTPS localmente é necessário um keystore JKS. Exemplo de geração (dev):

```bash
keytool -genkeypair -alias pdv -keyalg RSA -keysize 2048 \
  -keystore ~/pdv-keystore.jks -validity 3650 \
  -dname "CN=localhost, OU=PDV, O=Empresa, L=Cidade, ST=Estado, C=BR" \
  -storepass suaSenha -keypass suaSenha
```
Depois, configure `server.ssl.enabled=true`, `server.ssl.keystore.path` e `server.ssl.keystore.password` no arquivo de configurações ou via UI.

Observações:
- Se o keystore falhar ao carregar, o servidor fará fallback para HTTP e logará a razão.
- Em produção, use certificados válidos (CA) e não um keystore autoassinado sem considerar confiança no cliente.

3) Endpoints disponíveis
------------------------
Observação: quando `server.token` estiver definido, endpoints exigem header `Authorization: Bearer <token>`.

- GET /health
  - Retorno: 200 {"status":"ok"}

- POST /api/auth
  - Body JSON: { "username": "user", "password": "pass" }
  - Retorno 200: user info JSON (id, username, fullName, role)
  - 401 se credenciais inválidas

- POST /api/sales
  - Body JSON (exemplo):
    {
      "items": [ { "product_id":1, "sku":"SKU1", "name":"P1", "unit_price":10.0, "quantity":1, "discount":0.0 } ],
      "discount": 0.0,
      "payment_method": "DINHEIRO",
      "operator_name": "Operador"
    }
  - Retorno 201: { "saleId": 123 }

- GET /api/sales?start=YYYY-MM-DD&end=YYYY-MM-DD
  - Retorna array de vendas resumidas (id, date_time, total, subtotal, discount, payment_method, status, operator_name)

- GET /api/cash/movements?start=YYYY-MM-DD&end=YYYY-MM-DD
  - Retorna movimentações do caixa no período

- POST /api/cash/withdraw
  - Body JSON: { "session_id": 1, "amount": 50.0, "description":"Sangria", "operator":"Caixa" }
  - Retorno 201: { "id": 42 }

4) Outbox — fila de replicação
------------------------------
Estrutura (campos relevantes, como implementado em `OutboxDao`):
- id (PK)
- payload (TEXT) — JSON que será enviado para remote
- status (TEXT) — PENDING | SYNCHED | FAILED
- attempts (INT)
- last_error (TEXT, nullable)
- last_attempt_at (DATETIME, nullable)

Comportamento:
- Ao salvar uma venda localmente, se `remote.url` estiver configurada, um registro é criado em outbox com `status=PENDING` e `attempts=0`.
- `SyncService` processa PENDING, tenta POST no `remote.url` com Authorization Bearer `remote.token` (se definido).
- Em caso de sucesso (HTTP 2xx), item é marcado SYNCHED.
- Em caso de falha, `attempts` aumenta e `last_error`/`last_attempt_at` são registrados; quando `attempts >= MAX_ATTEMPTS` o item é marcado FAILED.

Consultas úteis (SQLite):

```sql
-- listar pendentes
SELECT id, attempts, last_error, last_attempt_at FROM outbox WHERE status='PENDING' ORDER BY id DESC;

-- listar falhos
SELECT id, attempts, last_error, last_attempt_at FROM outbox WHERE status='FAILED';
```

5) SyncService — retries e backoff
---------------------------------
Comportamento resumido:
- Busca itens PENDING via `OutboxDao.listPending()` (limite configurável).
- Para cada item:
  - Se `attempts >= MAX_ATTEMPTS` -> `markFailed(id)` e pular.
  - Tenta envio HTTP (connect/read timeout configurados).
  - Se retorno HTTP 2xx -> `markSynced(id)`.
  - Se falha -> `incrementAttempt(id, error)` e aguarda backoff exponencial relativo ao número de tentativas antes de tentar novamente.
- `SyncService.start()` inicia um loop periódico (ex.: a cada 30s) — `SyncService.stop()` interrompe.

6) Testes e comandos rápidos
---------------------------
Health check:

```bash
curl -v http://127.0.0.1:8080/health
```

Autenticar:

```bash
curl -X POST http://127.0.0.1:8080/api/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Criar venda (exemplo):

```bash
curl -X POST http://127.0.0.1:8080/api/sales \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN" \
  -d '{ "items": [ { "product_id":1, "sku":"SKU1", "name":"Produto 1", "unit_price":10.0, "quantity":1 } ], "discount":0.0, "payment_method":"DINHEIRO", "operator_name":"Teste" }'
```

Inspecionar DB (sqlite):

```bash
sqlite3 pdv_data.db "SELECT id, status, attempts, last_error, last_attempt_at FROM outbox ORDER BY id DESC LIMIT 50;"
```

7) Logs e troubleshooting
-------------------------
- Logs principais vão para stdout (console). Verifique mensagens de:
  - Inicialização do EmbeddedServer (bind/port)
  - Falha ao carregar keystore (mensagem de fallback para HTTP)
  - Resultado das inserções de venda (saleId, SQL exceptions)
  - SyncService: sucesso/falha dos envios outbox e número de attempts

Problemas comuns e correções:
- "Não foi possível obter ID da venda" — verifique `SaleDao.save` e se o driver SQLite retornou `last_insert_rowid()`. Cheque no DB se a venda foi inserida.
- Outbox não sincroniza — verifique `Config.remoteUrl` e `SyncService` logs; teste `curl` contra `remote.url` para validar autenticação.
- Erro HTTPS/keystore — confirme caminho e senha do JKS; teste HTTP primeiro.

8) Recomendados próximos passos (melhorias)
------------------------------------------
- Emitir tokens JWT via endpoint `/api/login` ao invés de usar `server.token` estático.
- Adicionar métricas e endpoint `/metrics` (Prometheus) para monitorar filas e latência.
- Implementar testes automatizados (unit + integração) que:
  - criem uma venda via `SaleDao` e verifiquem DB
  - simulem um servidor remoto (mock) e testem `SyncService`
- Migrar para SQLDelight (ou KMP) se quiser compartilhar a camada de dados entre Desktop e Android.

9) Onde alterar comportamento
----------------------------
- Retry / MAX_ATTEMPTS / timeouts: `SyncService.kt` (constantes e lógica de backoff).
- Endpoints e rotas: `EmbeddedServer.kt`.
- Schema e DAO: `OutboxDao.kt`, `SaleDao.kt`, `Database.kt`.
- Config persistence: `Config.kt`.

Contato / notas finais
----------------------
Se quiser, eu posso: 
- adicionar este arquivo ao repositório (já criado no caminho `DEV_SERVER.md`),
- criar scripts de smoke-test automáticos que iniciem a app, façam POST /api/sales e verifiquem DB/outbox,
- implementar endpoint de emissão de tokens JWT,
- ou preparar o passo-a-passo para migrar a persistência para SQLDelight.

-- Fim de `DEV_SERVER.md` --

