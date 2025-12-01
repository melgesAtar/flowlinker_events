package com.flowlinker.events.service.mapper;

import java.util.Map;

/**
 * Interface funcional para mapeadores de eventos de atividade.
 */
@FunctionalInterface
public interface ActivityMapper {

    /**
     * Mapeia um evento para uma entrada de atividade.
     *
     * @param ctx contexto do evento
     * @return mapa com os dados da atividade ou null se não puder mapear
     */
    Map<String, Object> map(EventContext ctx);
}

