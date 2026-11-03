package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.plantwatch.R
import com.alberto.plantwatch.ui.theme.PlantWatchTheme

/** Connects [PlantDetailViewModel] to the stateless [PlantDetailScreen]. */
@Composable
fun PlantDetailRoute(
    onBack: () -> Unit,
    viewModel: PlantDetailViewModel = viewModel(factory = PlantDetailViewModel.Factory),
) {
    val plant by viewModel.plant.collectAsStateWithLifecycle()

    PlantDetailScreen(
        // `?.name ?: ""`: the name if the plant is loaded, empty text otherwise.
        plantName = plant?.name ?: "",
        onBack = onBack,
    )
}

/**
 * One plant's screen: a top bar with a back arrow and the plant's name.
 * The care tasks list comes in the next step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(plantName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantDetailScreenPreview() {
    PlantWatchTheme {
        PlantDetailScreen(plantName = "Monstera", onBack = {})
    }
}
