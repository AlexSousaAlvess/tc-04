output "api_gateway_url" {
  description = "URL base da API Gateway"
  value       = "${aws_apigatewayv2_stage.prod.invoke_url}/avaliacao"
}

output "feedback_queue_url" {
  description = "URL da fila SQS de feedback"
  value       = aws_sqs_queue.feedback.url
}

output "notification_queue_url" {
  description = "URL da fila SQS de notificações"
  value       = aws_sqs_queue.notification.url
}

output "rds_endpoint" {
  description = "Endpoint do PostgreSQL (RDS)"
  value       = aws_db_instance.postgres.address
  sensitive   = true
}

output "dashboard_url" {
  description = "URL do CloudWatch Dashboard"
  value       = "https://console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#dashboards:name=${aws_cloudwatch_dashboard.main.dashboard_name}"
}
