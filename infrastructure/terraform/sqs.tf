resource "aws_sqs_queue" "feedback_dlq" {
  name                      = "${local.name_prefix}-feedback-dlq.fifo"
  fifo_queue                = true
  content_based_deduplication = true
  message_retention_seconds = 1209600

  tags = { Name = "${local.name_prefix}-feedback-dlq" }
}

resource "aws_sqs_queue" "feedback" {
  name                        = "${local.name_prefix}-feedback-queue.fifo"
  fifo_queue                  = true
  content_based_deduplication = true
  visibility_timeout_seconds  = 60
  message_retention_seconds   = 86400

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.feedback_dlq.arn
    maxReceiveCount     = 3
  })

  tags = { Name = "${local.name_prefix}-feedback-queue" }
}

resource "aws_sqs_queue" "notification_dlq" {
  name                      = "${local.name_prefix}-notification-dlq.fifo"
  fifo_queue                = true
  content_based_deduplication = true
  message_retention_seconds = 1209600

  tags = { Name = "${local.name_prefix}-notification-dlq" }
}

resource "aws_sqs_queue" "notification" {
  name                        = "${local.name_prefix}-notification-queue.fifo"
  fifo_queue                  = true
  content_based_deduplication = true
  visibility_timeout_seconds  = 60
  message_retention_seconds   = 86400

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.notification_dlq.arn
    maxReceiveCount     = 3
  })

  tags = { Name = "${local.name_prefix}-notification-queue" }
}

resource "aws_cloudwatch_metric_alarm" "feedback_dlq_alarm" {
  alarm_name          = "${local.name_prefix}-feedback-dlq-messages"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "Mensagens na DLQ da fila de feedback"

  dimensions = {
    QueueName = aws_sqs_queue.feedback_dlq.name
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
}
