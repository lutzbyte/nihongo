package com.nihongo.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import com.nihongo.app.navigation.TopLevelDestination
import com.nihongo.app.navigation.TopLevelDestinations
import com.nihongo.app.navigation.isSelected
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.IconSize

@Composable
fun NihongoBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = Elevation.none,
    ) {
        TopLevelDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination.isSelected(destination),
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.md),
                    )
                },
                label = { Text(text = stringResource(destination.labelRes)) },
            )
        }
    }
}