package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.SupabaseConfig
import com.example.data.model.ExchangeRateRecord
import com.example.data.model.LedgerTransaction
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SimulatorDisclaimerBanner(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = WarningAmberBg,
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Info,
        contentDescription = "Simulator Notice",
        tint = WarningAmber,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Closed-loop economy simulator for entertainment. Not real money or deposits.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = TextPrimary,
        lineHeight = 14.sp
      )
    }
  }
}

@Composable
fun BearBalanceCard(
  balance: Long,
  currentRateIdr: Double,
  changePercent: Double,
  onTransferClick: () -> Unit,
  onTopUpClick: () -> Unit,
  onExchangeClick: () -> Unit,
  onBankingClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isBalanceVisible by remember { mutableStateOf(true) }
  val formattedIdr = NumberFormat.getNumberInstance(Locale.US).format(Math.round(balance * currentRateIdr))
  val formattedBalance = NumberFormat.getNumberInstance(Locale.US).format(balance)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("bear_balance_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(ForestGreenPrimary, ForestGreenDark)
          )
        )
        .padding(20.dp)
    ) {
      Column {
        // Card Top Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MintSecondary),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "B",
                fontWeight = FontWeight.Bold,
                color = ForestGreenDark,
                fontSize = 16.sp
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Bear Wallet",
              style = MaterialTheme.typography.titleMedium,
              color = MintLight,
              fontWeight = FontWeight.SemiBold
            )
          }

          IconButton(
            onClick = { isBalanceVisible = !isBalanceVisible },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = "Toggle Balance Visibility",
              tint = SageSurface
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Display
        Text(
          text = "Available Balance",
          style = MaterialTheme.typography.bodySmall,
          color = SageSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = if (isBalanceVisible) formattedBalance else "••••••",
            style = MaterialTheme.typography.displayMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = SupabaseConfig.CURRENCY_CODE,
            style = MaterialTheme.typography.titleMedium,
            color = MintLight,
            modifier = Modifier.padding(bottom = 6.dp)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Reference IDR valuation
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (isBalanceVisible) "≈ Rp $formattedIdr (Simulated)" else "≈ Rp ••••••",
            style = MaterialTheme.typography.bodyMedium,
            color = MintContainer.copy(alpha = 0.9f)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            color = if (changePercent >= 0) PositiveGreenBg.copy(alpha = 0.2f) else NegativeRedBg.copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (changePercent >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = if (changePercent >= 0) MintLight else Color(0xFFFF8B80),
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "${if (changePercent >= 0) "+" else ""}${String.format(Locale.US, "%.1f", changePercent)}%",
                color = if (changePercent >= 0) MintLight else Color(0xFFFF8B80),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action shortcuts row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          ActionPillButton(
            icon = Icons.Default.Send,
            label = "Transfer",
            onClick = onTransferClick,
            modifier = Modifier.testTag("action_transfer")
          )
          ActionPillButton(
            icon = Icons.Default.AddCircleOutline,
            label = "Top-up",
            onClick = onTopUpClick,
            modifier = Modifier.testTag("action_topup")
          )
          ActionPillButton(
            icon = Icons.Default.CurrencyExchange,
            label = "Exchange",
            onClick = onExchangeClick,
            modifier = Modifier.testTag("action_exchange")
          )
          ActionPillButton(
            icon = Icons.Default.AccountBalance,
            label = "Bank Loan",
            onClick = onBankingClick,
            modifier = Modifier.testTag("action_bank")
          )
        }
      }
    }
  }
}

@Composable
fun ActionPillButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = MintLight,
        modifier = Modifier.size(22.dp)
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
      color = SageSurface,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun ExchangeRateChart(
  rates: List<ExchangeRateRecord>,
  modifier: Modifier = Modifier,
  isMini: Boolean = false
) {
  if (rates.isEmpty()) return

  val minRate = (rates.minOfOrNull { it.rateIdr } ?: 10.0) * 0.98
  val maxRate = (rates.maxOfOrNull { it.rateIdr } ?: 10.0) * 1.02
  val rateRange = (maxRate - minRate).coerceAtLeast(0.01)

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(if (isMini) 60.dp else 160.dp)
  ) {
    val width = size.width
    val height = size.height

    if (rates.size < 2) return@Canvas

    val stepX = width / (rates.size - 1)
    val points = rates.mapIndexed { index, item ->
      val x = index * stepX
      val normalized = ((item.rateIdr - minRate) / rateRange).toFloat()
      val y = height - (normalized * height)
      Offset(x, y.coerceIn(4f, height - 4f))
    }

    val strokePath = Path().apply {
      moveTo(points.first().x, points.first().y)
      for (i in 1 until points.size) {
        val p0 = points[i - 1]
        val p1 = points[i]
        // Cubic bezier smoothing
        val control1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
        val control2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
        cubicTo(control1.x, control1.y, control2.x, control2.y, p1.x, p1.y)
      }
    }

    val fillPath = Path().apply {
      addPath(strokePath)
      lineTo(points.last().x, height)
      lineTo(points.first().x, height)
      close()
    }

    // Gradient fill below curve
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          MintSecondary.copy(alpha = if (isMini) 0.25f else 0.35f),
          MintSecondary.copy(alpha = 0.0f)
        )
      )
    )

    // Main line
    drawPath(
      path = strokePath,
      color = MintSecondary,
      style = Stroke(
        width = if (isMini) 2.5f else 3.5f,
        cap = StrokeCap.Round
      )
    )

    // Current latest point circle
    val lastPoint = points.last()
    drawCircle(
      color = ForestGreenPrimary,
      radius = 5.dp.toPx(),
      center = lastPoint
    )
    drawCircle(
      color = MintSecondary,
      radius = 3.dp.toPx(),
      center = lastPoint
    )
  }
}

