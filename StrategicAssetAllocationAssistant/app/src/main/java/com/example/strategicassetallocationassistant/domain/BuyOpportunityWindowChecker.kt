package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.data.network.AShare
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 根据日期及上一次检查时间，判断是否应当触发一次买入机会窗口。
 */
class BuyOpportunityWindowChecker @Inject constructor(
    private val repository: PortfolioRepository
) {

    /** 默认使用上证指数代码用于判断交易日 */
    private val tradingCalendarCode = "sh000001"

    /**
     * 判断今天是否是中国 A 股交易日。
     */
    private suspend fun isTradingDay(date: LocalDate): Boolean = withContext(Dispatchers.IO) {
        try {
            val list = AShare.getPrice(tradingCalendarCode, endDate = date.toString(), count = 1, frequency = "1d")
            if (list.isEmpty()) return@withContext false
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val recordDate = try {
                LocalDate.parse(list.last().time.substring(0, 10), formatter)
            } catch (_: Exception) {
                return@withContext false
            }
            recordDate == date
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 判断是否应触发买入机会。
     * 规则：
     * 1. 若本周三是交易日，则周三 14:00 后触发；
     * 2. 若周三不是交易日，则顺延至之后第一个交易日的 14:00；
     */
    suspend fun shouldTrigger(now: LocalDateTime, lastCheck: LocalDateTime?): Boolean {
        // 14:00 之后才可能触发
        if (now.toLocalTime().isBefore(LocalTime.of(14, 0))) return false

        var postponed = repository.isBuyOpportunityPostponed()

        if (postponed) {
            // 顺延阶段：今天是交易日即可触发
            if (!isTradingDay(now.toLocalDate())) return false

            // 触发后复位并允许（不考虑同周限制）
            repository.setBuyOpportunityPostponed(false)
            return true
        }

        // 未顺延：检查当前时间与 lastCheck 之间是否跨越了“本周三14:00”这一分界
        val wf = WeekFields.of(Locale.getDefault())
        val wedDate = now.toLocalDate().with(wf.dayOfWeek(), 3) // 3 = Wednesday
        val wed14 = LocalDateTime.of(wedDate, LocalTime.of(14, 0))

        // 若 Wednesday 在未来，则说明本周三尚未到，不触发
        if (now.isBefore(wed14)) return false

        // 若周三不是交易日，则进入顺延流程
        if (!isTradingDay(wedDate)) {
            repository.setBuyOpportunityPostponed(true)
            return false
        }

        // 若 lastCheck 在 wed14 之后，说明已经触发过
        if (lastCheck != null && !lastCheck.isBefore(wed14)) return false

        val ok = true
        if (ok) {
            // 触发后复位顺延标记
            repository.setBuyOpportunityPostponed(false)
        }
        return ok
    }
}
