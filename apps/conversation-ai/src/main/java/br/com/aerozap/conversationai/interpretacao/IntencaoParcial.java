package br.com.aerozap.conversationai.interpretacao;

import br.com.aerozap.contracts.search.CabinClass;
import br.com.aerozap.contracts.search.Passengers;
import br.com.aerozap.contracts.search.SearchIntent;
import java.time.LocalDate;
import java.util.Optional;

/**
 * O que já se sabe da pesquisa enquanto ainda falta alguma coisa.
 *
 * <p>Existe porque {@link SearchIntent} é — de propósito — rígido: ou está completo e válido, ou
 * não existe. O meio do caminho precisa de outro tipo, senão a tentação é afrouxar o contrato
 * compartilhado para caber um rascunho, e aí todo serviço passa a lidar com pesquisa pela metade.
 *
 * @param origem IATA, quando já se sabe
 * @param destino IATA, quando já se sabe
 * @param ida data de ida, quando já se sabe
 * @param volta data de volta, quando houver
 * @param passageiros quem viaja; um adulto quando a pessoa não disse
 * @param cabine classe, quando a pessoa disse
 * @param querVolta verdadeiro quando a pessoa pediu ida e volta sem dar a data da volta
 */
public record IntencaoParcial(
        String origem,
        String destino,
        LocalDate ida,
        LocalDate volta,
        Passengers passageiros,
        CabinClass cabine,
        boolean querVolta) {

    public static IntencaoParcial vazia() {
        return new IntencaoParcial(null, null, null, null, null, null, false);
    }

    public IntencaoParcial comOrigem(String origem) {
        return new IntencaoParcial(origem, destino, ida, volta, passageiros, cabine, querVolta);
    }

    public IntencaoParcial comDestino(String destino) {
        return new IntencaoParcial(origem, destino, ida, volta, passageiros, cabine, querVolta);
    }

    public IntencaoParcial comDatas(LocalDate ida, LocalDate volta) {
        return new IntencaoParcial(origem, destino, ida, volta, passageiros, cabine, querVolta);
    }

    public IntencaoParcial comPassageiros(Passengers passageiros) {
        return new IntencaoParcial(origem, destino, ida, volta, passageiros, cabine, querVolta);
    }

    public IntencaoParcial comCabine(CabinClass cabine) {
        return new IntencaoParcial(origem, destino, ida, volta, passageiros, cabine, querVolta);
    }

    public IntencaoParcial comQuerVolta(boolean querVolta) {
        return new IntencaoParcial(origem, destino, ida, volta, passageiros, cabine, querVolta);
    }

    /** O primeiro campo essencial que falta, na ordem em que se pergunta. */
    public Optional<CampoFaltante> primeiroCampoFaltante() {
        if (destino == null) {
            return Optional.of(CampoFaltante.DESTINO);
        }
        if (origem == null) {
            return Optional.of(CampoFaltante.ORIGEM);
        }
        if (ida == null) {
            return Optional.of(CampoFaltante.DATA_DE_IDA);
        }
        if (querVolta && volta == null) {
            return Optional.of(CampoFaltante.DATA_DE_VOLTA);
        }
        return Optional.empty();
    }

    /** Converte para o contrato compartilhado. Só chame quando não falta nada. */
    public SearchIntent paraSearchIntent(String idioma, String moeda) {
        if (primeiroCampoFaltante().isPresent()) {
            throw new IllegalStateException("intenção incompleta: falta " + primeiroCampoFaltante().get());
        }
        return new SearchIntent(origem, destino, ida, volta,
                passageiros != null ? passageiros : Passengers.umAdulto(),
                cabine, moeda, null, null, null, idioma);
    }
}
