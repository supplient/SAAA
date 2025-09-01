package com.example.strategicassetallocationassistant.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// From AShare(https://github.com/mpquant/Ashare)

// region Data Models
@Serializable
data class StockData(
    val time: String,
    val open: Float,
    val close: Float,
    val high: Float,
    val low: Float,
    val volume: Float
)

// 聚合市场数据
data class MarketStats(
    val latestClose: Double,
    val sevenDayReturn: Double?,
    val annualVolatility: Double?
)

@Serializable
data class SinaStockData(
    val day: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String
)

@Serializable
data class TxDayResponse(
    val data: Map<String, TxDayDataContainer>
)

@Serializable
data class TxDayDataContainer(
    @SerialName("qfqday") val qfqDay: List<List<String>>? = null,
    val day: List<List<String>>? = null,
    @SerialName("qfqweek") val qfqWeek: List<List<String>>? = null,
    val week: List<List<String>>? = null,
    @SerialName("qfqmonth") val qfqMonth: List<List<String>>? = null,
    val month: List<List<String>>? = null
)

// endregion

// region Retrofit Service
interface SinaApiService {
    @GET
    suspend fun getSinaPrice(@Url url: String): List<SinaStockData>
}

interface TencentApiService {
    @GET("appstock/app/fqkline/get")
    suspend fun getTxDayPrice(@Query("param") param: String): TxDayResponse

    @GET("appstock/app/kline/mkline")
    suspend fun getTxMinPrice(@Query("param") param: String): ResponseBody
}
// endregion

object AShare {
    private const val SINA_BASE_URL = "http://money.finance.sina.com.cn/"
    private const val TX_DAY_BASE_URL = "http://web.ifzq.gtimg.cn/"
    private const val TX_MIN_BASE_URL = "http://ifzq.gtimg.cn/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder().build()

