package de.kindermaenner.playmymusic.ui.state

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

val UiStateIdKey = SemanticsPropertyKey<String>("ui_state_id")

var SemanticsPropertyReceiver.stateId by UiStateIdKey