package br.com.aerozap.conversationai.interpretacao.extracao;

import java.time.LocalDate;
import java.util.List;

/**
 * O que a frase disse sobre datas.
 *
 * @param datas datas completas, na ordem em que aparecem
 * @param mesSemDia verdadeiro quando a pessoa citou um mês sem dizer o dia ("em julho")
 * @param pediuVolta verdadeiro quando a frase indica ida e volta
 */
public record DatasEncontradas(List<LocalDate> datas, boolean mesSemDia, boolean pediuVolta) {

    public DatasEncontradas {
        datas = datas == null ? List.of() : List.copyOf(datas);
    }

    public LocalDate ida() {
        return datas.isEmpty() ? null : datas.get(0);
    }

    public LocalDate volta() {
        return datas.size() < 2 ? null : datas.get(1);
    }
}
