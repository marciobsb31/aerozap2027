package br.com.aerozap.conversationai.interpretacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.conversationai.interpretacao.extracao.Aeroportos;
import br.com.aerozap.conversationai.interpretacao.extracao.DatasEncontradas;
import br.com.aerozap.conversationai.interpretacao.extracao.ExtratorDeDatas;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Extração de datas")
class ExtratorDeDatasTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 6, 15);

    private DatasEncontradas extrair(String texto) {
        return ExtratorDeDatas.extrair(Aeroportos.normalizar(texto), HOJE);
    }

    @Test
    @DisplayName("ano omitido vira a próxima ocorrência, nunca o passado")
    void anoOmitidoVaiParaOFuturo() {
        assertThat(extrair("em 10/07").ida()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(extrair("em 10/01").ida()).isEqualTo(LocalDate.of(2027, 1, 10));
    }

    @Test
    @DisplayName("mês sem dia não vira data")
    void mesSemDiaNaoViraData() {
        DatasEncontradas datas = extrair("quero viajar em julho");

        assertThat(datas.datas()).isEmpty();
        assertThat(datas.mesSemDia()).isTrue();
    }

    @Test
    @DisplayName("dia solto se resolve pela data anterior: é assim que se diz a volta")
    void diaSoltoUsaADataAnterior() {
        DatasEncontradas datas = extrair("vou em 10/07 e volto dia 20");

        assertThat(datas.ida()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(datas.volta()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(datas.pediuVolta()).isTrue();
    }

    @Test
    @DisplayName("dia solto menor que a ida cai no mês seguinte")
    void diaSoltoMenorViraMesSeguinte() {
        assertThat(extrair("ida 28/07 e volta dia 03").volta()).isEqualTo(LocalDate.of(2026, 8, 3));
    }

    @Test
    @DisplayName("dia solto sem data anterior é ambíguo demais e não vira nada")
    void diaSoltoSozinhoNaoViraData() {
        assertThat(extrair("quero viajar dia 20").datas()).isEmpty();
    }

    @Test
    @DisplayName("data impossível é ignorada, não quebra")
    void dataImpossivelNaoQuebra() {
        assertThat(extrair("em 31/02").datas()).isEmpty();
    }

    @Test
    @DisplayName("próxima sexta é a sexta seguinte, não hoje")
    void proximaSexta() {
        assertThat(extrair("na próxima sexta").ida()).isEqualTo(LocalDate.of(2026, 6, 19));
    }
}
