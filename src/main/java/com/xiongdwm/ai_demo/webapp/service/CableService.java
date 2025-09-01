package com.xiongdwm.ai_demo.webapp.service;

import java.util.List;
import java.util.Map;

import com.xiongdwm.ai_demo.webapp.entities.Fiber;

public interface CableService {

    List<Fiber> getCableByFromStationId(Long fromStationId);

    List<Fiber> getCableByFromStationName(String fromStationName);

    List<Fiber>getCableByToStationName(String toStationName);

    List<Fiber> getCableByStation(String stationName);

    List<Fiber> getCableBetweenStationUndirection(String sn1,String sn2);

    List<Fiber> getCableBetweenStationIndirection(String fsn,String tsn);

    Map<String, Long> getCableByDateRangeClassifyByType(String startDate, String endDate);
    
}
