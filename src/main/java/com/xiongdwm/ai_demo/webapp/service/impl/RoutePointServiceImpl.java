package com.xiongdwm.ai_demo.webapp.service.impl;

import org.springframework.stereotype.Service;

import com.xiongdwm.ai_demo.webapp.entities.RoutePoint;
import com.xiongdwm.ai_demo.webapp.repository.RoutePointRepository;
import com.xiongdwm.ai_demo.webapp.service.RoutePointService;

import jakarta.annotation.Resource;

@Service
public class RoutePointServiceImpl implements RoutePointService{
    @Resource
    private RoutePointRepository routePointRepository;

    @Override
    public RoutePoint getRoutePointByName(String stationName) {

        return routePointRepository.findByName(stationName)
                .orElseThrow(() -> new RuntimeException("RoutePoint not found with name: " + stationName)).get(0);
    }   
    
}
