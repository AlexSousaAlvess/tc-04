# Plataforma de Feedback - FIAP Tech Challenge Fase 4

Plataforma serverless para coleta e análise de feedbacks de cursos online, com notificações automáticas para itens críticos e relatórios semanais.

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────────┐
│                            AWS Cloud                                 │
│                                                                      │
│   Cliente          API Gateway          Lambda 1                     │
│  ─────────  POST /avaliacao  ───────►  feedback-api                  │
│                                             │                        │
│                                             │ publica                │
│                                             ▼                        │
│                                    ┌─ SQS feedback-queue.fifo ─┐     │
│                                    │                            │     │
│                                    └────────────────────────────┘     │
│                                             │ trigger                │
│                                             ▼                        │
│                                        Lambda 2                      │
│                                     feedback-processor               │
│                                       │         │                    │
│                              salva    │         │ se nota ≤ 3        │
│                                       ▼         ▼                    │
│                              RDS PostgreSQL  SQS notification-queue  │
│                                                  │                   │
│                                                  │ trigger           │
│                                                  ▼                   │
│                                             Lambda 3                 │
│                                          notification-sender         │
│                                                  │                   │
│                                                  │ e-mail urgente    │
│                                                  ▼                   │
│                                              Amazon SES              │
│                                                                      │
│   EventBridge Scheduler                                              │
│   (toda segunda 08:00 BRT)  ──────────────►  Lambda 4               │
│                                           weekly-report              │
│                                              │         │             │
│                                    consulta  │         │ e-mail      │
│                                              ▼         ▼             │
│                                      RDS PostgreSQL  Amazon SES      │
│                                                                      │
│   CloudWatch: Logs, Métricas, Alarmes, Dashboard                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Componentes AWS

| Componente | Serviço AWS | Descrição |
|---|---|---|
| API REST | API Gateway v2 (HTTP API) | Endpoint `POST /avaliacao` |
| Lambda 1 | `feedback-api` | Valida e publica feedback na fila |
| Lambda 2 | `feedback-processor` | Persiste no banco e detecta urgência |
| Lambda 3 | `notification-sender` | Envia e-mail urgente via SES |
| Lambda 4 | `weekly-report` | Gera e envia relatório semanal |
| Filas | SQS FIFO | `feedback-queue.fifo` e `notification-queue.fifo` |
| Banco | RDS PostgreSQL 16 | Dentro de VPC privada |
| E-mail | Amazon SES | Notificações e relatórios |
| Agendamento | EventBridge Scheduler | Segunda-feira 08:00 BRT |
| Monitoramento | CloudWatch | Logs, métricas, alarmes, dashboard |
| Rede | VPC + Subnets | RDS e Lambdas com banco em subnet privada |

## Funções Serverless

### Lambda 1 — `feedback-api`
**Trigger:** API Gateway `POST /avaliacao`

Recebe e valida o feedback, calcula a urgência e publica na fila SQS `feedback-queue.fifo`.

**Request:**
```json
{
  "descricao": "string",
  "nota": 0
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "urgencia": "CRITICA|MEDIA|BAIXA",
  "mensagem": "Feedback recebido com sucesso"
}
```

**Regras de urgência:**
- `CRITICA`: nota de 0 a 3 → dispara notificação imediata
- `MEDIA`: nota de 4 a 6
- `BAIXA`: nota de 7 a 10

---

### Lambda 2 — `feedback-processor`
**Trigger:** SQS `feedback-queue.fifo` (batch de até 10 mensagens)

Persiste o feedback no PostgreSQL. Se a urgência for `CRITICA`, publica na fila `notification-queue.fifo`.

---

### Lambda 3 — `notification-sender`
**Trigger:** SQS `notification-queue.fifo` (batch de até 5 mensagens)

Envia e-mail HTML ao administrador via Amazon SES com os dados:
- Descrição do feedback
- Urgência
- Data de envio
- ID do feedback

---

### Lambda 4 — `weekly-report`
**Trigger:** EventBridge Scheduler (toda segunda-feira às 08:00 BRT)

Consulta o PostgreSQL com os feedbacks da semana anterior (D-7 a D-1) e envia relatório HTML com:
- Descrição dos feedbacks
- Urgência de cada feedback
- Data de envio
- Quantidade de avaliações por dia
- Quantidade de avaliações por urgência
- Média das notas

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose
- AWS CLI v2 configurado
- Terraform 1.6+
- Conta AWS com SES verificado

## Desenvolvimento Local

```bash
# 1. Subir PostgreSQL e LocalStack (simula SQS e SES)
docker compose up -d

# 2. Verificar saúde dos serviços
docker compose ps

# 3. Buildar todos os módulos
mvn clean package -DskipTests

# 4. Rodar os testes
mvn test
```

