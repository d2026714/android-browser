package com.example.browser.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.example.browser.R
import com.example.browser.manager.SettingsManager
import com.example.browser.ui.viewmodel.BrowserViewModel

data class WallpaperItem(
    val key: String,
    val name: String,
    val resId: Int?,
    val nameResId: Int = 0
)

val wallpaperOptions = listOf(
    WallpaperItem(SettingsManager.WALLPAPER_NONE, "None", null, R.string.wallpaper_none),
    WallpaperItem(SettingsManager.WALLPAPER_DEFAULT, "Default", R.drawable.wallpaper_geometric_blue, R.string.wallpaper_default),
    WallpaperItem("night_mountain", "Night Mountain", R.drawable.wallpaper_night_mountain, R.string.wallpaper_night_mountain),
    WallpaperItem("sunset_ocean", "Sunset Ocean", R.drawable.wallpaper_sunset_ocean, R.string.wallpaper_sunset_ocean),
    WallpaperItem("forest", "Forest", R.drawable.wallpaper_forest, R.string.wallpaper_forest),
    WallpaperItem("aurora", "Aurora", R.drawable.wallpaper_aurora, R.string.wallpaper_aurora),
    WallpaperItem("abstract_waves", "Abstract Waves", R.drawable.wallpaper_abstract_waves, R.string.wallpaper_abstract_waves),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPickerSheet(viewModel: BrowserViewModel, onDismiss: () -> Unit) {
    val currentWallpaper by viewModel.wallpaper.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                stringResource(R.string.wallpaper),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                stringResource(R.string.choose_wallpaper_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(wallpaperOptions) { item ->
                    WallpaperOptionCard(
                        item = item,
                        isSelected = currentWallpaper == item.key,
                        onClick = {
                            viewModel.setWallpaper(item.key)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperOptionCard(
    item: WallpaperItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.resId != null) {
                Image(
                    painter = painterResource(id = item.resId),
                    contentDescription = if (item.nameResId != 0) stringResource(item.nameResId) else item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Semi-transparent overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            } else {
                // "None" option - show theme background color
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            // Label
            Text(
                text = if (item.nameResId != 0) stringResource(item.nameResId) else item.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (item.resId != null) Color.White else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(2.dp)
                )
            }
        }
    }
}
