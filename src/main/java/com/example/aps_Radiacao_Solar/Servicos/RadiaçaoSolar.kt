package com.example.aps_Radiacao_Solar.Servicos

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RadiaçaoSolar {
    private val client = OkHttpClient()

    fun TaxaDeRadiacao(latitude: Double?, longitude: Double?, callback: (List<Pair<String, Double?>>?, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("https://satellite-api.open-meteo.com/v1/archive?latitude=$latitude&longitude=$longitude&hourly=shortwave_radiation&models=satellite_radiation_seamless&timezone=auto&past_days=1")
                    .build()

//                val request = Request.Builder()
//                    .url("https://satellite-api.open-meteo.com/v1/archive?latitude=-23.950655&longitude=-46.2025592&hourly=shortwave_radiation&models=satellite_radiation_seamless&timezone=auto&past_days=1")
//                    .build()


                val response = client.newCall(request).execute()
                val dados = response.body?.string()

                withContext(Dispatchers.Main) {
                    Log.d("API_SOLAR", dados ?: "Nenhum dado")

                    try {
                        if (dados != null) {
                            val jsonObject = JSONObject(dados)
                            val solar = jsonObject.getJSONObject("hourly")
                                .getJSONArray("shortwave_radiation")
                            val datas = jsonObject.getJSONObject("hourly").getJSONArray("time")
                            Log.d("API_SOLAR", "$solar")

                            val dadosCombinados = (0 until solar.length()).map { i ->
                                datas.getString(i) to if (solar.isNull(i)) null else solar.getDouble(
                                    i
                                )
                            }
                            Log.d("API_SOLAR", "$dadosCombinados")

                            // Chama o callback com sucesso
                            callback(dadosCombinados, null)
                        } else {
                            callback(null, "Dados vazios da API")
                        }
                    } catch (e: Exception) {
                        Log.e("JSON_ERRO_solar", "Erro solar ${e.message}")
                        callback(null, "Erro ao processar dados: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e("API_ERRO", "Erro: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback(null, "Erro na requisição: ${e.message}")
                }
            }
        }
    }
}