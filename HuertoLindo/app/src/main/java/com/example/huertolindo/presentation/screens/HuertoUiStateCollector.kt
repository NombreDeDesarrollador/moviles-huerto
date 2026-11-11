package com.example.huertolindo.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.huertolindo.presentation.state.HuertoUiState
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel

@Composable
internal fun collectHuertoUiState(viewModel: HuertoViewModel): HuertoUiState =
    viewModel.uiState.collectAsState().value
