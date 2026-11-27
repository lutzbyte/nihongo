package com.nihongo.app.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nihongo.app.R
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.PracticeSection
import com.nihongo.app.domain.model.StudyScope
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.ChoiceChips
import com.nihongo.app.ui.components.LevelSelector
import com.nihongo.app.ui.components.PrimaryButton
import com.nihongo.app.ui.theme.Spacing

@Composable
fun PracticeSetupScreen(
    mode: String,
    onBack: () -> Unit,
    onStartFlashcard: (StudyScope, JlptLevel) -> Unit,
    onStartQuiz: (JlptLevel, PracticeSection, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var level by rememberSaveable { mutableStateOf(JlptLevel.N5) }
    var scope by rememberSaveable { mutableStateOf(StudyScope.VOCABULARY) }
    var section by rememberSaveable { mutableStateOf(PracticeSection.MIXED) }

    val titleRes = when (mode) {
        "FLASHCARD" -> R.string.practice_mode_flashcard
        "EXAM" -> R.string.practice_mode_exam
        else -> R.string.practice_quiz_title
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(titleRes), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Label(stringResource(R.string.practice_choose_level))
            LevelSelector(selected = level, onSelect = { level = it })

            when (mode) {
                "FLASHCARD" -> {
                    Label(stringResource(R.string.setup_scope))
                    ChoiceChips(
                        options = listOf(
                            StudyScope.VOCABULARY to stringResource(R.string.category_vocabulary),
                            StudyScope.KANJI to stringResource(R.string.category_kanji),
                            StudyScope.GRAMMAR to stringResource(R.string.category_grammar),
                        ),
                        selected = scope,
                        onSelect = { scope = it },
                    )
                    Note(stringResource(R.string.setup_flashcard_info))
                }
                "EXAM" -> {
                    Note(stringResource(R.string.setup_exam_info, EXAM_MINUTES))
                    Note(stringResource(R.string.setup_reading_listening_note))
                }
                else -> {
                    Label(stringResource(R.string.setup_section))
                    ChoiceChips(
                        options = listOf(
                            PracticeSection.MOJI_GOI to stringResource(R.string.section_moji_goi),
                            PracticeSection.BUNPO to stringResource(R.string.section_bunpo),
                            PracticeSection.MIXED to stringResource(R.string.section_mixed),
                        ),
                        selected = section,
                        onSelect = { section = it },
                    )
                    Note(stringResource(R.string.setup_reading_listening_note))
                }
            }
        }

        PrimaryButton(
            text = stringResource(R.string.practice_start),
            onClick = {
                when (mode) {
                    "FLASHCARD" -> onStartFlashcard(scope, level)
                    "EXAM" -> onStartQuiz(level, PracticeSection.MIXED, "EXAM")
                    else -> onStartQuiz(level, section, "QUIZ")
                }
            },
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}