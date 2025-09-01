package com.xiongdwm.ai_demo.webapp.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.xiongdwm.ai_demo.webapp.entities.Fiber;
import com.xiongdwm.ai_demo.webapp.entities.RoutePoint;
import com.xiongdwm.ai_demo.webapp.repository.FiberRepository;
import com.xiongdwm.ai_demo.webapp.repository.RoutePointRepository;
import com.xiongdwm.ai_demo.webapp.service.CableService;

import jakarta.annotation.Resource;

@Service
public class CableServiceImpl implements CableService {
    @Resource
    private FiberRepository fiberRepository;
    @Resource
    private RoutePointRepository routePointRepository;
    @Override
    public List<Fiber> getCableByFromStationId(Long fromStationId) {
        RoutePoint routePoint = routePointRepository.findById(fromStationId)
                .orElseThrow(() -> new RuntimeException("RoutePoint not found with id: " + fromStationId));
        return routePoint.getWhichAsFromStation().orElse(Collections.emptyList());
    }
    @Override
    public List<Fiber> getCableByFromStationName(String fromStationName) {
        RoutePoint routePoint = routePointRepository.findByName(fromStationName)
                .orElseThrow(() -> new RuntimeException("RoutePoint not found with name: " + fromStationName)).get(0);
                System.out.println(routePoint.toString());
        return routePoint.getWhichAsFromStation().orElse(Collections.emptyList());
    }
    @Override
    public List<Fiber> getCableByToStationName(String toStationName) {
        RoutePoint routePoint = routePointRepository.findByName(toStationName)
                .orElseThrow(() -> new RuntimeException("RoutePoint not found with name: " + toStationName)).get(0);
        return routePoint.getWhichAsToStation().orElse(Collections.emptyList());
    }
    @Override
    public List<Fiber> getCableByStation(String stationName) {
        RoutePoint routePoint = routePointRepository.findByName(stationName)
                .orElseThrow(() -> new RuntimeException("RoutePoint not found with name: " + stationName)).get(0);;
        List<Fiber> fibers = routePoint.getWhichAsFromStation().orElse(Collections.emptyList());
        fibers.addAll(routePoint.getWhichAsToStation().orElse(Collections.emptyList()));
        return fibers;
    }
    @Override
    public List<Fiber> getCableBetweenStationUndirection(String sn1, String sn2) {
        RoutePoint routePoint1 = routePointRepository.findByName(sn1)
                .orElseThrow(() -> new RuntimeException("RoutePoint not found with name: " + sn1)).get(0);
        RoutePoint routePoint2 = routePointRepository.findByName(sn2)
                .orElseThrow(() -> new RuntimeException("RoutePoint not found with name: " + sn2)).get(0);
        Specification<Fiber> spec = (root, query, criteriaBuilder) -> {
            return criteriaBuilder.or(
                criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("fromStation"), routePoint1),
                    criteriaBuilder.equal(root.get("toStation"), routePoint2)
                ),
                criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("fromStation"), routePoint2),
                    criteriaBuilder.equal(root.get("toStation"), routePoint1)
                )
            );
        };
        return fiberRepository.findAll(spec);
    }

    @Override
    public List<Fiber> getCableBetweenStationIndirection(String fsn, String tsn) {
        RoutePoint routePoint1 = routePointRepository.findByName(fsn)
                .orElseThrow(() -> new RuntimeException("RoutePoint not found with name: " + fsn)).get(0);
        RoutePoint routePoint2 = routePointRepository.findByName(tsn)
                .orElseThrow(() -> new RuntimeException("RoutePoint not found with name: " + tsn)).get(0);
        return fiberRepository.findByFromStationAndToStation(routePoint1, routePoint2);
    }
    @Override
    public Map<String, Long> getCableByDateRangeClassifyByType(String startDate, String endDate) {
        
        return new HashMap<>();
    }

    
    
}