    private fun <T> createRetrofitService(baseUrl: String, service: Class<T>): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(okHttpClient)
            .build()
            .create(service)
    }

    private val sinaService: SinaApiService = createRetrofitService(SINA_BASE_URL, SinaApiService::class.java)
    private val txDayService: TencentApiService = createRetrofitService(TX_DAY_BASE_URL, TencentApiService::class.java)
    private val txMinService: TencentApiService = createRetrofitService(TX_MIN_BASE_URL, TencentApiService::class.java)

    suspend fun getPrice(
        code: String,
        endDate: String = "",
        count: Int = 10,
        frequency: String = "1d"
    ): List<StockData> {
        val xcode = code.replace(".XSHG", "").replace(".XSHE", "")
        val formattedCode = if (code.contains("XSHG")) "sh$xcode" else if (code.contains("XSHE")) "sz$xcode" else code

        return when (frequency) {
            in listOf("1d", "1w", "1M") -> {
                try {
                    getPriceSina(formattedCode, endDate, count, frequency)
                } catch (e: Exception) {
                    getPriceDayTx(formattedCode, endDate, count, frequency)
                }
            }
            in listOf("1m", "5m", "15m", "30m", "60m") -> {
                if (frequency == "1m") {
                    return getPriceMinTx(formattedCode, count, frequency)
                }
                try {
                    getPriceSina(formattedCode, endDate, count, frequency)
                } catch (e: Exception) {
                    getPriceMinTx(formattedCode, count, frequency)
                }
            }
            else -> emptyList()
        }
    }

    /**
     * Fetch one set of daily price data (最多 90 日) 并一次性计算：
     *  - 最新收盘价
     *  - 七日涨跌幅
     *  - 90 日年化波动率
     */
    suspend fun getMarketStats(code: String): MarketStats? {
        // 拉取 90 个交易日的收盘价（含今天）
        val prices = getPrice(code = code, count = 90, frequency = "1d")
        if (prices.isEmpty()) return null

        val latestClose = prices.last().close.toDouble()

        // --- 七日涨跌幅 ---
        val sevenDayReturn: Double? = if (prices.size >= 8) {
            val close7 = prices[prices.size - 8].close.toDouble()
            if (close7 == 0.0) null else (latestClose - close7) / close7
        } else null

        // --- 90 日年化波动率 ---
        val annualVol: Double? = if (prices.size >= 90) {
            val deltas = prices.takeLast(90).windowed(2) { (p1, p2) -> kotlin.math.ln(p1.close / p2.close) }
            if (deltas.isEmpty()) null else {
                val mean = deltas.average()
                val variance = deltas.sumOf { (it - mean) * (it - mean) } / deltas.size
                kotlin.math.sqrt(variance) * 15.87
            }
        } else null

        return MarketStats(latestClose, sevenDayReturn, annualVol)
    }


    private suspend fun getPriceDayTx(
        code: String,
        endDate: String,
        count: Int,
        frequency: String
    ): List<StockData> {
        val unit = when (frequency) {
            "1w" -> "week"
            "1M" -> "month"
            else -> "day"
        }
        val end = if (endDate.isNotEmpty() && endDate != LocalDate.now().toString()) endDate else ""
        val param = "$code,$unit,,$end,$count,qfq"
        val response = txDayService.getTxDayPrice(param)
        val dataContainer = response.data[code] ?: return emptyList()

        val series = when (unit) {
            "day" -> dataContainer.qfqDay ?: dataContainer.day
            "week" -> dataContainer.qfqWeek ?: dataContainer.week
            "month" -> dataContainer.qfqMonth ?: dataContainer.month
            else -> null
        }

        return series?.map {
            StockData(
                time = it[0],
                open = it[1].toFloat(),
                close = it[2].toFloat(),
                high = it[3].toFloat(),
                low = it[4].toFloat(),
                volume = it[5].toFloat()
            )
        } ?: emptyList()
    }

    private suspend fun getPriceMinTx(
        code: String,
        count: Int,
        frequency: String
    ): List<StockData> {
        val ts = frequency.removeSuffix("m").toIntOrNull() ?: 1
        val param = "$code,m$ts,,$count"
        val responseBody = txMinService.getTxMinPrice(param)
        val responseString = responseBody.string()

        val jsonObject = Json.parseToJsonElement(responseString).jsonObject
        val data = jsonObject["data"]?.jsonObject?.get(code)?.jsonObject
        val seriesKey = "m$ts"
        val series = data?.get(seriesKey)?.jsonArray
        val qt = data?.get("qt")?.jsonObject?.get(code)?.jsonArray

        val stockDataList = series?.map {
            val item = it.jsonArray
            StockData(
                time = formatTxMinTime(item[0].jsonPrimitive.content),
                open = item[1].jsonPrimitive.content.toFloat(),
                close = item[2].jsonPrimitive.content.toFloat(),
                high = item[3].jsonPrimitive.content.toFloat(),
                low = item[4].jsonPrimitive.content.toFloat(),
                volume = item[5].jsonPrimitive.content.toFloat()
            )
        }?.toMutableList()

        if (stockDataList != null && stockDataList.isNotEmpty() && qt != null && qt.size > 3) {
            val lastClose = qt[3].jsonPrimitive.content.toFloat()
            val lastElement = stockDataList.last()
            stockDataList[stockDataList.size - 1] = lastElement.copy(close = lastClose)
        }

        return stockDataList ?: emptyList()
    }

    private fun formatTxMinTime(txTime: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            LocalDateTime.parse(txTime, formatter).toString()
        } catch (e: Exception) {
            txTime
        }
    }


    private suspend fun getPriceSina(
        code: String,
        endDate: String,
        count: Int,
        frequency: String
    ): List<StockData> {
        val freq = frequency.replace("1d", "240m").replace("1w", "1200m").replace("1M", "7200m")
        val ts = freq.removeSuffix("m").toIntOrNull() ?: 1
        var dataLen = count
        val isDaily = frequency in listOf("1d", "1w", "1M")

        if (endDate.isNotEmpty() && isDaily) {
            try {
                val end = LocalDate.parse(endDate)
                val now = LocalDate.now()
                val days = java.time.temporal.ChronoUnit.DAYS.between(end, now)
                val unit = when (frequency) {
                    "1w" -> 7
                    "1M" -> 30
                    else -> 1
                }
                dataLen = count + (days / unit).toInt().coerceAtLeast(0)
            } catch (_: Exception) {
                // Invalid date format, proceed with default count
            }
        }

        val url = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=$code&scale=$ts&ma=5&datalen=$dataLen"
        val response = sinaService.getSinaPrice(url)

        val mapped = response.map {
            StockData(
                time = it.day,
                open = it.open.toFloat(),
                close = it.close.toFloat(),
                high = it.high.toFloat(),
                low = it.low.toFloat(),
                volume = it.volume.toFloat()
            )
        }

        if (endDate.isNotEmpty() && isDaily) {
            try {
                val end = LocalDate.parse(endDate)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                return mapped.filter {
                    try {
                        val date = LocalDate.parse(it.time, formatter)
                        !date.isAfter(end)
                    } catch (e: Exception) {
                        false
                    }
                }.takeLast(count)
            } catch (_: Exception) {
                // Invalid date format in endDate
            }
        }

        return mapped
    }
}
