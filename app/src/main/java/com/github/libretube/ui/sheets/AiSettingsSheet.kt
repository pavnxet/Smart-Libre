package com.github.libretube.ui.sheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.github.libretube.databinding.DialogAiSettingsBinding
import com.github.libretube.helpers.AiChaptersService
import com.github.libretube.helpers.TranscriptHelper
import com.github.libretube.helpers.TursoSyncService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AiSettingsSheet : BottomSheetDialogFragment() {
    private var _binding: DialogAiSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAiSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        val aiPrefs = context.getSharedPreferences("smart_chapters_ai_prefs", Context.MODE_PRIVATE)
        val tursoPrefs = context.getSharedPreferences("smart_chapters_turso_prefs", Context.MODE_PRIVATE)

        val currentProvider = aiPrefs.getString(AiChaptersService.KEY_AI_PROVIDER, "aikit") ?: "aikit"
        if (currentProvider == "openrouter") {
            binding.rbOpenrouter.isChecked = true
        } else {
            binding.rbAikit.isChecked = true
        }

        val savedTranscriptKey = aiPrefs.getString(TranscriptHelper.KEY_TRANSCRIPT_API_KEY, TranscriptHelper.DEFAULT_KEY)
        binding.etTranscriptApiKey.setText(savedTranscriptKey)

        binding.etAiToken.setText(aiPrefs.getString(AiChaptersService.KEY_AI_TOKEN, ""))
        binding.etAiModel.setText(aiPrefs.getString(AiChaptersService.KEY_AI_MODEL, ""))
        binding.swSoundEnabled.isChecked = aiPrefs.getBoolean(AiChaptersService.KEY_SOUND_ENABLED, true)

        binding.etTursoUrl.setText(tursoPrefs.getString(TursoSyncService.KEY_TURSO_URL, ""))
        binding.etTursoToken.setText(tursoPrefs.getString(TursoSyncService.KEY_TURSO_TOKEN, ""))

        val currentSbUserId = com.github.libretube.helpers.PreferenceHelper.getSponsorBlockUserID()
        binding.etSbUserId.setText(currentSbUserId)

        binding.btnSaveSettings.setOnClickListener {
            val provider = if (binding.rbOpenrouter.isChecked) "openrouter" else "aikit"
            val transcriptApiKey = binding.etTranscriptApiKey.text?.toString().orEmpty().trim()
            val aiToken = binding.etAiToken.text?.toString().orEmpty().trim()
            val aiModel = binding.etAiModel.text?.toString().orEmpty().trim()
            val soundEnabled = binding.swSoundEnabled.isChecked

            val tursoUrl = binding.etTursoUrl.text?.toString().orEmpty().trim()
            val tursoToken = binding.etTursoToken.text?.toString().orEmpty().trim()

            val customSbUserId = binding.etSbUserId.text?.toString().orEmpty().trim()
            if (customSbUserId.isNotEmpty()) {
                com.github.libretube.helpers.PreferenceHelper.putString(
                    com.github.libretube.constants.PreferenceKeys.SB_USER_ID,
                    customSbUserId
                )
            }

            aiPrefs.edit()
                .putString(TranscriptHelper.KEY_TRANSCRIPT_API_KEY, transcriptApiKey)
                .putString(AiChaptersService.KEY_AI_PROVIDER, provider)
                .putString(AiChaptersService.KEY_AI_TOKEN, aiToken)
                .putString(AiChaptersService.KEY_AI_MODEL, aiModel)
                .putBoolean(AiChaptersService.KEY_SOUND_ENABLED, soundEnabled)
                .apply()

            tursoPrefs.edit()
                .putString(TursoSyncService.KEY_TURSO_URL, tursoUrl)
                .putString(TursoSyncService.KEY_TURSO_TOKEN, tursoToken)
                .apply()

            Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}