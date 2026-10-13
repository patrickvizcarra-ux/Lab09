
package com.example.lab09

import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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
        val cliente = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()


        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(cliente)
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
                    Text("JSONPlaceholder Access")
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
                    icon = {
                        Text("⌂")
                    },
                    label = {
                        Text("Inicio")
                    }
                )

                NavigationBarItem(
                    selected = rutaActual == "posts",
                    onClick = {
                        // Si existe la lista en la pila, vuelve a ella.
                        val regreso = navController.popBackStack(
                            route = "posts",
                            inclusive = false
                        )

                        // Si no existe, abre la lista.
                        if (!regreso) {
                            navController.navigate("posts") {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = {
                        Text("▤")
                    },
                    label = {
                        Text("Posts")
                    }
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
                        navController.navigate("posts") {
                            launchSingleTop = true
                        }
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

        Text("Consulta publicaciones desde una API REST.")

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

    var cargando by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var intento by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(intento) {

        cargando = true
        error = null

        Log.d("API_TEST", "Iniciando petición a /posts")

        try {
            val resultado = servicio.getUserPosts()
            listaPosts = resultado

            Log.d(
                "API_TEST",
                "Respuesta recibida: ${resultado.size} publicaciones"
            )

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            error = "${e.javaClass.simpleName}: ${
                e.message ?: "Error desconocido"
            }"

            Log.e("API_TEST", "Error real de la API", e)

        } finally {
            cargando = false
            Log.d("API_TEST", "Finalizó la petición")
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
                Text(
                    text = "Error al consultar la API",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(error ?: "Error desconocido")

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { intento++ }) {
                    Text("Reintentar")
                }
            }
        }

        listaPosts.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("La API no devolvió publicaciones.")

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = { intento++ }) {
                    Text("Volver a cargar")
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = listaPosts,
                    key = { post -> post.id }
                ) { post ->

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

    LaunchedEffect(id) {

        cargando = true
        error = null

        Log.d("API_TEST", "Consultando publicación $id")

        try {
            post = servicio.getUserPostById(id)

            Log.d("API_TEST", "Publicación $id recibida")

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {

            error = "${e.javaClass.simpleName}: ${
                e.message ?: "Error desconocido"
            }"

            Log.e("API_TEST", "Error al consultar publicación $id", e)

        } finally {
            cargando = false
            Log.d("API_TEST", "Finalizó consulta de publicación $id")
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
                Text(
                    text = "Error al cargar la publicación",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(error ?: "Error desconocido")
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
