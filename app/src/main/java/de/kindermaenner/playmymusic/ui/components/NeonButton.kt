package de.kindermaenner.playmymusic.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black
        ),
        modifier = modifier
            .padding(16.dp)
            .shadow(
                elevation = 24.dp,
                spotColor = Color(0xFF00FF66),
                ambientColor = Color(0xFF00FF66)
            )
            .border(
                width = 2.dp,
                color = Color(0xFF00FF66),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Text(
            text = text,
            color = Color(0xFF00FF66),
            fontSize = 20.sp
        )
    }
}