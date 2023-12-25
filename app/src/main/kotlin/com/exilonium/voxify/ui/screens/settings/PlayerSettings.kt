package com.exilonium.voxify.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.exilonium.voxify.LocalPlayerServiceBinder
import com.exilonium.voxify.R
import com.exilonium.voxify.preferences.PlayerPreferences
import com.exilonium.voxify.ui.components.themed.SecondaryTextButton
import com.exilonium.voxify.ui.screens.Route
import com.exilonium.voxify.utils.isAtLeastAndroid6
import com.exilonium.voxify.utils.toast
import kotlin.math.floor

@OptIn(UnstableApi::class)
@Route
@Composable
fun PlayerSettings() = with(PlayerPreferences) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    SettingsCategoryScreen(title = stringResource(R.string.player)) {
        SettingsGroup(title = stringResource(R.string.player)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.persistent_queue),
                text = stringResource(R.string.persistent_queue_description),
                isChecked = persistentQueue,
                onCheckedChange = { persistentQueue = it }
            )

            if (isAtLeastAndroid6) SwitchSettingsEntry(
                title = stringResource(R.string.resume_playback),
                text = stringResource(R.string.resume_playback_description),
                isChecked = resumePlaybackWhenDeviceConnected,
                onCheckedChange = {
                    resumePlaybackWhenDeviceConnected = it
                }
            )

            SwitchSettingsEntry(
                title = stringResource(R.string.stop_when_closed),
                text = stringResource(R.string.stop_when_closed_description),
                isChecked = stopWhenClosed,
                onCheckedChange = { stopWhenClosed = it }
            )
        }
        SettingsGroup(title = stringResource(R.string.audio)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.skip_silence),
                text = stringResource(R.string.skip_silence_description),
                isChecked = skipSilence,
                onCheckedChange = {
                    skipSilence = it
                }
            )

            val currentValue = { minimumSilence.toFloat() / 1000.0f }
            var initialValue by rememberSaveable { mutableFloatStateOf(currentValue()) }
            var changed by rememberSaveable { mutableStateOf(false) }

            AnimatedVisibility(visible = skipSilence) {
                Column {
                    SliderSettingsEntry(
                        title = stringResource(R.string.minimum_silence_length),
                        text = stringResource(R.string.minimum_silence_length_description),
                        initialValue = initialValue,
                        onSlide = { changed = it != initialValue },
                        onSlideCompleted = { minimumSilence = (it * 1000.0f).toLong() },
                        toDisplay = { stringResource(R.string.format_ms, it.toInt()) },
                        min = 1.00f,
                        max = 2000.000f
                    )

                    AnimatedVisibility(visible = changed) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SettingsDescription(
                                text = stringResource(R.string.minimum_silence_length_warning),
                                important = true,
                                modifier = Modifier.weight(2f)
                            )
                            SecondaryTextButton(
                                text = stringResource(R.string.restart_service),
                                onClick = {
                                    binder?.restartForegroundOrStop()?.let { changed = false }
                                    initialValue = currentValue()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 24.dp)
                            )
                        }
                    }
                }
            }

            SwitchSettingsEntry(
                title = stringResource(R.string.loudness_normalization),
                text = stringResource(R.string.loudness_normalization_description),
                isChecked = volumeNormalization,
                onCheckedChange = { volumeNormalization = it }
            )

            AnimatedVisibility(visible = volumeNormalization) {
                SliderSettingsEntry(
                    title = stringResource(R.string.loudness_base_gain),
                    text = stringResource(R.string.loudness_base_gain_description),
                    initialValue = volumeNormalizationBaseGain,
                    onSlideCompleted = { volumeNormalizationBaseGain = it },
                    toDisplay = { stringResource(R.string.format_db, "%.2f".format(it)) },
                    min = -20.00f,
                    max = 20.00f
                )
            }

            SwitchSettingsEntry(
                title = stringResource(R.string.bass_boost),
                text = stringResource(R.string.bass_boost_description),
                isChecked = bassBoost,
                onCheckedChange = { bassBoost = it }
            )

            AnimatedVisibility(visible = bassBoost) {
                SliderSettingsEntry(
                    title = stringResource(R.string.bass_boost_level),
                    text = stringResource(R.string.bass_boost_level_description),
                    initialValue = bassBoostLevel / 1000.0f,
                    onSlideCompleted = { bassBoostLevel = floor(it * 1000f).toInt() },
                    toDisplay = { floor(it * 1000f).toInt().toString() },
                    min = 0f,
                    max = 1f
                )
            }

            SettingsEntry(
                title = stringResource(R.string.equalizer),
                text = stringResource(R.string.equalizer_description),
                onClick = {
                    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder?.player?.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }

                    try {
                        activityResultLauncher.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        context.toast(context.getString(R.string.no_equalizer_installed))
                    }
                }
            )
        }
    }
}