## Deploy na AWS

### 1. Provisionamento da infraestrutura (uma vez)

```bash
cd infrastructure/terraform

# Copiar e preencher variáveis
cp terraform.tfvars.example terraform.tfvars
# Editar terraform.tfvars com seus valores

# Inicializar e aplicar
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

**Importante:** Antes do primeiro deploy, verifique o e-mail remetente no SES:
```bash
aws ses verify-email-identity --email-address noreply@suaempresa.com
```

### 2. Deploy das Lambdas (automatizado via GitHub Actions)

O pipeline `.github/workflows/deploy.yml` é acionado automaticamente em cada push para `main`:
1. Builda todos os módulos com Maven
2. Executa os testes
3. Faz upload dos ZIPs para cada Lambda

**Secrets necessários no GitHub:**
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

### 3. Deploy manual (alternativa)

```bash
# Buildar
mvn clean package -DskipTests

# Deploy de cada Lambda
aws lambda update-function-code \
  --function-name feedback-platform-prod-feedback-api \
  --zip-file fileb://feedback-api/target/function.zip

aws lambda update-function-code \
  --function-name feedback-platform-prod-feedback-processor \
  --zip-file fileb://feedback-processor/target/function.zip

aws lambda update-function-code \
  --function-name feedback-platform-prod-notification-sender \
  --zip-file fileb://notification-sender/target/function.zip

aws lambda update-function-code \
  --function-name feedback-platform-prod-weekly-report \
  --zip-file fileb://weekly-report/target/function.zip
```

## Variáveis de Ambiente das Lambdas

| Lambda | Variável | Descrição |
|---|---|---|
| feedback-api | `FEEDBACK_QUEUE_URL` | URL da fila SQS de feedback |
| feedback-api | `AWS_REGION` | Região AWS |
| feedback-processor | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão ao RDS |
| feedback-processor | `NOTIFICATION_QUEUE_URL` | URL da fila SQS de notificações |
| notification-sender | `ADMIN_EMAIL` | E-mail do administrador |
| notification-sender | `SES_SENDER_EMAIL` | E-mail remetente verificado no SES |
| weekly-report | Todas acima | Banco de dados + SES |

## Segurança

- **RDS** em subnet privada, sem acesso público
- **Security Groups** restringem acesso ao PostgreSQL somente pelas Lambdas
- **IAM Roles** com princípio do menor privilégio (cada Lambda tem apenas as permissões necessárias)
- **Senhas** armazenadas no AWS SSM Parameter Store (SecureString)
- **SES** com e-mails verificados para evitar spam
- **SQS FIFO** com DLQ para mensagens que falham após 3 tentativas
- **X-Ray** habilitado em todas as Lambdas para rastreamento distribuído

## Monitoramento

O **CloudWatch Dashboard** (`feedback-platform-prod-dashboard`) exibe:
- Invocações por Lambda
- Erros por Lambda
- Duração média das execuções
- Mensagens nas filas SQS

**Alarmes configurados:**
- `feedback-api-errors`: mais de 5 erros/min → notifica via SNS/e-mail
- `processor-errors`: mais de 3 erros/min → notifica via SNS/e-mail
- `feedback-dlq-messages`: qualquer mensagem na DLQ → notifica
- `rds-cpu`: CPU do RDS > 80% → notifica

## Estrutura do Projeto

```
feedback-platform/
├── feedback-api/          # Lambda 1: Recebe feedback (API Gateway → SQS)
├── feedback-processor/    # Lambda 2: Processa feedback (SQS → RDS + SQS)
├── notification-sender/   # Lambda 3: Notificação urgente (SQS → SES)
├── weekly-report/         # Lambda 4: Relatório semanal (EventBridge → RDS → SES)
├── infrastructure/
│   ├── terraform/         # IaC: VPC, RDS, SQS, Lambda, API GW, EventBridge
│   ├── database/          # Script SQL de criação da tabela
│   └── localstack/        # Script de inicialização local
├── .github/workflows/     # Pipeline CI/CD (GitHub Actions)
└── docker-compose.yml     # Ambiente de desenvolvimento local
```

Cada módulo Java segue **arquitetura hexagonal**:
```
src/main/java/br/com/fiap/feedback/<modulo>/
├── domain/
│   ├── model/       ← Entidades de domínio puras
│   └── port/
│       ├── in/      ← Interfaces de casos de uso
│       └── out/     ← Interfaces de saída (repositórios, filas, e-mail)
├── application/
│   └── service/     ← Implementação dos casos de uso
└── infrastructure/
    └── adapter/
        ├── in/      ← Handlers Lambda (entrada)
        └── out/     ← Adaptadores: SQS, RDS, SES (saída)
```
