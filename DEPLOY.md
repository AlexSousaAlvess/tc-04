# Guia de Deploy — Plataforma de Feedback

Passo a passo completo para provisionar a plataforma na AWS.

> **Atenção — a ordem importa:**
> - O Terraform (Passo 8) precisa dos ZIPs compilados (Passo 5) para fazer upload das Lambdas.
> - O SES (Passo 4) precisa ter os e-mails verificados **antes** de qualquer envio.

---

## Sumário

1. [Pré-requisitos](#1-pré-requisitos)
2. [Criar usuário IAM para deploy](#2-criar-usuário-iam-para-deploy)
3. [Configurar AWS CLI](#3-configurar-aws-cli)
4. [Verificar e-mails no SES](#4-verificar-e-mails-no-ses)
5. [Build do projeto (Maven)](#5-build-do-projeto-maven)
6. [Criar bucket S3 para state do Terraform](#6-criar-bucket-s3-para-state-do-terraform)
7. [Configurar variáveis do Terraform](#7-configurar-variáveis-do-terraform)
8. [Provisionar infraestrutura com Terraform](#8-provisionar-infraestrutura-com-terraform)
9. [Inicializar o banco de dados](#9-inicializar-o-banco-de-dados)
10. [Configurar GitHub Actions (CI/CD)](#10-configurar-github-actions-cicd)
11. [Testar a aplicação](#11-testar-a-aplicação)
12. [Verificar monitoramento](#12-verificar-monitoramento)
13. [Destruir a infraestrutura](#13-destruir-a-infraestrutura)

---

## 1. Pré-requisitos

| Ferramenta | Versão mínima | Verificar |
|---|---|---|
| Java (JDK) | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| AWS CLI | v2 | `aws --version` |
| Terraform | 1.6+ | `terraform -version` |
| Git | qualquer | `git --version` |
| psql | qualquer | `psql --version` |

---

## 2. Criar usuário IAM para deploy

```bash
# Criar usuário
aws iam create-user --user-name feedback-platform-deploy

# Anexar política de administrador
aws iam attach-user-policy \
  --user-name feedback-platform-deploy \
  --policy-arn arn:aws:iam::aws:policy/AdministratorAccess

# Gerar Access Key
aws iam create-access-key --user-name feedback-platform-deploy
```

Salve o `AccessKeyId` e o `SecretAccessKey` — o Secret não aparece de novo.

---

## 3. Configurar AWS CLI

```bash
aws configure
# AWS Access Key ID: AKIA...
# AWS Secret Access Key: ...
# Default region: us-east-1
# Default output: json

# Verificar conexão
aws sts get-caller-identity
```

---

## 4. Verificar e-mails no SES

```bash
aws ses verify-email-identity --email-address noreply@seudominio.com --region us-east-1
aws ses verify-email-identity --email-address admin@seudominio.com --region us-east-1
```

Acesse a caixa de entrada dos dois e-mails e clique no link de confirmação da AWS.

```bash
# Confirmar verificação
aws sesv2 list-email-identities --region us-east-1 \
  --query "EmailIdentities[*].{Email:IdentityName,Status:VerificationStatus}" --output table
```

> **Sandbox:** em modo sandbox só é possível enviar para e-mails verificados. Para produção real, solicite a saída do sandbox no Console AWS → SES → Account dashboard.

---

## 5. Build do projeto (Maven)

```bash
git clone https://github.com/AlexSousaAlvess/tc-04.git
cd tc-04

mvn clean package -DskipTests --batch-mode

# Verificar os ZIPs gerados (entre 10MB e 40MB cada)
ls -lh feedback-api/target/function.zip \
        feedback-processor/target/function.zip \
        notification-sender/target/function.zip \
        weekly-report/target/function.zip
```

---

## 6. Criar bucket S3 para state do Terraform

```bash
aws s3 mb s3://feedback-platform-tfstate-rm367425 --region us-east-1

aws s3api put-bucket-versioning \
  --bucket feedback-platform-tfstate-rm367425 \
  --versioning-configuration Status=Enabled

aws s3api put-public-access-block \
  --bucket feedback-platform-tfstate-rm367425 \
  --public-access-block-configuration \
  BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
```

> Se o nome já existir, adicione um sufixo e atualize o `bucket` em `infrastructure/terraform/main.tf`.

---

## 7. Configurar variáveis do Terraform

```bash
cd infrastructure/terraform
cp terraform.tfvars.example terraform.tfvars
```

Edite `terraform.tfvars`:

```hcl
aws_region       = "us-east-1"
project_name     = "feedback-platform"
environment      = "prod"
db_name          = "feedbackdb"
db_username      = "feedbackuser"
db_password      = "SuaSenhaSegura123!"   # sem / @ " ou espaço
admin_email      = "admin@seudominio.com"
ses_sender_email = "noreply@seudominio.com"
```

> `terraform.tfvars` está no `.gitignore` — nunca commite este arquivo.

---

## 8. Provisionar infraestrutura com Terraform

```bash
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

Tempo estimado: **10–15 minutos** (RDS é o mais demorado).

Ao final, anote os outputs:

```bash
terraform output api_gateway_url   # URL para POST /avaliacao
terraform output dashboard_url     # CloudWatch Dashboard
terraform output rds_endpoint      # Endpoint do RDS (sensível)
```

---

## 9. Inicializar o banco de dados

O RDS está em subnet privada. Use um EC2 temporário como bastion:

```bash
# Obter IDs da infraestrutura
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=tag:Project,Values=feedback-platform" \
  --query "Vpcs[0].VpcId" --output text)

SUBNET_ID=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=*public*" \
  --query "Subnets[0].SubnetId" --output text)

# Criar key pair e security group temporários
aws ec2 create-key-pair --key-name bastion-temp \
  --query "KeyMaterial" --output text > /tmp/bastion-temp.pem
chmod 400 /tmp/bastion-temp.pem

SG_BASTION=$(aws ec2 create-security-group \
  --group-name bastion-temp-sg \
  --description "Bastion temporario" \
  --vpc-id $VPC_ID \
  --query "GroupId" --output text)

aws ec2 authorize-security-group-ingress \
  --group-id $SG_BASTION \
  --protocol tcp --port 22 --cidr $(curl -s https://checkip.amazonaws.com)/32

# Lançar EC2
INSTANCE_ID=$(aws ec2 run-instances \
  --image-id ami-0c02fb55956c7d316 \
  --instance-type t2.micro \
  --key-name bastion-temp \
  --subnet-id $SUBNET_ID \
  --security-group-ids $SG_BASTION \
  --associate-public-ip-address \
  --query "Instances[0].InstanceId" --output text)

aws ec2 wait instance-running --instance-ids $INSTANCE_ID

BASTION_IP=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID \
  --query "Reservations[0].Instances[0].PublicIpAddress" --output text)

# Liberar bastion no security group do RDS
RDS_SG=$(aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=feedback-platform-prod-rds-sg" \
  --query "SecurityGroups[0].GroupId" --output text)

aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG --protocol tcp --port 5432 --source-group $SG_BASTION

DB_HOST=$(cd infrastructure/terraform && terraform output -raw rds_endpoint)

# Instalar psql e executar o script
scp -i /tmp/bastion-temp.pem -o StrictHostKeyChecking=no \
  infrastructure/database/V1__create_feedbacks_table.sql \
  ec2-user@$BASTION_IP:/tmp/

ssh -i /tmp/bastion-temp.pem -o StrictHostKeyChecking=no ec2-user@$BASTION_IP \
  "sudo amazon-linux-extras enable postgresql14 -y && sudo yum install -y postgresql && \
   PGPASSWORD='SuaSenha' psql -h $DB_HOST -U feedbackuser -d feedbackdb \
   -f /tmp/V1__create_feedbacks_table.sql"

# Verificar
ssh -i /tmp/bastion-temp.pem -o StrictHostKeyChecking=no ec2-user@$BASTION_IP \
  "PGPASSWORD='SuaSenha' psql -h $DB_HOST -U feedbackuser -d feedbackdb -c 'SELECT COUNT(*) FROM feedbacks;'"

# Limpar bastion
aws ec2 revoke-security-group-ingress \
  --group-id $RDS_SG --protocol tcp --port 5432 --source-group $SG_BASTION
aws ec2 terminate-instances --instance-ids $INSTANCE_ID
aws ec2 wait instance-terminated --instance-ids $INSTANCE_ID
aws ec2 delete-security-group --group-id $SG_BASTION
aws ec2 delete-key-pair --key-name bastion-temp
rm /tmp/bastion-temp.pem
```

---

## 10. Configurar GitHub Actions (CI/CD)

Acesse o repositório no GitHub → **Settings** → **Secrets and variables** → **Actions**:

| Secret | Valor |
|---|---|
| `AWS_ACCESS_KEY_ID` | Access Key ID do usuário de deploy |
| `AWS_SECRET_ACCESS_KEY` | Secret Access Key do usuário de deploy |

Faça push para `main` — o pipeline executa automaticamente build, testes e deploy das 4 Lambdas.

---

## 11. Testar a aplicação

```bash
API_URL="https://SEU_ID.execute-api.us-east-1.amazonaws.com/prod/avaliacao"

# Feedback BAIXA (nota 7-10)
curl -s -w "\nHTTP %{http_code}\n" -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{"descricao": "Aula excelente, conteudo bem estruturado", "nota": 9}'

# Feedback CRITICA (nota 0-3) — dispara e-mail em até 30s
curl -s -w "\nHTTP %{http_code}\n" -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{"descricao": "Aula muito confusa, sem exemplos praticos", "nota": 1}'

# Nota invalida — deve retornar 400
curl -s -w "\nHTTP %{http_code}\n" -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{"descricao": "Teste", "nota": 11}'

# Disparar relatório semanal manualmente
aws lambda invoke \
  --function-name feedback-platform-prod-weekly-report \
  --payload '{}' --cli-binary-format raw-in-base64-out response.json
```

---

## 12. Verificar monitoramento

```bash
# Logs em tempo real de uma Lambda
aws logs tail /aws/lambda/feedback-platform-prod-feedback-api --follow --since 10m

# Verificar alarmes
aws cloudwatch describe-alarms \
  --alarm-name-prefix "feedback-platform" \
  --query "MetricAlarms[*].[AlarmName,StateValue]" --output table
```

Dashboard: **Console AWS → CloudWatch → Dashboards → feedback-platform-prod-dashboard**

X-Ray: **Console AWS → X-Ray → Traces**

---

## 13. Destruir a infraestrutura

> **Atenção:** apaga tudo — banco, filas, Lambdas, VPC.

```bash
cd infrastructure/terraform
terraform destroy

# Remover bucket de state (opcional)
aws s3 rm s3://feedback-platform-tfstate-rm367425 --recursive
aws s3 rb s3://feedback-platform-tfstate-rm367425
```

---

## Checklist resumido

| # | Passo | Tempo estimado | Feito? |
|---|---|---|---|
| 1 | Instalar pré-requisitos | 15–30 min | [ ] |
| 2 | Criar usuário IAM | 5 min | [ ] |
| 3 | Configurar AWS CLI | 2 min | [ ] |
| 4 | Verificar e-mails no SES | 5 min | [ ] |
| 5 | Build Maven | 5–10 min | [ ] |
| 6 | Criar bucket S3 | 2 min | [ ] |
| 7 | Configurar terraform.tfvars | 3 min | [ ] |
| 8 | Terraform apply | 10–15 min | [ ] |
| 9 | Inicializar banco | 10–15 min | [ ] |
| 10 | Configurar GitHub Actions | 5 min | [ ] |
| 11 | Testar a aplicação | 5 min | [ ] |
| 12 | Verificar monitoramento | 5 min | [ ] |

**Tempo total estimado: 70–100 minutos**
