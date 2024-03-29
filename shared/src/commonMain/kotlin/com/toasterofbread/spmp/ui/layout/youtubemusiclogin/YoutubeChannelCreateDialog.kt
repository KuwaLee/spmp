package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.rememberImagePainter
import com.toasterofbread.composekit.utils.composable.LinkifyText
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.composekit.utils.composable.WidthShrinkText
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.external.YoutubeAccountCreationForm
import dev.toastbits.ytmkt.model.external.YoutubeAccountCreationForm.InputField
import io.ktor.http.Headers
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@Composable
fun YoutubeChannelCreateDialog(
    headers: Headers,
    form: YoutubeAccountCreationForm.ChannelCreationForm,
    api: YoutubeiApi,
    onFinished: (Result<String>?) -> Unit
) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val fields = remember(form) { form.contents.createCoreIdentityChannelContentRenderer.getInputFields() }
    val params = remember(fields) {
        mutableStateMapOf<String, String>().apply {
            for (field in fields) {
                put(field.key.getParameterName(), field.initial_value ?: "")
            }
        }
    }
    val can_create = params[fields[0].key.getParameterName()]!!.isNotBlank()

    AlertDialog(
        onDismissRequest = { onFinished(null) },
        confirmButton = {
            var loading by remember { mutableStateOf(false) }

            Button(
                {
                    if (loading) {
                        return@Button
                    }

                    coroutine_scope.launch {
                        loading = true
                        coroutineContext.job.invokeOnCompletion {
                            loading = false
                        }

                        onFinished(runCatching {
                            val channel = api.CreateYoutubeChannel.createYoutubeChannel(headers, form.getChannelCreationToken()!!, params).getOrThrow()

                            // Give YouTube time to update the account before we proceed
                            delay(1000)

                            return@runCatching channel.id
                        })
                    }
                },
                enabled = can_create
            ) {
                Crossfade(loading) { loading ->
                    Box(contentAlignment = Alignment.Center) {
                        if (loading) {
                            SubtleLoadingIndicator()
                        }
                        Text(
                            getString("youtube_channel_creation_confirm"),
                            Modifier.alpha(if (loading) 0f else 1f)
                        )
                    }
                }
            }
        },
        dismissButton = {
            ShapedIconButton(
                { onFinished(null) },
                IconButtonDefaults.iconButtonColors(
                    containerColor = player.theme.accent,
                    contentColor = player.theme.on_accent,
                    disabledContainerColor = player.theme.accent.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Default.Close, null)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val thumbnail = form.contents.createCoreIdentityChannelContentRenderer.profilePhoto.thumbnails.firstOrNull()
                if (thumbnail != null) {
                    Image(rememberImagePainter(thumbnail.url), null, Modifier.size(30.dp).clip(CircleShape))
                }
                WidthShrinkText(getString("youtube_channel_creation_title"))
            }
        },
        text = {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((index, field) in fields.withIndex()) {
                    val is_error: Boolean = index == 0 && !can_create
                    val parameter_name: String = field.key.getParameterName()

                    TextField(
                        params[parameter_name]!!,
                        { params[parameter_name] = it },
                        Modifier.appTextField(),
                        label = {
                            Text(
                                when (field.key) {
                                    InputField.Key.GIVEN_NAME -> getString("youtube_channel_creation_field_given_name")
                                    InputField.Key.FAMILY_NAME -> getString("youtube_channel_creation_field_family_name")
                                }
                            )
                        },
                        supportingText = {
                            AnimatedVisibility(is_error) {
                                Text(getString("error_message_generic"))
                            }
                        },
                        isError = is_error
                    )
                }

                LinkifyText(
                    getString("youtube_channel_creation_subtitle"),
                    player.theme.accent,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    )
}
