locals {
  lambda_env_db = {
    DB_HOST                = aws_db_instance.postgres.address
    DB_PORT                = "5432"
    DB_NAME                = var.db_name
    DB_USER                = var.db_username
    DB_PASSWORD            = var.db_password
    NOTIFICATION_QUEUE_URL = aws_sqs_queue.notification.url
  }
}

resource "aws_lambda_function" "feedback_api" {
  function_name = "${local.name_prefix}-feedback-api"
  role          = aws_iam_role.lambda_base.arn
  handler       = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime       = "java21"
  memory_size   = var.lambda_memory_mb
  timeout       = var.lambda_timeout_seconds
  filename      = "${path.module}/../../feedback-api/target/function.zip"

  environment {
    variables = {
      FEEDBACK_QUEUE_URL     = aws_sqs_queue.feedback.url
      QUARKUS_LAMBDA_HANDLER = "feedback-handler"
    }
  }

  tracing_config { mode = "Active" }

  tags = { Name = "${local.name_prefix}-feedback-api" }
}

resource "aws_lambda_function" "feedback_processor" {
  function_name = "${local.name_prefix}-feedback-processor"
  role          = aws_iam_role.lambda_base.arn
  handler       = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime       = "java21"
  memory_size   = var.lambda_memory_mb
  timeout       = var.lambda_timeout_seconds
  filename      = "${path.module}/../../feedback-processor/target/function.zip"

  vpc_config {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda.id]
  }

  environment {
    variables = merge(local.lambda_env_db, {
      QUARKUS_LAMBDA_HANDLER = "processor-handler"
    })
  }

  tracing_config { mode = "Active" }

  tags = { Name = "${local.name_prefix}-feedback-processor" }
}

resource "aws_lambda_function" "notification_sender" {
  function_name = "${local.name_prefix}-notification-sender"
  role          = aws_iam_role.lambda_base.arn
  handler       = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime       = "java21"
  memory_size   = var.lambda_memory_mb
  timeout       = var.lambda_timeout_seconds
  filename      = "${path.module}/../../notification-sender/target/function.zip"

  environment {
    variables = {
      ADMIN_EMAIL            = var.admin_email
      SES_SENDER_EMAIL       = var.ses_sender_email
      QUARKUS_LAMBDA_HANDLER = "notification-handler"
    }
  }

  tracing_config { mode = "Active" }

  tags = { Name = "${local.name_prefix}-notification-sender" }
}

resource "aws_lambda_function" "weekly_report" {
  function_name = "${local.name_prefix}-weekly-report"
  role          = aws_iam_role.lambda_base.arn
  handler       = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime       = "java21"
  memory_size   = var.lambda_memory_mb
  timeout       = 60
  filename      = "${path.module}/../../weekly-report/target/function.zip"

  vpc_config {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda.id]
  }

  environment {
    variables = merge(local.lambda_env_db, {
      ADMIN_EMAIL            = var.admin_email
      SES_SENDER_EMAIL       = var.ses_sender_email
      QUARKUS_LAMBDA_HANDLER = "report-handler"
    })
  }

  tracing_config { mode = "Active" }

  tags = { Name = "${local.name_prefix}-weekly-report" }
}

resource "aws_lambda_event_source_mapping" "sqs_feedback" {
  event_source_arn                   = aws_sqs_queue.feedback.arn
  function_name                      = aws_lambda_function.feedback_processor.arn
  batch_size                         = 10
  function_response_types            = ["ReportBatchItemFailures"]
}

resource "aws_lambda_event_source_mapping" "sqs_notification" {
  event_source_arn                   = aws_sqs_queue.notification.arn
  function_name                      = aws_lambda_function.notification_sender.arn
  batch_size                         = 5
  function_response_types            = ["ReportBatchItemFailures"]
}

resource "aws_cloudwatch_log_group" "feedback_api" {
  name              = "/aws/lambda/${aws_lambda_function.feedback_api.function_name}"
  retention_in_days = 14
}

resource "aws_cloudwatch_log_group" "feedback_processor" {
  name              = "/aws/lambda/${aws_lambda_function.feedback_processor.function_name}"
  retention_in_days = 14
}

resource "aws_cloudwatch_log_group" "notification_sender" {
  name              = "/aws/lambda/${aws_lambda_function.notification_sender.function_name}"
  retention_in_days = 14
}

resource "aws_cloudwatch_log_group" "weekly_report" {
  name              = "/aws/lambda/${aws_lambda_function.weekly_report.function_name}"
  retention_in_days = 14
}
