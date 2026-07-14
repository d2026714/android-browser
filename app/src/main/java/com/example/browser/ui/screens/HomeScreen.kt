package com.example.browser.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.R
import com.example.browser.manager.SettingsManager
import com.example.browser.ui.viewmodel.BrowserViewModel

data class QuickLink(
    val title: String,
    val url: String,
    @DrawableRes val iconRes: Int
)

val defaultQuickLinks = listOf(
    QuickLink("Google", "https://www.google.com", R.drawable.ic_brand_google),
    QuickLink("YouTube", "https://www.youtube.com", R.drawable.ic_brand_youtube),
    QuickLink("Wikipedia", "https://www.wikipedia.org", R.drawable.ic_brand_wikipedia),
    QuickLink("GitHub", "https://github.com", R.drawable.ic_brand_github),
    QuickLink("Reddit", "https://www.reddit.com", R.drawable.ic_brand_reddit),
    QuickLink("Twitter", "https://twitter.com", R.drawable.ic_brand_twitter),
    QuickLink("Amazon", "https://www.amazon.com", R.drawable.ic_brand_amazon),
    QuickLink("Netflix", "https://www.netflix.com", R.drawable.ic_brand_netflix),
)

private fun getWallpaperResId(key: String): Int? = when (key) {
    SettingsManager.WALLPAPER_DEFAULT -> R.drawable.wallpaper_geometric_blue
    "night_mountain" -> R.drawable.wallpaper_night_mountain
    "sunset_ocean" -> R.drawable.wallpaper_sunset_ocean
    "forest" -> R.drawable.wallpaper_forest
    "aurora" -> R.drawable.wallpaper_aurora
    "abstract_waves" -> R.drawable.wallpaper_abstract_waves
    else -> null
}

@Composable
fun HomeScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    val wallpaperKey by viewModel.wallpaper.collectAsState()
    val wallpaperResId = getWallpaperResId(wallpaperKey)
    val hasWallpaper = wallpaperResId != null

    val textColor = if (hasWallpaper) Color.White else MaterialTheme.colorScheme.onBackground

    Box(modifier = modifier.fillMaxSize()) {
        // Background
        if (wallpaperResId != null) {
            Image(
                painter = painterResource(id = wallpaperResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dark scrim for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Browser logo/title
            Text(
                text = "🌐",
                fontSize = 48.sp
            )
            Text(
                text = "Android Browser",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (hasWallpaper) Color.White.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_icon),
                    tint = if (hasWallpaper) Color.White.copy(alpha = 0.7f)
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_or_enter_url),
                            color = if (hasWallpaper) Color.White.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = if (hasWallpaper) Color.White else MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = if (hasWallpaper) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true
                )
                if (searchText.isNotBlank()) {
                    IconButton(onClick = {
                        viewModel.navigateTo(searchText)
                        searchText = ""
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.go),
                            tint = if (hasWallpaper) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quick links grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(defaultQuickLinks) { link ->
                    QuickLinkItem(
                        link = link,
                        hasWallpaper = hasWallpaper
                    ) {
                        viewModel.navigateTo(link.url)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickLinkItem(
    link: QuickLink,
    hasWallpaper: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (hasWallpaper) Color.White.copy(alpha = 0.2f)
                    else Color.White
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = link.iconRes),
                contentDescription = link.title,
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = link.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (hasWallpaper) Color.White else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
