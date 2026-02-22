package fr.uge.android.forkeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.uge.android.forkeat.designsystem.theme.ForkEatTheme
import fr.uge.android.forkeat.recipes.RecipesViewModel
import androidx.activity.viewModels
import fr.uge.android.forkeat.MainContent

class MainActivity : ComponentActivity() {
    private val recipesViewModel: RecipesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForkEatTheme {
                MainContent(recipesViewModel)
            }
        }
    }
}
