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
import android.util.Log
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
        // 获取最新收盘价（使用分钟级数据）
        val latestPrice = getPrice(code = code, count = 1, frequency = "1m")
        if (latestPrice.isEmpty()) {
            Log.e("AShare", "getMarketStats: latest price is empty for code: $code")
            return null
        }
        val latestClose = latestPrice.last().close.toDouble()

        // 获取昨天的日期作为 endDate，确保获取的是之前交易日的数据
        val yesterday = LocalDate.now().minusDays(1)
        val yesterdayStr = yesterday.toString()

        // 拉取 90 个交易日的历史收盘价（不包含今天）
        val historicalPrices = getPrice(code = code, endDate = yesterdayStr, count = 90, frequency = "1d")
        if (historicalPrices.isEmpty()) {
            Log.e("AShare", "getMarketStats: historical prices is empty for code: $code")
            return null
        }

        // --- 七日涨跌幅 ---
        val sevenDayReturn: Double? = if (historicalPrices.isNotEmpty()) {
            val referenceClose: Double
            if (historicalPrices.size >= 7) {
                referenceClose = historicalPrices[historicalPrices.size - 7].close.toDouble()
            } else {
                // 使用最早交易日的收盘价进行计算
                referenceClose = historicalPrices.first().close.toDouble()
                Log.i(
                    "AShare",
                    "getMarketStats: historicalPrices.size < 7 for code: $code, using data ${historicalPrices.size - 1} days ago to calculate sevenDayReturn"
                )
            }

            if (referenceClose == 0.0) null else (latestClose - referenceClose) / referenceClose
        } else {
            null
        }

        // --- 90 日年化波动率（Yang–Zhang） ---
        val effectiveList = if (historicalPrices.size >= 90) historicalPrices.takeLast(90) else {
            Log.i(
                "AShare",
                "getMarketStats: historicalPrices.size < 90 for code: $code, using available ${historicalPrices.size} records to calculate annualVol"
            )
            historicalPrices
        }
        val annualVol: Double? = calculateYangZhangAnnualVolatility(effectiveList) ?: run {
            Log.i(
                "AShare",
                "getMarketStats: insufficient or invalid OHLC data for Yang–Zhang volatility for code: $code"
            )
            null
        }

        return MarketStats(latestClose, sevenDayReturn, annualVol)
    }


    /**
     * 计算给定日K线数据的杨-张（Yang-Zhang）年化波动率。
     *
     * 杨-张波动率是一种结合开盘价、最高价、最低价和收盘价的波动率估计方法，
     * 能够有效处理价格跳空和趋势漂移等情况，提供更准确的波动率估计。
     *
     * 计算步骤：
     * 1. 计算开盘跳空方差（σ_o²）：衡量开盘价相对于前一日收盘价的变动程度。
     * 2. 计算收盘方差（σ_c²）：衡量收盘价相对于开盘价的变动程度。
     * 3. 计算 Rogers-Satchell 方差（σ_rs²）：综合考虑最高价、最低价、开盘价和收盘价的变动情况。
     * 4. 计算权重系数 k：用于在综合波动率计算中平衡各方差的贡献。
     * 5. 计算日方差（σ_d²）：综合上述方差和权重系数，得到每日的波动率估计。
     * 6. 年化波动率：将日方差乘以 252（假设一年有 252 个交易日），再开平方，得到年化波动率。
     *
     * 注意事项：
     * - 输入的 K 线数据应包含开盘价、最高价、最低价和收盘价。
     * - 数据长度应至少为 2，否则无法计算波动率。
     * - 方法会自动过滤价格非正的情况，并在数据不足时返回 null。
     *
     * @param ohlc 包含日 K 线数据的列表，每个元素应包含开盘价、最高价、最低价和收盘价。
     * @param tradingDaysPerYear 年交易天数，默认为 252 天。
     * @return 计算得到的杨-张年化波动率，若数据不足或无效，则返回 null。
     */
    fun calculateYangZhangAnnualVolatility(
        ohlc: List<StockData>,
        tradingDaysPerYear: Int = 252
    ): Double? {
        // 数据长度检查：至少需要3个数据点才能计算波动率
        if (ohlc.size < 3) return null

        val n = ohlc.size
        // 初始化存储列表
        val ocList = ArrayList<Double>(n - 1) // 存储 ln(O_i / C_{i-1}) - 开盘跳空对数收益率
        val coList = ArrayList<Double>(n)     // 存储 ln(C_i / O_i) - 日内收盘对开盘对数收益率
        val rsList = ArrayList<Double>(n)     // 存储 Rogers-Satchell 项

        // 遍历每个交易日，计算各项对数收益率
        for (i in 0 until n) {
            val day = ohlc[i]
            val open = day.open.toDouble()
            val close = day.close.toDouble()
            val high = day.high.toDouble()
            val low = day.low.toDouble()

            // 过滤无效价格数据（价格必须为正数）
            if (open <= 0.0 || close <= 0.0 || high <= 0.0 || low <= 0.0) continue

            // 计算日内收盘对开盘的对数收益率：ln(C_i / O_i)
            coList.add(kotlin.math.ln(close / open))

            // 计算 Rogers-Satchell 项：
            // ln(H_i/C_i) * ln(H_i/O_i) + ln(L_i/C_i) * ln(L_i/O_i)
            val rsTerm = kotlin.math.ln(high / close) * kotlin.math.ln(high / open) +
                    kotlin.math.ln(low / close) * kotlin.math.ln(low / open)
            rsList.add(rsTerm)

            // 计算开盘跳空对数收益率：ln(O_i / C_{i-1})
            // 需要前一日收盘价，所以从第二个交易日开始计算
            if (i >= 1) {
                val prevClose = ohlc[i - 1].close.toDouble()
                if (prevClose > 0.0) {
                    ocList.add(kotlin.math.ln(open / prevClose))
                }
            }
        }

        // 获取有效数据点数量
        val coCount = coList.size
        val ocCount = ocList.size
        val rsCount = rsList.size

        // 数据充足性检查：需要足够的数据点才能计算方差
        if (coCount < 2 || ocCount < 2 || rsCount < 1) return null

        // 计算各项对数收益率的均值
        val muCo = coList.average()  // 日内收盘对开盘对数收益率的均值
        val muOc = ocList.average()  // 开盘跳空对数收益率的均值

        // 计算收盘方差 σ_c² = (1/(n-1)) * Σ(ln(C_i/O_i) - μ_c)²
        var sumSqCo = 0.0
        for (v in coList) {
            val d = v - muCo
            sumSqCo += d * d
        }
        val sigmaC2 = sumSqCo / (coCount - 1)

        // 计算开盘跳空方差 σ_o² = (1/(n-1)) * Σ(ln(O_i/C_{i-1}) - μ_o)²
        var sumSqOc = 0.0
        for (v in ocList) {
            val d = v - muOc
            sumSqOc += d * d
        }
        val sigmaO2 = sumSqOc / (ocCount - 1)

        // 计算 Rogers-Satchell 方差 σ_rs² = (1/n) * Σ[ln(H_i/C_i)*ln(H_i/O_i) + ln(L_i/C_i)*ln(L_i/O_i)]
        var sumRs = 0.0
        for (v in rsList) sumRs += v
        val sigmaRS2 = sumRs / rsCount

        // 计算权重系数 k = 0.34 / (1.34 + (n+1)/(n-1))
        // 使用最小有效样本数确保计算稳定性
        val nForK = kotlin.math.min(kotlin.math.min(coCount, rsCount), ocCount + 1)
        if (nForK <= 1) return null
        val k = 0.34 / (1.34 + (nForK + 1.0) / (nForK - 1.0))

        // 计算 Yang-Zhang 日方差：σ_yz² = σ_o² + k*σ_c² + (1-k)*σ_rs²
        val yzVarianceDaily = sigmaO2 + k * sigmaC2 + (1.0 - k) * sigmaRS2
        if (yzVarianceDaily < 0.0) return null

        // 年化波动率：σ_yz_annual = √(σ_yz² * 年交易天数)
        return kotlin.math.sqrt(yzVarianceDaily * tradingDaysPerYear)
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

        // Debug: print the final URL for Tencent day data
        Log.d("AShare", "Fetching TX day data with URL: ${TX_DAY_BASE_URL}appstock/app/fqkline/get?param=$param")
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

        // Debug: print the final URL for Tencent minute data
        Log.d("AShare", "Fetching TX minute data with URL: ${TX_MIN_BASE_URL}appstock/app/kline/mkline?param=$param")
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

        // Debug: print the final URL used for fetching data
        Log.d("AShare", "Fetching data from URL: $url")

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
