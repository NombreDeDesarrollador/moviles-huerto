package com.example.huertolindo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.huertolindo.viewmodel.HuertoUiState
import com.example.huertolindo.viewmodel.HuertoViewModel

@Composable
internal fun collectHuertoUiState(viewModel: HuertoViewModel): HuertoUiState =
    viewModel.uiState.collectAsState().value
