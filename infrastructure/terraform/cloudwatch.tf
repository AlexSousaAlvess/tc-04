resource "aws_sns_topic" "alerts" {
  name = "${local.name_prefix}-alerts"
}

resource "aws_sns_topic_subscription" "admin_email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.admin_email
}

resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "${local.name_prefix}-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type = "metric"
        properties = {
          title  = "Lambda - Invocacoes"
          region = var.aws_region
          period = 300
          stat   = "Sum"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", aws_lambda_function.feedback_api.function_name],
            ["AWS/Lambda", "Invocations", "FunctionName", aws_lambda_function.feedback_processor.function_name],
            ["AWS/Lambda", "Invocations", "FunctionName", aws_lambda_function.notification_sender.function_name],
            ["AWS/Lambda", "Invocations", "FunctionName", aws_lambda_function.weekly_report.function_name]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "Lambda - Erros"
          region = var.aws_region
          period = 300
          stat   = "Sum"
          metrics = [
            ["AWS/Lambda", "Errors", "FunctionName", aws_lambda_function.feedback_api.function_name],
            ["AWS/Lambda", "Errors", "FunctionName", aws_lambda_function.feedback_processor.function_name],
            ["AWS/Lambda", "Errors", "FunctionName", aws_lambda_function.notification_sender.function_name]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "Lambda - Duracao (ms)"
          region = var.aws_region
          period = 300
          stat   = "Average"
          metrics = [
            ["AWS/Lambda", "Duration", "FunctionName", aws_lambda_function.feedback_api.function_name],
            ["AWS/Lambda", "Duration", "FunctionName", aws_lambda_function.feedback_processor.function_name]
          ]
        }
      },
      {
        type = "metric"
        properties = {
          title  = "SQS - Mensagens visiveis"
          region = var.aws_region
          period = 60
          stat   = "Maximum"
          metrics = [
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.feedback.name],
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.notification.name]
          ]
        }
      }
    ]
  })
}

resource "aws_cloudwatch_metric_alarm" "lambda_feedback_api_errors" {
  alarm_name          = "${local.name_prefix}-feedback-api-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 60
  statistic           = "Sum"
  threshold           = 5
  alarm_description   = "Mais de 5 erros por minuto na Lambda feedback-api"
  treat_missing_data  = "notBreaching"

  dimensions = { FunctionName = aws_lambda_function.feedback_api.function_name }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "lambda_processor_errors" {
  alarm_name          = "${local.name_prefix}-processor-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 60
  statistic           = "Sum"
  threshold           = 3
  alarm_description   = "Erros no processamento de feedbacks"
  treat_missing_data  = "notBreaching"

  dimensions = { FunctionName = aws_lambda_function.feedback_processor.function_name }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  alarm_name          = "${local.name_prefix}-rds-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "CPU do RDS acima de 80%"

  dimensions = { DBInstanceIdentifier = aws_db_instance.postgres.identifier }
  alarm_actions = [aws_sns_topic.alerts.arn]
}