@Composable
fun TransactionItemRow(
  transaction: LedgerTransaction,
  currentUserId: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isOutgoing = transaction.sourceAccount == currentUserId
  val isSignup = transaction.transactionType == "SIGNUP_BONUS"
  val isTopup = transaction.transactionType == "TOPUP"
  val isBuy = transaction.transactionType == "EXCHANGE_BUY"
  val isSell = transaction.transactionType == "EXCHANGE_SELL"
  val isLoanDisburse = transaction.transactionType == "LOAN_DISBURSEMENT"
  val isLoanRepay = transaction.transactionType == "LOAN_REPAYMENT"

  val isCredit = isSignup || isTopup || isBuy || isLoanDisburse || (!isOutgoing && !isLoanRepay && !isSell)

  val icon = when {
    isSignup -> Icons.Default.CardGiftcard
    isTopup -> Icons.Default.AddCircleOutline
    isBuy || isSell -> Icons.Default.CurrencyExchange
    isLoanDisburse -> Icons.Default.AccountBalance
    isLoanRepay -> Icons.Default.Payment
    isOutgoing -> Icons.Default.ArrowOutward
    else -> Icons.Default.ArrowDownward
  }

  val title = when {
    isSignup -> "Welcome Distribution Grant"
    isTopup -> "Community Faucet Top-up"
    isBuy -> "Exchange: Buy Bear"
    isSell -> "Exchange: Sell Bear"
    isLoanDisburse -> "Loan Disbursed"
    isLoanRepay -> "Loan Repayment"
    isOutgoing -> "Transfer Sent"
    else -> "Transfer Received"
  }

  val dateStr = remember(transaction.createdAt) {
    SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(transaction.createdAt))
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = PureWhite
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(if (isCredit) PositiveGreenBg else SurfaceVariantColor),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isCredit) PositiveGreen else ForestGreenPrimary,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        )
        Text(
          text = if (transaction.note.isNotEmpty()) transaction.note else dateStr,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          maxLines = 1
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${if (isCredit) "+" else "-"}${transaction.amount} B",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = if (isCredit) PositiveGreen else TextPrimary
        )
        Text(
          text = dateStr,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = TextTertiary
        )
      }
    }
  }
}

@Composable
fun FourStageStepper(
  currentStage: Int,
  stageStatus: String,
  modifier: Modifier = Modifier
) {
  val stageTitles = listOf(
    "1. Eligibility Screening",
    "2. Credit Score Analysis",
    "3. Bank Liquidity Check",
    "4. Decision & Offer"
  )

  Column(modifier = modifier.fillMaxWidth()) {
    stageTitles.forEachIndexed { index, title ->
      val stageNumber = index + 1
      val isPast = stageNumber < currentStage || (stageNumber == 4 && stageStatus != "IN_PROGRESS")
      val isCurrent = stageNumber == currentStage && stageStatus == "IN_PROGRESS"
      val isFailed = stageNumber == 4 && stageStatus == "REJECTED"

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
              when {
                isFailed -> NegativeRed
                isPast -> PositiveGreen
                isCurrent -> MintSecondary
                else -> SurfaceVariantColor
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          if (isCurrent) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = ForestGreenDark,
              strokeWidth = 2.dp
            )
          } else if (isPast) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Completed",
              tint = PureWhite,
              modifier = Modifier.size(18.dp)
            )
          } else if (isFailed) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Rejected",
              tint = PureWhite,
              modifier = Modifier.size(18.dp)
            )
          } else {
            Text(
              text = "$stageNumber",
              style = MaterialTheme.typography.labelMedium,
              color = TextTertiary,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrent) ForestGreenPrimary else if (isPast) TextPrimary else TextTertiary
          )
          val subtitle = when {
            isCurrent -> "Verifying authoritative backend records..."
            isPast -> "Completed"
            isFailed -> "Liquidity constraint encountered"
            else -> "Pending"
          }
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = if (isCurrent) MintSecondary else TextTertiary
          )
        }
      }
    }
  }
}

@Composable
fun ConfirmRow(label: String, value: String, isHighlight: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
      color = if (isHighlight) ForestGreenPrimary else TextPrimary
    )
  }
}

