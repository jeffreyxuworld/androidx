/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.foundation

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.foundation.selection.selectable
import androidx.ui.layout.Stack
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertIsInMutuallyExclusiveGroup
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertIsUnselected
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.doPartialGesture
import androidx.ui.test.find
import androidx.ui.test.findAll
import androidx.ui.test.findByText
import androidx.ui.test.first
import androidx.ui.test.isInMutuallyExclusiveGroup
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendDown
import androidx.ui.test.sendUp
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SelectableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectable_defaultSemantics() {
        composeTestRule.setContent {
            Text(
                "Text in item",
                modifier = Modifier.selectable(selected = true, onClick = {})
            )
        }

        findAll(isInMutuallyExclusiveGroup())
            .assertCountEquals(1)
            .first()
            .assertIsInMutuallyExclusiveGroup()
            .assertIsSelected()
    }

    @Test
    fun selectable_defaultClicks() {
        composeTestRule.setContent {
            val (selected, onSelected) = state { false }
            Text(
                "Text in item",
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = { onSelected(!selected) }
                )
            )
        }

        find(isInMutuallyExclusiveGroup())
            .assertIsUnselected()
            .doClick()
            .assertIsSelected()
            .doClick()
            .assertIsUnselected()
    }

    @Test
    fun selectable_noClicksNoChanges() {
        composeTestRule.setContent {
            val (selected, _) = state { false }
            Text(
                "Text in item",
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = {})
            )
        }

        find(isInMutuallyExclusiveGroup())
            .assertIsUnselected()
            .doClick()
            .assertIsUnselected()
    }

    @Test
    fun selectableTest_interactionState() {
        val interactionState = InteractionState()

        composeTestRule.setContent {
            Stack {
                Box(Modifier.selectable(
                    selected = true,
                    interactionState = interactionState,
                    onClick = {}
                )) {
                    Text("SelectableText")
                }
            }
        }

        runOnIdleCompose {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        findByText("SelectableText")
            .doPartialGesture { sendDown(center) }

        runOnIdleCompose {
            Truth.assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        findByText("SelectableText")
            .doPartialGesture { sendUp() }

        runOnIdleCompose {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }

    @Test
    fun selectableTest_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        var emitSelectableText by mutableStateOf(true)

        composeTestRule.setContent {
            Stack {
                if (emitSelectableText) {
                    Box(Modifier.selectable(
                        selected = true,
                        interactionState = interactionState,
                        onClick = {}
                    )) {
                        Text("SelectableText")
                    }
                }
            }
        }

        runOnIdleCompose {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        findByText("SelectableText")
            .doPartialGesture { sendDown(center) }

        runOnIdleCompose {
            Truth.assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        // Dispose selectable
        runOnIdleCompose {
            emitSelectableText = false
        }

        runOnIdleCompose {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }
}