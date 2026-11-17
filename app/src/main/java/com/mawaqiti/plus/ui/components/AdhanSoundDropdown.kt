package com.mawaqiti.plus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.notify.AdhanSound
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * beta14: the golden-night adhan picker.
 *
 * A closed trigger (selected name + chevron) opens a themed [DropdownMenu]:
 * `sheet` surface, `accent` hairline border, `sheetText` labels, a golden wash
 * and a check icon on the current choice. Every colour is a [skyPalette] token;
 * nothing is hardcoded, and Arabic labels always come from `strings.xml`
 * (through [AdhanSound.labelRes]).
 *
 * Material3's [DropdownMenu] supplies the smooth scale/fade open-close
 * animation and the dismiss-on-outside-tap behaviour, so there is no bespoke
 * animator to keep alive. The list itself is [AdhanSound.ordered] - the picker
 * cannot drift from the registry, and adding an adhan needs no change here.
 */
@Composable
fun AdhanSoundDropdown(
    selected: AdhanSound,
    onSelect: (AdhanSound) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = skyPalette
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .testTag("adhan_sound_trigger")
                .clip(shape)
                .background(palette.text.copy(alpha = 0.06f))
                .border(1.dp, palette.accent.copy(alpha = 0.32f), shape)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    role = Role.DropdownList,
                    onClick = { expanded = true }
                )
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(selected.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .testTag("adhan_sound_menu")
                .widthIn(min = 220.dp),
            offset = DpOffset(x = 0.dp, y = 6.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = palette.sheet.copy(alpha = 0.98f),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.30f))
        ) {
            AdhanSound.ordered.forEach { sound ->
                val isSelected = sound == selected
                DropdownMenuItem(
                    modifier = Modifier
                        .testTag("adhan_sound_option_${sound.storageKey}")
                        .background(
                            if (isSelected) palette.accent.copy(alpha = 0.14f)
                            else palette.sheet.copy(alpha = 0f)
                        ),
                    text = {
                        Text(
                            text = stringResource(sound.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) palette.accent else palette.sheetText
                        )
                    },
                    onClick = {
                        expanded = false
                        if (!isSelected) onSelect(sound)
                    },
                    leadingIcon = {
                        // A constant-size slot keeps every row's text aligned;
                        // only the selected row paints the golden check.
                        Box(modifier = Modifier.size(20.dp)) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
