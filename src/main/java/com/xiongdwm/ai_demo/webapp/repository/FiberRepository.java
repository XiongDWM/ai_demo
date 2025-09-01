package com.xiongdwm.ai_demo.webapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.ai_demo.webapp.entities.Fiber;
import com.xiongdwm.ai_demo.webapp.entities.RoutePoint;

@Repository
public interface FiberRepository extends JpaRepository<Fiber,Long>, JpaSpecificationExecutor<Fiber> {
    List<Fiber> findByFromStationAndToStation(RoutePoint fromStation, RoutePoint toStation);

    
}
