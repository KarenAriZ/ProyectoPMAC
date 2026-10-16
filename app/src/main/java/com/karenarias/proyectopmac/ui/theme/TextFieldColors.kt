package com.karenarias.proyectopmac.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable

/** Los formularios usan superficies claras incluso cuando el teléfono está en modo oscuro. */
@Composable
fun metroGoTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PurpleDark,
    unfocusedTextColor = PurpleDark,
    disabledTextColor = PurpleDark,
    errorTextColor = PurpleDark,
    focusedContainerColor = CardWhite,
    unfocusedContainerColor = CardWhite,
    disabledContainerColor = CardWhite,
    errorContainerColor = CardWhite,
    focusedLabelColor = PurplePrimary,
    unfocusedLabelColor = PurplePrimary,
    disabledLabelColor = PurplePrimary,
    errorLabelColor = ErrorWine,
    focusedPlaceholderColor = PurplePrimary,
    unfocusedPlaceholderColor = PurplePrimary,
    disabledPlaceholderColor = PurplePrimary,
    errorPlaceholderColor = PurplePrimary,
    focusedSupportingTextColor = PurplePrimary,
    unfocusedSupportingTextColor = PurplePrimary,
    disabledSupportingTextColor = PurplePrimary,
    errorSupportingTextColor = ErrorWine,
    focusedBorderColor = PurplePrimary,
    unfocusedBorderColor = MauveAccent,
    disabledBorderColor = MauveAccent,
    errorBorderColor = ErrorWine,
    cursorColor = PurplePrimary,
    errorCursorColor = ErrorWine
)
