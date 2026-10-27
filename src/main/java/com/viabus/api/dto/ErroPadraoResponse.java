package com.viabus.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroPadraoResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String path,
        List<ErroCampoValidation> errosValidacao
) {

    public record ErroCampoValidation(
            String campo,
            String mensagem
    ) {
    }

    @JsonProperty("error")
    public String errorAlias() {
        return erro;
    }

    @JsonProperty("message")
    public String messageAlias() {
        return mensagem;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("errors")
    public Map<String, String> errorsAlias() {
        if (errosValidacao == null || errosValidacao.isEmpty()) {
            return null;
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (ErroCampoValidation item : errosValidacao) {
            map.put(item.campo(), item.mensagem());
        }
        return map;
    }
}
