package com.androidtech.features.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidtech.base.R
import com.androidtech.base.theme.Body24Bold

@Preview
@Composable
private fun PreviewOnboarding() {
    FirstOnboarding(
        modifier = Modifier.fillMaxSize()
            .background(Color.White)
    )
}

@Composable
fun FirstOnboarding(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_onboarding_page1),
            contentDescription = "First onboarding"
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.text_onboarding_title_1),
            style = Body24Bold,
            color = Color.Black
        )
    }
}


@Composable
fun SecondOnboarding(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_onboarding_page2),
            contentDescription = "Second onboarding"
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.text_onboarding_title_2),
            style = Body24Bold,
            color = Color.Black
        )
    }
}


@Composable
fun ThirdOnboarding(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_onboarding_page3),
            contentDescription = "Third onboarding"
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.text_onboarding_title_3),
            style = Body24Bold,
            color = Color.Black
        )
    }
}