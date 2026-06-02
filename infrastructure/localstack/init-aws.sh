#!/bin/bash
set -e

echo "Inicializando recursos AWS locais via LocalStack..."

awslocal sqs create-queue \
  --queue-name feedback-queue.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true

awslocal sqs create-queue \
  --queue-name notification-queue.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true

awslocal ses verify-email-identity \
  --email-address noreply@fiap.com.br

awslocal ses verify-email-identity \
  --email-address admin@fiap.com.br

echo "Recursos LocalStack criados com sucesso!"
echo "  - SQS: feedback-queue.fifo"
echo "  - SQS: notification-queue.fifo"
echo "  - SES: noreply@fiap.com.br (verificado)"
echo "  - SES: admin@fiap.com.br (verificado)"
