package com.alberto.plantwatch.plants.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alberto.plantwatch.R
import com.alberto.plantwatch.guide.CareGuideEntry
import com.alberto.plantwatch.guide.LocalizedText
import com.alberto.plantwatch.guide.Problem
import com.alberto.plantwatch.guide.SpeciesMatch
import com.alberto.plantwatch.guide.SunNeed
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.ui.IconBadge
import com.alberto.plantwatch.ui.theme.PlantWatchTheme

/*
 * The care guide on the plant screen: a card with the linked species' hints,
 * and the dialog to pick the species.
 */

/** The display name of an entry: its first Portuguese name, capitalized ("Jiboia"). */
fun displayName(entry: CareGuideEntry): String =
    entry.namesPt.first().replaceFirstChar { it.uppercase() }

/**
 * The Care guide card. Three states:
 * - not linked: a prompt to link a species (tapping the card opens the picker);
 * - linked to an id missing from the guide: "unknown species";
 * - linked: the species and its hints, with a menu to change or unlink.
 */
@Composable
fun CareGuideCard(
    entry: CareGuideEntry?,
    isLinked: Boolean,
    suggestedTasks: List<CareTask>,
    onLinkClick: () -> Unit,
    onUnlinkClick: () -> Unit,
    onAddSuggestedTasks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (entry == null) {
            // Not linked (or unknown id): the whole card is one tap target.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLinkClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GuideBadge()
                Column {
                    Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(if (isLinked) R.string.guide_unknown_species else R.string.guide_not_linked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LinkedGuide(entry, suggestedTasks, onChangeClick = onLinkClick, onUnlinkClick = onUnlinkClick, onAddSuggestedTasks)
        }
    }
}

@Composable
private fun GuideBadge() {
    IconBadge(
        icon = R.drawable.ic_eco,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/** The card's content for a linked species. */
@Composable
private fun LinkedGuide(
    entry: CareGuideEntry,
    suggestedTasks: List<CareTask>,
    onChangeClick: () -> Unit,
    onUnlinkClick: () -> Unit,
    onAddSuggestedTasks: () -> Unit,
) {
    // UI-only state kept across rotation: is the overflow menu open, are the
    // full hints shown (collapsed by default, so the tasks below stay in view),
    // are all problems shown.
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showAllProblems by rememberSaveable { mutableStateOf(false) }

    Column(
        // `animateContentSize()`: when the content grows or shrinks (expanding
        // and collapsing), the card resizes smoothly instead of jumping.
        modifier = Modifier
            .animateContentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Tapping the badge and names toggles the hints; the menu button
            // handles its own taps.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GuideBadge()
                Column {
                    Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.titleMedium)
                    Text(displayName(entry), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        entry.scientificName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // A Box anchors the drop-down menu to the button.
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.guide_menu_description))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.guide_change_species)) },
                        onClick = { menuOpen = false; onChangeClick() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.guide_unlink)) },
                        onClick = { menuOpen = false; onUnlinkClick() },
                    )
                }
            }
        }

        if (expanded) {
            GuideDetails(entry, showAllProblems, onToggleProblems = { showAllProblems = !showAllProblems })
        } else {
            // Collapsed: the essentials in one line.
            Text(
                guideSummary(entry),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Always visible: the suggested-tasks action and the safety warning.
        if (suggestedTasks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(onClick = onAddSuggestedTasks) {
                    Text(stringResource(R.string.guide_add_suggested))
                }
                Text(
                    text = suggestedTasks.map { task ->
                        pluralStringResource(R.plurals.guide_suggested_task, task.intervalDays, taskName(task), task.intervalDays)
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (entry.toxicToPets == true) {
            Text(
                stringResource(R.string.guide_toxic),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }

        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.guide_hide_hints else R.string.guide_show_hints))
        }
    }
}

/** The full hints (water, sun, fertilizer) and the common problems. */
@Composable
private fun GuideDetails(entry: CareGuideEntry, showAllProblems: Boolean, onToggleProblems: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (entry.generic) {
            Text(
                stringResource(R.string.guide_generic_note),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        GuideSection(
            icon = R.drawable.ic_water_drop,
            title = pluralStringResource(R.plurals.guide_water_every, entry.waterEveryDays, entry.waterEveryDays),
            body = localized(entry.water),
        )
        GuideSection(icon = R.drawable.ic_sunny, title = sunNeedLabel(entry.sun), body = localized(entry.sunHint))
        GuideSection(
            icon = R.drawable.ic_compost,
            // `?.let { } ?: ...`: one text when there's an interval, another when not.
            title = entry.fertilizeEveryDays?.let { pluralStringResource(R.plurals.guide_fertilize_every, it, it) }
                ?: stringResource(R.string.guide_fertilize_rarely),
            body = localized(entry.fertilizer),
        )

        HorizontalDivider()
        Text(stringResource(R.string.guide_problems), style = MaterialTheme.typography.titleSmall)
        val shown = if (showAllProblems) entry.problems else entry.problems.take(2)
        shown.forEach { ProblemRow(it) }
        if (entry.problems.size > 2) {
            TextButton(onClick = onToggleProblems) {
                Text(
                    if (showAllProblems) stringResource(R.string.guide_show_less)
                    else stringResource(R.string.guide_show_all, entry.problems.size),
                )
            }
        }
    }
}

/** One line for the collapsed card: "Water ~7 days · Bright indirect light · Fertilize ~30 days". */
@Composable
private fun guideSummary(entry: CareGuideEntry): String {
    val water = pluralStringResource(R.plurals.guide_summary_water, entry.waterEveryDays, entry.waterEveryDays)
    val sun = stringResource(
        when (entry.sun) {
            SunNeed.FULL_SUN -> R.string.sun_short_full_sun
            SunNeed.PART_SUN -> R.string.sun_short_part_sun
            SunNeed.BRIGHT_INDIRECT -> R.string.sun_short_bright_indirect
            SunNeed.LOW_LIGHT -> R.string.sun_short_low_light
        },
    )
    val fertilize = entry.fertilizeEveryDays?.let { pluralStringResource(R.plurals.guide_summary_fertilize, it, it) }
        ?: stringResource(R.string.guide_summary_fertilize_rarely)
    return listOf(water, sun, fertilize).joinToString(" · ")
}

/** One hint: a small icon, a bold title line and the explanation. */
@Composable
private fun GuideSection(@DrawableRes icon: Int, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProblemRow(problem: Problem) {
    Column {
        Text(localized(problem.title), style = MaterialTheme.typography.bodyLarge)
        Text(localized(problem.hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "Sun · bright indirect light", ... */
@Composable
fun sunNeedLabel(sun: SunNeed): String = stringResource(
    when (sun) {
        SunNeed.FULL_SUN -> R.string.sun_need_full_sun
        SunNeed.PART_SUN -> R.string.sun_need_part_sun
        SunNeed.BRIGHT_INDIRECT -> R.string.sun_need_bright_indirect
        SunNeed.LOW_LIGHT -> R.string.sun_need_low_light
    },
)

/**
 * Dialog to pick a species: a search field and the matching entries, updated as
 * you type (the caller runs the search; this composable only shows [results]).
 * Each result shows the Portuguese name, the scientific name, and the name that
 * matched when it isn't the first one ("matches: snake plant").
 */
@Composable
fun SpeciesPickerDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SpeciesMatch>,
    onPick: (CareGuideEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.species_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.species_picker_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (results.isEmpty() && query.isNotBlank()) {
                    Text(stringResource(R.string.species_picker_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // At most ~320 dp tall, scrolling inside the dialog.
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(results, key = { it.entry.id }) { match ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(match.entry) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(displayName(match.entry), style = MaterialTheme.typography.titleSmall)
                            Text(
                                match.entry.scientificName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!match.matchedName.equals(match.entry.namesPt.first(), ignoreCase = true)) {
                                Text(
                                    match.matchedName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * A guide text in the app's current language: the per-app language if one is
 * chosen in Android's settings, otherwise the phone's. `LocalConfiguration`
 * holds the configuration the screen is drawn with; `locales[0]` is the
 * language actually in use.
 */
@Composable
private fun localized(text: LocalizedText): String =
    text.forLanguage(LocalConfiguration.current.locales[0].language)

// --- Previews ---

private val previewEntry = CareGuideEntry(
    id = "epipremnum-aureum", scientificName = "Epipremnum aureum",
    namesPt = listOf("jiboia"), namesEn = listOf("pothos"),
    waterEveryDays = 7, water = LocalizedText("Water when the top 2-3 cm of soil is dry.", "Regue quando os 2-3 cm de cima estiverem secos."),
    sun = SunNeed.BRIGHT_INDIRECT, sunHint = LocalizedText("Bright indirect light; tolerates low light.", "Luz indireta forte; tolera pouca luz."),
    fertilizeEveryDays = 30, fertilizer = LocalizedText("Balanced liquid fertilizer at half strength, spring to summer.", "Adubo líquido equilibrado em meia dose, da primavera ao verão."),
    problems = listOf(
        Problem(LocalizedText("Yellow leaves", "Folhas amarelas"), LocalizedText("Usually overwatering.", "Geralmente excesso de água.")),
        Problem(LocalizedText("Brown edges", "Bordas marrons"), LocalizedText("Dry air or underwatering.", "Ar seco ou falta de água.")),
        Problem(LocalizedText("Mealybugs", "Cochonilhas"), LocalizedText("Wipe off with alcohol.", "Limpe com álcool.")),
    ),
    toxicToPets = true,
)

@Preview(showBackground = true)
@Composable
private fun CareGuideCardPreview() {
    PlantWatchTheme {
        CareGuideCard(
            entry = previewEntry, isLinked = true,
            suggestedTasks = listOf(CareTask(plantId = 1, type = com.alberto.plantwatch.plants.data.TaskType.WATER, intervalDays = 7, nextDue = java.time.LocalDate.of(2026, 11, 4))),
            onLinkClick = {}, onUnlinkClick = {}, onAddSuggestedTasks = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CareGuideCardNotLinkedPreview() {
    PlantWatchTheme {
        CareGuideCard(
            entry = null, isLinked = false, suggestedTasks = emptyList(),
            onLinkClick = {}, onUnlinkClick = {}, onAddSuggestedTasks = {},
        )
    }
}

@Preview
@Composable
private fun SpeciesPickerPreview() {
    PlantWatchTheme {
        SpeciesPickerDialog(
            query = "jiboia",
            onQueryChange = {},
            results = listOf(SpeciesMatch(previewEntry, "jiboia")),
            onPick = {},
            onDismiss = {},
        )
    }
}
