@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.strategicassetallocationassistant.data.network.AShare
import com.example.strategicassetallocationassistant.data.network.StockData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTestScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var stockCode by remember { mutableStateOf("sh513500") }
    var endDate by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("1") }
    var frequency by remember { mutableStateOf("1m") }
    var apiResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 连接测试") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 输入区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "API 参数设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = stockCode,
                        onValueChange = { stockCode = it },
                        label = { Text("股票代码") },
                        placeholder = { Text("例如: 000001") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("结束日期") },
                        placeholder = { Text("例如: 2024-01-01，留空则使用当前日期") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = count,
                        onValueChange = { count = it },
                        label = { Text("数据条数") },
                        placeholder = { Text("默认: 1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("频率") },
                        placeholder = { Text("1d, 1w, 1M, 1m, 5m, 15m, 30m, 60m") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = ""
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        AShare.getPrice(
                                            code = stockCode,
                                            endDate = endDate,
                                            count = count.toIntOrNull() ?: 10,
                                            frequency = frequency
                                        )
                                    }

                                    // 格式化API响应为JSON字符串
                                    val json = Json {
                                        prettyPrint = true
                                        ignoreUnknownKeys = true
                                    }
                                    apiResponse = json.encodeToString(result)
                                } catch (e: Exception) {
                                    errorMessage = "API调用失败: ${e.message}\n\n${e.stackTraceToString()}"
                                    apiResponse = ""
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && stockCode.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("调用中...")
                        } else {
                            Text("调用 AShare API")
                        }
                    }
                }
            }

            // 响应显示区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "API 响应内容",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (apiResponse.isNotEmpty()) {
                        OutlinedTextField(
                            value = apiResponse,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            label = { Text("JSON 响应") }
                        )
                    } else if (!isLoading && errorMessage.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "点击上方按钮调用API，响应内容将显示在这里",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("正在调用API...")
                            }
                        }
                    }
                }
            }

            // 说明区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    Text(
                        text = "• 股票代码：输入6位数字的A股代码\n• 结束日期：格式为YYYY-MM-DD，留空使用当前日期\n• 数据条数：获取的历史数据条数\n• 频率：1d(日线), 1w(周线), 1M(月线), 1m,5m,15m,30m,60m(分钟线)",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "API来源：底层爬取新浪和腾讯的接口数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}