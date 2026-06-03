package com.dalily.services.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dalily.services.data.FirebaseSimulator
import com.dalily.services.data.ServiceProvider

@Composable
fun FavoritesScreen(
    onBackClick: () -> Unit,
    onNavigateToProvider: (String) -> Unit
) {
    val context = LocalContext.current
    val providers by FirebaseSimulator.providers.collectAsState()
    val systemSettings by FirebaseSimulator.systemSettings.collectAsState()

    // Labeled Folders list
    val folders = remember { mutableStateListOf("الكافة", "العمل (Work)", "الشخصي (Personal)") }
    var selectedFolder by remember { mutableStateOf("الكافة") }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    // Persistent favorite provider IDs mapping (Local state with nice starter items)
    val favoriteMappings = remember { 
        mutableStateMapOf<String, String>().apply {
            put("PRV_01", "الشخصي (Personal)")
            put("PRV_02", "العمل (Work)")
        }
    }

    // Filtered providers
    val favoriteProviders = remember(providers, favoriteMappings, selectedFolder) {
        providers.filter { p ->
            favoriteMappings.containsKey(p.id) && 
            (selectedFolder == "الكافة" || favoriteMappings[p.id] == selectedFolder)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick, modifier = Modifier.testTag("favorites_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "المفضلة والمجلدات المصنفة 🗂️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Add custom label folder button
                    IconButton(onClick = { showNewFolderDialog = true }, modifier = Modifier.testTag("btn_create_folder")) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "إنشاء مجلد", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Horizontal scroll folders bar
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders) { folderName ->
                        val isSelected = selectedFolder == folderName
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedFolder = folderName }
                                .testTag("favorite_folder_$folderName"),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    size = 14.dp,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = folderName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Favorite providers lists
                if (favoriteProviders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color.Gray.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("مجلد ($selectedFolder) فارغ حالياً.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favoriteProviders) { provider ->
                            val currentFolder = favoriteMappings[provider.id] ?: "الكافة"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fav_item_${provider.id}"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onNavigateToProvider(provider.id) }
                                    ) {
                                        Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("القسم: ${provider.category} | ${provider.subcategory}", fontSize = 10.sp, color = Color.Gray)
                                        
                                        // Folder Assignment dropdown
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("المجلد النشط: ", fontSize = 8.sp)
                                                Text(currentFolder, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }

                                    // Controls to remove or re-categorize folder
                                    Row {
                                        // Cycle through folders assignment
                                        IconButton(
                                            onClick = {
                                                val remainingFolders = folders.filter { f -> f != "الكافة" }
                                                val nextIndex = (remainingFolders.indexOf(currentFolder) + 1) % remainingFolders.size
                                                favoriteMappings[provider.id] = remainingFolders[nextIndex]
                                                Toast.makeText(context, "تم نقل الفني لمجلد ${remainingFolders[nextIndex]}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.testTag("cycle_folder_btn_${provider.id}")
                                        ) {
                                            Icon(Icons.Default.Folder, contentDescription = "تغيير المجلد البديل", tint = MaterialTheme.colorScheme.secondary, size = 18.dp)
                                        }

                                        // Delete/Remove Favorite
                                        IconButton(
                                            onClick = {
                                                favoriteMappings.remove(provider.id)
                                                Toast.makeText(context, "تمت الإزالة من تفضيلاته المبرمجة", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.testTag("del_fav_btn_${provider.id}")
                                        ) {
                                            Icon(Icons.Default.Favorite, contentDescription = "مزيل", tint = Color.Red, size = 18.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add custom labeled folder dialog popup
        if (showNewFolderDialog) {
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                title = { Text("إنشاء مجلد مفضلة مخصص 📂", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column {
                        Text("ادخل اسماً معنوناً للمجلد الجديد لتصنيف الفنيين كالعائلات، الحالات المنزلية الجانبية، الأجهزة، العقود الفترية:", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        TextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            placeholder = { Text("مثال: صيانة المنزل", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFolderName.trim().isNotEmpty() && !folders.contains(newFolderName.trim())) {
                                folders.add(newFolderName.trim())
                                showNewFolderDialog = false
                                newFolderName = ""
                                Toast.makeText(context, "تم إنشاء المجلد المخصص وحفظه!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("حفظ المجلد")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
