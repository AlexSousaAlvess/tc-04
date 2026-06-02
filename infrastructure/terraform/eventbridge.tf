resource "aws_scheduler_schedule" "weekly_report" {
  name                         = "${local.name_prefix}-weekly-report"
  description                  = "Dispara o relatório semanal toda segunda-feira às 08:00 (horário de Brasília = 11:00 UTC)"
  schedule_expression          = "cron(0 11 ? * MON *)"
  schedule_expression_timezone = "America/Sao_Paulo"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_lambda_function.weekly_report.arn
    role_arn = aws_iam_role.eventbridge_scheduler.arn

    input = jsonencode({
      source = "aws.scheduler"
      detail = { trigger = "weekly-report" }
    })

    retry_policy {
      maximum_retry_attempts = 2
    }
  }
}

resource "aws_iam_role" "eventbridge_scheduler" {
  name = "${local.name_prefix}-eventbridge-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "scheduler.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "eventbridge_invoke_lambda" {
  name = "${local.name_prefix}-eventbridge-invoke-lambda"
  role = aws_iam_role.eventbridge_scheduler.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = "lambda:InvokeFunction"
      Resource = aws_lambda_function.weekly_report.arn
    }]
  })
}

resource "aws_lambda_permission" "eventbridge_weekly_report" {
  statement_id  = "AllowEventBridgeScheduler"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.weekly_report.function_name
  principal     = "scheduler.amazonaws.com"
  source_arn    = aws_scheduler_schedule.weekly_report.arn
}
