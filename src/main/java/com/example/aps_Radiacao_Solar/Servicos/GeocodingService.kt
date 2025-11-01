package com.example.aps_Radiacao_Solar.Servicos

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class Cidade(
    val nome: String,
    val pais: String,
    val estado: String?,
    val latitude: Double,
    val longitude: Double
) {
    fun getNomeCompleto(): String {
        return buildString {
            append(nome)
            if (!estado.isNullOrEmpty()) {
                append(", $estado")
            }
            append(" - $pais")
        }
    }
}

class GeocodingService {
    private val client = OkHttpClient()

    fun buscarCidade(
        nomeCidade: String,
        callback: (List<Cidade>?, String?) -> Unit
    ) {
        if (nomeCidade.isBlank()) {
            callback(null, "Por favor, digite o nome de uma cidade")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Codifica o nome da cidade para URL
                val cidadeCodificada = URLEncoder.encode(nomeCidade.trim(), "UTF-8")
                val url = "https://geocoding-api.open-meteo.com/v1/search?name=$cidadeCodificada&count=10&language=pt&format=json"

                Log.d("API_GEOCODING", "URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        callback(null, "Erro na conexão: ${response.code}")
                    }
                    return@launch
                }

                val dados = response.body?.string()
                Log.d("API_GEOCODING", "Resposta: $dados")

                withContext(Dispatchers.Main) {
                    try {
                        if (dados.isNullOrEmpty()) {
                            callback(null, "Resposta vazia do servidor")
                            return@withContext
                        }

                        val jsonObject = JSONObject(dados)

                        if (!jsonObject.has("results")) {
                            Log.d("API_GEOCODING", "Nenhum resultado encontrado")
                            callback(null, "❌ Nenhuma cidade encontrada com o nome '$nomeCidade'")
                            return@withContext
                        }

                        val results: JSONArray = jsonObject.getJSONArray("results")

                        if (results.length() == 0) {
                            callback(null, "❌ Nenhuma cidade encontrada")
                            return@withContext
                        }

                        val cidades = mutableListOf<Cidade>()

                        for (i in 0 until results.length()) {
                            try {
                                val item = results.getJSONObject(i)

                                val nome = item.optString("name", "Desconhecido")
                                val pais = item.optString("country", "")
                                val estado = item.optString("admin1", null)
                                val latitude = item.optDouble("latitude", 0.0)
                                val longitude = item.optDouble("longitude", 0.0)

                                Log.d("API_GEOCODING", "Cidade encontrada: $nome, $estado, $pais")

                                val cidade = Cidade(
                                    nome = nome,
                                    pais = pais,
                                    estado = estado,
                                    latitude = latitude,
                                    longitude = longitude
                                )
                                cidades.add(cidade)
                            } catch (e: Exception) {
                                Log.e("API_GEOCODING", "Erro ao processar cidade $i: ${e.message}")
                            }
                        }

                        if (cidades.isEmpty()) {
                            callback(null, "❌ Erro ao processar resultados")
                        } else {
                            Log.d("API_GEOCODING", "Total de cidades encontradas: ${cidades.size}")
                            callback(cidades, null)
                        }

                    } catch (e: Exception) {
                        Log.e("JSON_ERRO_GEOCODING", "Erro ao processar JSON: ${e.message}", e)
                        callback(null, "❌ Erro ao processar dados: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e("API_ERRO_GEOCODING", "Erro na requisição: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(null, "❌ Erro na conexão com o servidor")
                }
            }
        }
    }
}