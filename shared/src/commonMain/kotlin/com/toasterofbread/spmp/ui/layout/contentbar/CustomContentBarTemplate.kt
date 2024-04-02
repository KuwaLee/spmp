package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.element.*
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.theme.appHover
import com.toasterofbread.spmp.model.appaction.OtherAppAction
import com.toasterofbread.spmp.model.appaction.action.navigation.AppPageNavigationAction
import com.toasterofbread.spmp.model.appaction.SongAppAction

enum class CustomContentBarTemplate {
    NAVIGATION,
    LYRICS,
    SONG_ACTIONS;

    fun getName(): String =
        when (this) {
            NAVIGATION -> getString("content_bar_template_navigation")
            LYRICS -> getString("content_bar_template_lyrics")
            SONG_ACTIONS -> getString("content_bar_template_song_actions")
        }

    fun getIcon(): ImageVector =
        when (this) {
            NAVIGATION -> Icons.Default.Widgets
            LYRICS -> Icons.Default.Lyrics
            SONG_ACTIONS -> Icons.Default.MusicNote
        }

    fun getElements(): List<ContentBarElement> =
        when (this) {
            NAVIGATION -> listOf(
                ContentBarElementButton.ofAppPage(AppPage.Type.SONG_FEED),
                ContentBarElementButton.ofAppPage(AppPage.Type.LIBRARY),
                ContentBarElementButton.ofAppPage(AppPage.Type.SEARCH),
                ContentBarElementButton.ofAppPage(AppPage.Type.RADIO_BUILDER),
                ContentBarElementButton(OtherAppAction(OtherAppAction.Action.RELOAD_PAGE)),
                ContentBarElementPinnedItems(size_mode = ContentBarElement.SizeMode.FILL),
                ContentBarElementButton.ofAppPage(AppPage.Type.PROFILE),
                ContentBarElementButton.ofAppPage(AppPage.Type.CONTROL_PANEL),
                ContentBarElementButton.ofAppPage(AppPage.Type.SETTINGS)
            )
            LYRICS -> listOf(
                ContentBarElementLyrics(size_mode = ContentBarElement.SizeMode.FILL)
            )
            SONG_ACTIONS -> listOf(
                ContentBarElementButton(SongAppAction(SongAppAction.Action.OPEN_EXTERNALLY)),
                ContentBarElementButton(SongAppAction(SongAppAction.Action.TOGGLE_LIKE)),
                ContentBarElementSpacer(size_mode = ContentBarElement.SizeMode.FILL),
                ContentBarElementButton(SongAppAction(SongAppAction.Action.DOWNLOAD)),
                ContentBarElementButton(SongAppAction(SongAppAction.Action.START_RADIO))
            )
        }

    @Composable
    private fun BarPreview(modifier: Modifier = Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val bar: CustomContentBar = remember { CustomContentBar("", elements = getElements()) }

        Column(
            modifier
                .background(player.theme.card, RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(getIcon(), null)
                Text(getName())
            }

            bar.CustomBarContent(
                modifier = Modifier.background(player.theme.vibrant_accent, RoundedCornerShape(16.dp)),
                background_colour = Theme.Colour.VIBRANT_ACCENT,
                vertical = false,
                content_padding = PaddingValues(5.dp),
                buttonContent = { _, element, size ->
                    element.Element(false, size, onPreviewClick = {})
                }
            )
        }
    }

    companion object {
        @Composable
        fun SelectionDialog(modifier: Modifier = Modifier, onSelected: (CustomContentBarTemplate?) -> Unit) {
            val player: PlayerState = LocalPlayerState.current

            AlertDialog(
                { onSelected(null) },
                modifier = modifier,
                confirmButton = {
                    Button(
                        { onSelected(null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = player.theme.background,
                            contentColor = player.theme.on_background
                        ),
                        modifier = Modifier.appHover(true)
                    ) {
                        Text(getString("action_cancel"))
                    }
                },
                title = {
                    Text(getString("content_bar_editor_template_dialog_title"))
                },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries) { template ->
                            template.BarPreview(
                                Modifier
                                    .platformClickable(
                                        onClick = { onSelected(template) }
                                    )
                                    .appHover(true)
                            )
                        }
                    }
                }
            )
        }
    }
}
