package com.example.aps_Radiacao_Solar.Adapters

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.example.aps_Radiacao_Solar.R
import com.example.aps_Radiacao_Solar.Servicos.Cidade

class CidadeAdapter(
    context: Context,
    private val onSearchCallback: (String, (List<Cidade>?) -> Unit) -> Unit
) : ArrayAdapter<Cidade>(context, 0), Filterable {

    private var cidadesFiltradasList: List<Cidade> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun getCount(): Int = cidadesFiltradasList.size

    override fun getItem(position: Int): Cidade? {
        return if (position < cidadesFiltradasList.size) {
            cidadesFiltradasList[position]
        } else null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_sugestao_cidade,
            parent,
            false
        )

        val cidade = getItem(position)

        val textoNome = view.findViewById<TextView>(R.id.texto_nome_cidade)
        val textoLocalizacao = view.findViewById<TextView>(R.id.texto_localizacao_cidade)

        cidade?.let {
            textoNome.text = "📍 ${it.nome}"

            val localizacao = buildString {
                if (!it.estado.isNullOrEmpty()) {
                    append(it.estado)
                    append(", ")
                }
                append(it.pais)
            }
            textoLocalizacao.text = localizacao
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                if (constraint.isNullOrBlank() || constraint.length < 2) {
                    results.values = emptyList<Cidade>()
                    results.count = 0
                    return results
                }

                Log.d("CIDADE_ADAPTER", "Buscando: $constraint")

                // Cancela busca anterior se houver
                searchRunnable?.let { handler.removeCallbacks(it) }

                // Espera 300ms antes de buscar (debounce)
                val countDownLatch = java.util.concurrent.CountDownLatch(1)
                var resultadoCidades: List<Cidade>? = null

                searchRunnable = Runnable {
                    onSearchCallback(constraint.toString()) { cidades ->
                        resultadoCidades = cidades
                        Log.d("CIDADE_ADAPTER", "Encontradas: ${cidades?.size ?: 0} cidades")
                        countDownLatch.countDown()
                    }
                }

                handler.postDelayed(searchRunnable!!, 300)

                // Aguarda a resposta da API (com timeout de 5 segundos)
                try {
                    val sucesso = countDownLatch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    if (!sucesso) {
                        Log.e("CIDADE_ADAPTER", "Timeout ao buscar cidades")
                    }
                } catch (e: InterruptedException) {
                    Log.e("CIDADE_ADAPTER", "Interrompido ao buscar cidades", e)
                }

                results.values = resultadoCidades ?: emptyList<Cidade>()
                results.count = resultadoCidades?.size ?: 0

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                cidadesFiltradasList = if (results != null && results.count > 0) {
                    @Suppress("UNCHECKED_CAST")
                    results.values as List<Cidade>
                } else {
                    emptyList()
                }

                Log.d("CIDADE_ADAPTER", "Publicando ${cidadesFiltradasList.size} resultados")

                if (cidadesFiltradasList.isNotEmpty()) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return if (resultValue is Cidade) {
                    resultValue.nome
                } else {
                    ""
                }
            }
        }
    }
}