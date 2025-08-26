package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 应用侧边栏组件
 * 包含配置备注、API测试、设置三个菜单项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    onClose: () -> Unit,
    onNavigateToConfigNote: () -> Unit,
    onNavigateToApiTest: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(300.dp)
    ) {
        // 侧边栏头部 - 关闭按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭侧边栏"
                )
            }
        }

        Divider()

        // 菜单项列表
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // 配置备注
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Note,
                        contentDescription = null
                    )
                },
                label = { Text("配置备注") },
                selected = false,
                onClick = {
                    onNavigateToConfigNote()
                    onClose()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // API测试
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null
                    )
                },
                label = { Text("API测试") },
                selected = false,
                onClick = {
                    onNavigateToApiTest()
                    onClose()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // 设置
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                },
                label = { Text("设置") },
                selected = false,
                onClick = {
                    onNavigateToSettings()
                    onClose()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

