package com.xiongdwm.ai_demo.webapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xiongdwm.ai_demo.webapp.entities.RoutePoint;

public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {

    Optional<List<RoutePoint>> findByName(String stationName);
    
} 
