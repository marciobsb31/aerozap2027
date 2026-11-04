package br.com.aerozap.conversationai.interpretacao;

import br.com.aerozap.conversationai.interpretacao.extracao.Aeroportos;
import br.com.aerozap.conversationai.interpretacao.extracao.DatasEncontradas;
import br.com.aerozap.conversationai.interpretacao.extracao.ExtratorDeDatas;
import br.com.aerozap.conversationai.interpretacao.extracao.ExtratorDeLugares;
import br.com.aerozap.conversationai.interpretacao.extracao.ExtratorDePassageiros;
import br.com.aerozap.conversationai.interpretacao.extracao.Lugar;
import br.com.aerozap.contracts.search.Passengers;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interpretador v0: regras, dicionário e expressões regulares. Sem modelo de linguagem.
 *
 * <p>Existir antes da IA tem três motivos práticos. Ele é a <strong>linha de base</strong> contra a
 * qual o modelo vai ser comparado no mesmo conjunto dourado — sem isso, "a IA acerta 85%" não
 * quer dizer nada. Ele é o <strong>plano B</strong> de quando o provedor de LLM está fora, caro ou
 * lento (RF-045). E ele obriga a <strong>desenhar o contrato</strong> de interpretação agora:
 * pesquisa, pergunta ou recusa, nada além disso.
 *
 * <p>O que ele não faz, e é melhor dizer do que esconder: não entende pedido vago ("quero viajar
 * barato em julho"), não negocia datas flexíveis, não usa contexto de mensagens anteriores e não
 * resolve cidade fora do dicionário. Esses casos estão no conjunto dourado marcados como fora do
 * alcance do v0 — é o mapa do que a IA precisa ganhar.
 */
public class InterpretadorDeterministico implements Interpretador {

    private static final Logger log = LoggerFactory.getLogger(InterpretadorDeterministico.class);

    private static final double CONFIANCA_BASE = 0.95;
    private static final double DESCONTO_POR_ORDEM = 0.20;

    @Override
    public String nome() {
        return "deterministico-v0";
    }

    @Override
    public ResultadoDaInterpretacao interpretar(Enunciado enunciado, ContextoDaInterpretacao contexto) {
        String texto = Aeroportos.normalizar(enunciado.texto());

        Optional<String> sinal = GuardaDeInjecao.detectar(texto);
        if (sinal.isPresent()) {
            // Evento de segurança (RF-046): registra o padrão, nunca o texto recebido.
            log.warn("Mensagem recusada por tentativa de injeção; sinal={}", sinal.get());
            return new ResultadoDaInterpretacao.Recusa(MotivoDaRecusa.TENTATIVA_DE_INJECAO,
                    Perguntas.recusaDeInjecao(enunciado.idioma()));
        }

        List<Lugar> lugares = ExtratorDeLugares.extrair(texto);
        DatasEncontradas datas = ExtratorDeDatas.extrair(texto, contexto.hoje());
        Passengers passageiros = ExtratorDePassageiros.extrair(texto);

        if (lugares.isEmpty() && datas.datas().isEmpty() && !datas.mesSemDia()) {
            return new ResultadoDaInterpretacao.Recusa(MotivoDaRecusa.NAO_COMPREENDIDO,
                    Perguntas.naoEntendi(enunciado.idioma()));
        }

        IntencaoParcial parcial = IntencaoParcial.vazia()
                .comOrigem(ExtratorDeLugares.origem(lugares).orElse(null))
                .comDestino(ExtratorDeLugares.destino(lugares).orElse(null))
                .comDatas(datas.ida(), datas.volta())
                .comPassageiros(passageiros)
                .comCabine(ExtratorDePassageiros.cabine(texto).orElse(null))
                .comQuerVolta(datas.pediuVolta());

        Optional<CampoFaltante> faltante = parcial.primeiroCampoFaltante();
        if (faltante.isPresent()) {
            return new ResultadoDaInterpretacao.Pergunta(faltante.get(),
                    Perguntas.para(faltante.get(), enunciado.idioma()), parcial);
        }

        return new ResultadoDaInterpretacao.Pesquisa(
                parcial.paraSearchIntent(enunciado.idioma(), contexto.moedaPadrao()),
                confianca(lugares));
    }

    /**
     * Quanto dá para confiar nesta leitura. Cai quando origem e destino vieram só da ordem das
     * palavras, sem preposição que confirme — é o ponto em que o v0 mais erra, e quem consome
     * decide se confirma com a pessoa antes de gastar a consulta (RF-044).
     */
    private double confianca(List<Lugar> lugares) {
        boolean semPista = lugares.stream().allMatch(l -> l.papel() == Lugar.Papel.INDEFINIDO);
        return semPista && lugares.size() > 1 ? CONFIANCA_BASE - DESCONTO_POR_ORDEM : CONFIANCA_BASE;
    }
}
