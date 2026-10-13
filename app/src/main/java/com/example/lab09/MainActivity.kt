
package com.example.lab09

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lab09.ui.theme.Lab09Theme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Lab09Theme {
                AppPrincipal()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPrincipal() {
    val navController = rememberNavController()

    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://json-placeholder.mock.beeceptor.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val servicio = remember(retrofit) {
        retrofit.create(PostApiService::class.java)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "JSONPlaceholder Access",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = rutaActual == "inicio",
                    onClick = {
                        navController.navigate("inicio") {
                            popUpTo("inicio") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    icon = { Text("⌂") },
                    label = { Text("Inicio") }
                )

                NavigationBarItem(
                    selected = rutaActual == "posts",
                    onClick = {
                        navController.navigate("posts") {
                            launchSingleTop = true
                        }
                    },
                    icon = { Text("▤") },
                    label = { Text("Posts") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "inicio",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("inicio") {
                ScreenInicio(
                    onVerPosts = {
                        navController.navigate("posts")
                    }
                )
            }

            composable("posts") {
                ScreenPosts(
                    navController = navController,
                    servicio = servicio
                )
            }

            composable(
                route = "postsVer/{id}",
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.IntType
                    }
                )
            ) { entrada ->
                val id = entrada.arguments?.getInt("id") ?: 1

                ScreenPost(
                    id = id,
                    servicio = servicio
                )
            }
        }
    }
}

@Composable
fun ScreenInicio(onVerPosts: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bienvenido",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Consulta publicaciones desde una API REST."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onVerPosts) {
            Text("Ver publicaciones")
        }
    }
}

@Composable
fun ScreenPosts(
    navController: NavHostController,
    servicio: PostApiService
) {
    var listaPosts by remember {
        mutableStateOf<List<PostModel>>(emptyList())
    }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(servicio) {
        cargando = true
        error = null

        try {
            listaPosts = servicio.getUserPosts()
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las publicaciones."
        } finally {
            cargando = false
        }
    }

    when {
        cargando -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cargando publicaciones...")
            }
        }

        error != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Error al consultar la API")
                Spacer(modifier = Modifier.height(8.dp))
                Text(error ?: "")
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    navController.navigate("posts") {
                        popUpTo("posts") {
                            inclusive = true
                        }
                    }
                }) {
                    Text("Reintentar")
                }
            }
        }

        listaPosts.isEmpty() -> {
            Text(
                text = "La API no devolvió publicaciones.",
                modifier = Modifier.padding(24.dp)
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listaPosts, key = { it.id }) { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${post.id}. ${post.title}",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = post.body,
                                    maxLines = 3,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Button(
                                onClick = {
                                    navController.navigate(
                                        "postsVer/${post.id}"
                                    )
                                }
                            ) {
                                Text("Ver")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenPost(
    id: Int,
    servicio: PostApiService
) {
    var post by remember(id) {
        mutableStateOf<PostModel?>(null)
    }
    var cargando by remember(id) {
        mutableStateOf(true)
    }
    var error by remember(id) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(id, servicio) {
        cargando = true
        error = null

        try {
            post = servicio.getUserPostById(id)
        } catch (e: Exception) {
            error = e.message ?: "No se pudo obtener la publicación."
        } finally {
            cargando = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            cargando -> {
                CircularProgressIndicator()
                Text("Cargando publicación...")
            }

            error != null -> {
                Text("Error: ${error}")
            }

            post != null -> {
                Text(
                    text = "Detalle de la publicación",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text("ID: ${post!!.id}")
                Text("Usuario: ${post!!.userId}")

                Text(
                    text = post!!.title,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = post!!.body,
                    style = MaterialTheme.typography.bodyLarge
                )

                post!!.link?.let {
                    Text("Enlace: $it")
                }

                post!!.commentCount?.let {
                    Text("Comentarios: $it")
                }
            }
        }
    }
}
