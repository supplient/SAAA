package com.example.strategicassetallocationassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.strategicassetallocationassistant.data.network.AShare
import com.example.strategicassetallocationassistant.data.network.StockData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiTestViewModel @Inject constructor() : ViewModel() {

    // API参数
    val code = MutableStateFlow("")
    val count = MutableStateFlow("10")
    val frequency = MutableStateFlow("1d")
    val endDate = MutableStateFlow("")

    // API响应状态
    private val _apiResponse = MutableStateFlow<String>("")
    val apiResponse: StateFlow<String> = _apiResponse.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun callApi() {
        if (code.value.isBlank()) {
            _error.value = "请输入资产代码"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _apiResponse.value = ""

            try {
                val result = AShare.getPrice(
                    code = code.value.trim(),
                    count = count.value.toIntOrNull() ?: 10,
                    frequency = frequency.value,
                    endDate = endDate.value.trim()
                )

                _apiResponse.value = formatApiResponse(result)
            } catch (e: Exception) {
                _error.value = "API调用失败: ${e.message}"
                _apiResponse.value = "错误详情:\n${e.stackTraceToString()}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun formatApiResponse(data: List<StockData>): String {
        if (data.isEmpty()) {
            return "API返回空数据"
        }

        val header = """
            |API调用参数:
            |  代码: ${code.value}
            |  数量: ${count.value}
            |  频率: ${frequency.value}
            |  结束日期: ${endDate.value.ifBlank { "默认" }}
            |
            |返回数据 (${data.size} 条记录):
            |
            """.trimMargin()

        val dataContent = data.joinToString("\n") { stock ->
            "时间: ${stock.time}, 开盘: ${stock.open}, 收盘: ${stock.close}, 最高: ${stock.high}, 最低: ${stock.low}, 成交量: ${stock.volume}"
        }

        return header + dataContent
    }

    fun clearResponse() {
        _apiResponse.value = ""
        _error.value = null
    }
}
