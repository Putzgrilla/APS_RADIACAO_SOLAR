package com.example.aps_Radiacao_Solar

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.aps_Radiacao_Solar.Servicos.Gps
import com.example.aps_Radiacao_Solar.Servicos.RadiaçaoSolar

import com.google.android.gms.location.FusedLocationProviderClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {


    private lateinit var gps: Gps
    private lateinit var statusTextView: TextView
    private lateinit var rodape: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.telaprincipal)



        gps = Gps(this)

        rodape = findViewById<TextView>(R.id.rodape_info)
        statusTextView = findViewById(R.id.status)
        buscardados()
        atualizar()
    }

    private fun formmatado(lista: List<Pair<String, Double?>>?): String {
        var texto: String
        var textoAtual: String
        var textorodape: String
        if (lista?.isNotEmpty() == true) {
            val maior = lista.filter { it.second != null }.maxByOrNull { it.second!! }
            val atual = lista.filter { it.second != null }.lastOrNull()

            var atualhora = formatardata(atual?.first)
            rodape.text =
                "\" Dados fornecidos pela Open-Meteo \n Última atualização: $atualhora"
            var maiorStatus = medidorSolar(maior?.second)
            if (atual?.second == null || atual.second == 0.0) {
                textoAtual = "Atual : Esta de noite"
            } else {
                var atualStatus = medidorSolar(atual.second)
                textoAtual = " Radiação atual é de ${atual.second}: $atualStatus "
            }
            texto = "Maior medição nas ultimas 24 horas: foi as  ${
                formatardata(
                    maior?.first
                )
            } de ${maior?.second}:$maiorStatus\n$textoAtual"
            return texto

        } else {
            texto = "Erro"
            return texto

        }


    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        gps.onRequestPermissionsResult(requestCode, grantResults)
    }

    fun buscardados() {
        statusTextView.text = "Obtendo localização..."
        if (gps.checkLocationPermissions(this)) {
            gps.obterLocalizacao(this) { latitude, longitude, erro ->
                RadiaçaoSolar().TaxaDeRadiacao(
                    latitude, longitude
                ) { pairs, string ->
                    var texto = formmatado(pairs)

                    statusTextView.text = texto

                }
            }
        } else {
            gps.solicitarPermissoes(this)
        }

    }

    fun atualizar() {
        var botao = findViewById<Button>(R.id.botao_atualizar)
        botao.setOnClickListener {

            buscardados()
        }

    }

    fun formatardata(data: String?): String? {
        val dateTime = LocalDateTime.parse(data)
        val hourFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val hourOnly = dateTime.format(hourFormatter)
        return hourOnly
    }

    fun medidorSolar(valor: Double?): String {

        val status = when {

            valor!! <= 200 -> "Muito Baixa"
            valor <= 400 -> "Baixa"
            valor <= 600 -> "Moderada"
            valor <= 800 -> "Alto"
            valor <= 1000 -> "Muito alto"
            else -> "fim do mundo corra"
        }
        return status


    }
}