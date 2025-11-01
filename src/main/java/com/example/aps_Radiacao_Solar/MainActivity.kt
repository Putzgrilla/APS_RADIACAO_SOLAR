package com.example.aps_Radiacao_Solar

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.aps_Radiacao_Solar.Adapters.CidadeAdapter
import com.example.aps_Radiacao_Solar.Servicos.Cidade
import com.example.aps_Radiacao_Solar.Servicos.GeocodingService
import com.example.aps_Radiacao_Solar.Servicos.Gps
import com.example.aps_Radiacao_Solar.Servicos.RadiaçaoSolar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private lateinit var gps: Gps
    private lateinit var geocodingService: GeocodingService
    private lateinit var cidadeAdapter: CidadeAdapter

    private lateinit var statusTextView: TextView
    private lateinit var rodape: TextView
    private lateinit var campoPesquisaCidade: AutoCompleteTextView
    private lateinit var textoSugestao: TextView
    private lateinit var textoCidadeAtual: TextView
    private lateinit var valorRadiacaoAtual: TextView
    private lateinit var statusRadiacaoAtual: TextView
    private lateinit var valorRadiacaoMaxima: TextView
    private lateinit var statusRadiacaoMaxima: TextView
    private lateinit var horaRadiacaoMaxima: TextView

    private var latitudeAtual: Double? = null
    private var longitudeAtual: Double? = null
    private var cidadeAtual: String = "Localização"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.telaprincipal)

        inicializarComponentes()
        configurarAutoComplete()
        configurarListeners()
    }

    private fun inicializarComponentes() {
        gps = Gps(this)
        geocodingService = GeocodingService()

        rodape = findViewById(R.id.rodape_info)
        statusTextView = findViewById(R.id.status)
        campoPesquisaCidade = findViewById(R.id.campo_pesquisa_cidade)
        textoSugestao = findViewById(R.id.texto_sugestao)
        textoCidadeAtual = findViewById(R.id.texto_cidade_atual)
        valorRadiacaoAtual = findViewById(R.id.valor_radiacao_atual)
        statusRadiacaoAtual = findViewById(R.id.status_radiacao_atual)
        valorRadiacaoMaxima = findViewById(R.id.valor_radiacao_maxima)
        statusRadiacaoMaxima = findViewById(R.id.status_radiacao_maxima)
        horaRadiacaoMaxima = findViewById(R.id.hora_radiacao_maxima)
    }

    private fun configurarAutoComplete() {
        // Cria o adapter com callback para buscar cidades
        cidadeAdapter = CidadeAdapter(this) { query, callback ->
            geocodingService.buscarCidade(query) { cidades, erro ->
                callback(cidades)
            }
        }

        campoPesquisaCidade.setAdapter(cidadeAdapter)
        campoPesquisaCidade.threshold = 2 // Mínimo de 2 caracteres

        // Quando o usuário seleciona uma cidade da lista
        campoPesquisaCidade.setOnItemClickListener { parent, _, position, _ ->
            val cidadeSelecionada = parent.getItemAtPosition(position) as? Cidade
            cidadeSelecionada?.let {
                selecionarCidade(it)
            }
        }

        // TextWatcher para feedback visual
        campoPesquisaCidade.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s?.toString() ?: ""

                when {
                    texto.isEmpty() -> {
                        textoSugestao.text = "💡 Digite pelo menos 2 letras para ver sugestões"
                        textoSugestao.visibility = TextView.VISIBLE
                    }
                    texto.length == 1 -> {
                        textoSugestao.text = "✍️ Digite mais uma letra..."
                        textoSugestao.visibility = TextView.VISIBLE
                    }
                    texto.length >= 2 -> {
                        textoSugestao.text = "🔍 Buscando sugestões..."
                        textoSugestao.visibility = TextView.VISIBLE
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun configurarListeners() {
        // Botão localização atual
        findViewById<Button>(R.id.botao_localizacao_atual).setOnClickListener {
            usarLocalizacaoAtual()
        }

        // Botão atualizar
        findViewById<Button>(R.id.botao_atualizar).setOnClickListener {
            if (latitudeAtual != null && longitudeAtual != null) {
                buscarDadosRadiacao(latitudeAtual!!, longitudeAtual!!)
            } else {
                Toast.makeText(this, "⚠️ Selecione uma localização primeiro", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selecionarCidade(cidade: Cidade) {
        latitudeAtual = cidade.latitude
        longitudeAtual = cidade.longitude
        cidadeAtual = "📍 ${cidade.getNomeCompleto()}"

        textoCidadeAtual.text = cidadeAtual
        campoPesquisaCidade.setText("")
        textoSugestao.text = "✅ ${cidade.nome} selecionada!"

        Toast.makeText(this, "📍 ${cidade.nome} selecionada!", Toast.LENGTH_SHORT).show()

        // Busca dados automaticamente
        buscarDadosRadiacao(cidade.latitude, cidade.longitude)
    }

    private fun usarLocalizacaoAtual() {
        statusTextView.text = "📡 Obtendo sua localização..."
        limparDados()

        if (gps.checkLocationPermissions(this)) {
            gps.obterLocalizacao(this) { latitude, longitude, erro ->
                if (erro != null) {
                    statusTextView.text = "❌ $erro"
                    Toast.makeText(this, erro, Toast.LENGTH_LONG).show()
                    return@obterLocalizacao
                }

                if (latitude != null && longitude != null) {
                    latitudeAtual = latitude
                    longitudeAtual = longitude
                    cidadeAtual = "📍 Sua Localização Atual"
                    textoCidadeAtual.text = cidadeAtual

                    Toast.makeText(this, "✅ Localização obtida!", Toast.LENGTH_SHORT).show()

                    buscarDadosRadiacao(latitude, longitude)
                } else {
                    statusTextView.text = "❌ Não foi possível obter localização"
                }
            }
        } else {
            statusTextView.text = "⚠️ Solicitando permissões..."
            gps.solicitarPermissoes(this)
        }
    }

    private fun buscarDadosRadiacao(latitude: Double, longitude: Double) {
        statusTextView.text = "⏳ Carregando dados de radiação solar..."

        RadiaçaoSolar().TaxaDeRadiacao(latitude, longitude) { pairs, erro ->
            if (erro != null) {
                statusTextView.text = "❌ $erro"
                Toast.makeText(this, erro, Toast.LENGTH_LONG).show()
                return@TaxaDeRadiacao
            }

            if (pairs.isNullOrEmpty()) {
                statusTextView.text = "❌ Nenhum dado disponível para esta localização"
                Toast.makeText(this, "Sem dados disponíveis", Toast.LENGTH_SHORT).show()
                return@TaxaDeRadiacao
            }

            atualizarInterface(pairs)
            Toast.makeText(this, "✅ Dados atualizados!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun atualizarInterface(lista: List<Pair<String, Double?>>) {
        // Encontra o valor máximo
        val maior = lista.filter { it.second != null }.maxByOrNull { it.second!! }

        // Encontra o valor atual (último não nulo)
        val atual = lista.filter { it.second != null }.lastOrNull()

        // Atualiza radiação atual
        if (atual?.second == null || atual.second == 0.0) {
            valorRadiacaoAtual.text = "0"
            statusRadiacaoAtual.text = "🌙 Noite"
            statusRadiacaoAtual.setBackgroundColor(getColor(R.color.cinza_medio))
        } else {
            valorRadiacaoAtual.text = String.format("%.0f", atual.second)
            val status = medidorSolar(atual.second)
            statusRadiacaoAtual.text = status
            statusRadiacaoAtual.setBackgroundColor(getCorStatus(atual.second))
        }

        // Atualiza radiação máxima
        if (maior != null) {
            valorRadiacaoMaxima.text = String.format("%.0f W/m²", maior.second)
            statusRadiacaoMaxima.text = medidorSolar(maior.second)
            horaRadiacaoMaxima.text = formatardata(maior.first) ?: "--:--"
        } else {
            valorRadiacaoMaxima.text = "-- W/m²"
            statusRadiacaoMaxima.text = "--"
            horaRadiacaoMaxima.text = "--:--"
        }

        // Atualiza status e rodapé
        val atualhora = formatardata(atual?.first)
        statusTextView.text = "✅ Dados atualizados com sucesso"
        rodape.text = "Dados fornecidos pela Open-Meteo\nÚltima atualização: $atualhora"

        // Atualiza texto de sugestão
        textoSugestao.text = "💡 Digite para buscar outra cidade"
    }

    private fun limparDados() {
        valorRadiacaoAtual.text = "--"
        statusRadiacaoAtual.text = "--"
        valorRadiacaoMaxima.text = "-- W/m²"
        statusRadiacaoMaxima.text = "--"
        horaRadiacaoMaxima.text = "--:--"
    }

    private fun getCorStatus(valor: Double?): Int {
        return when {
            valor == null || valor == 0.0 -> getColor(R.color.cinza_medio)
            valor <= 200 -> getColor(R.color.verde_claro)
            valor <= 400 -> getColor(R.color.azul_claro)
            valor <= 600 -> getColor(R.color.amarelo_aviso)
            valor <= 800 -> getColor(R.color.roxo_claro)
            else -> getColor(R.color.vermelho_erro)
        }
    }

    private fun medidorSolar(valor: Double?): String {
        return when {
            valor == null || valor == 0.0 -> "Sem radiação"
            valor <= 200 -> "☀️ Muito Baixa"
            valor <= 400 -> "🌤️ Baixa"
            valor <= 600 -> "⛅ Moderada"
            valor <= 800 -> "🌞 Alta"
            valor <= 1000 -> "🔥 Muito Alta"
            else -> "🚨 Extrema"
        }
    }

    private fun formatardata(data: String?): String? {
        return try {
            val dateTime = LocalDateTime.parse(data)
            val hourFormatter = DateTimeFormatter.ofPattern("HH:mm")
            dateTime.format(hourFormatter)
        } catch (e: Exception) {
            "--:--"
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        gps.onRequestPermissionsResult(requestCode, grantResults)
    }
}