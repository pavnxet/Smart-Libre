package com.github.libretube.ui.sheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import com.github.libretube.databinding.DialogAiSettingsBinding
import com.github.libretube.helpers.AiChaptersService
import com.github.libretube.helpers.SoundHelper
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

        val currentProvider = aiPrefs.getString(AiChaptersService.KEY_AI_PROVIDER, AiChaptersService.PROVIDER_AIKIT) ?: AiChaptersService.PROVIDER_AIKIT
        when (currentProvider) {
            AiChaptersService.PROVIDER_OPENROUTER -> binding.rbOpenrouter.isChecked = true
            AiChaptersService.PROVIDER_APIBEAM -> binding.rbApibeam.isChecked = true
            else -> binding.rbAikit.isChecked = true
        }

        fun updateProviderUi(provider: String) {
            val isApiBeam = provider == AiChaptersService.PROVIDER_APIBEAM
            val isOpenRouter = provider == AiChaptersService.PROVIDER_OPENROUTER

            binding.tilApibeamUrl.isVisible = isApiBeam
            if (isApiBeam) {
                binding.tilAiToken.hint = "AI API Token (Optional for ApiBeam)"
                binding.tilAiModel.hint = "Model (default: gpt-4)"
            } else if (isOpenRouter) {
                binding.tilAiToken.hint = "OpenRouter API Key (sk-or-...)"
                binding.tilAiModel.hint = "Model (default: google/gemma-4-31b-it:free)"
            } else {
                binding.tilAiToken.hint = "AI API Token / Key"
                binding.tilAiModel.hint = "Model (default: qwen3.8-max)"
            }
        }

        updateProviderUi(currentProvider)

        binding.rgAiProvider.setOnCheckedChangeListener { _, checkedId ->
            val selectedProvider = when (checkedId) {
                binding.rbOpenrouter.id -> AiChaptersService.PROVIDER_OPENROUTER
                binding.rbApibeam.id -> AiChaptersService.PROVIDER_APIBEAM
                else -> AiChaptersService.PROVIDER_AIKIT
            }
            updateProviderUi(selectedProvider)
        }

        val savedTranscriptKey = aiPrefs.getString(TranscriptHelper.KEY_TRANSCRIPT_API_KEY, TranscriptHelper.DEFAULT_KEY)
        binding.etTranscriptApiKey.setText(savedTranscriptKey)

        binding.etApibeamUrl.setText(aiPrefs.getString(AiChaptersService.KEY_APIBEAM_URL, AiChaptersService.DEFAULT_APIBEAM_URL))
        binding.etAiToken.setText(aiPrefs.getString(AiChaptersService.KEY_AI_TOKEN, ""))
        binding.etAiModel.setText(aiPrefs.getString(AiChaptersService.KEY_AI_MODEL, ""))
        binding.swSoundEnabled.isChecked = aiPrefs.getBoolean(AiChaptersService.KEY_SOUND_ENABLED, true)

        binding.btnTestSound.setOnClickListener {
            SoundHelper.playAnimeWow(requireContext())
            Toast.makeText(context, "Playing Anime Wow sound! 🔊", Toast.LENGTH_SHORT).show()
        }

        binding.etTursoUrl.setText(tursoPrefs.getString(TursoSyncService.KEY_TURSO_URL, ""))
        binding.etTursoToken.setText(tursoPrefs.getString(TursoSyncService.KEY_TURSO_TOKEN, ""))

        val currentSbUserId = com.github.libretube.helpers.PreferenceHelper.getSponsorBlockUserID()
        binding.etSbUserId.setText(currentSbUserId)

        binding.btnSaveSettings.setOnClickListener {
            val provider = when {
                binding.rbOpenrouter.isChecked -> AiChaptersService.PROVIDER_OPENROUTER
                binding.rbApibeam.isChecked -> AiChaptersService.PROVIDER_APIBEAM
                else -> AiChaptersService.PROVIDER_AIKIT
            }
            val transcriptApiKey = binding.etTranscriptApiKey.text?.toString().orEmpty().trim()
            val apibeamUrl = binding.etApibeamUrl.text?.toString().orEmpty().trim()
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
                .putString(AiChaptersService.KEY_APIBEAM_URL, apibeamUrl)
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