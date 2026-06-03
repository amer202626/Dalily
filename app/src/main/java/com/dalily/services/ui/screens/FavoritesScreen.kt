package com.dalily.services.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dalily.services.data.FirebaseSimulator

@Composable
fun FavoritesScreen(navController: NavController) {
    val context = LocalContext.current
    val dbState by FirebaseSimulator.dbState.collectAsState()
    
    // Shared Preferences for favorites storage
    val sharedPrefs = remember { context.getSharedPreferences("dalily_favs", Context.MODE_PRIVATE) }
    var favoriteIds by remember {
        mutableStateOf(sharedPrefs.getStringSet("ids", emptySet()) ?: emptySet())
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("المفضلة وصيانتي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        val bookmarkedProviders = dbState.providers.filter { favoriteIds.contains(it.id) }

        if (bookmarkedProviders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Text("قائمتك المفضلة فارغة!", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("ابدأ بإضافة الأنشطة الموثوقة لتصل إليها بسرعة", fontSize = 11.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookmarkedProviders) { provider ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("detail/${provider.id}") }
                            .testTag("fav_card_${provider.id}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val placeholderImage = "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&q=80&w=300"
                            AsyncImage(
                                model = if (provider.imageUrl.isEmpty()) placeholderImage else provider.imageUrl,
                                contentDescription = provider.name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(provider.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                    Text(" ${provider.rating} (${provider.reviewsCount} تقييم)", fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            IconButton(
                                onClick = {
                                    val updated = favoriteIds.toMutableSet().apply { remove(provider.id) }
                                    sharedPrefs.edit().putStringSet("ids", updated).apply()
                                    favoriteIds = updated
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف المفضلة", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
