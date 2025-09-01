package com.xiongdwm.ai_demo.webapp.resource;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.webapp.entities.Fiber;
import com.xiongdwm.ai_demo.webapp.service.CableService;


@RestController
public class FiberController {
    @Autowired
    private CableService fiberService;

    @PostMapping("/fiber/getFiberByFromStationId")
    public ApiResponse<List<Fiber>> getFiberByFromStationId(@RequestParam("fromStationId")Long fromStationId) {
        List<Fiber> fibers = fiberService.getCableByFromStationId(fromStationId);
        return ApiResponse.success(fibers);
    }

    @PostMapping("/fiber/getFiberByFromStationName")
    public ApiResponse<List<Fiber>> getFiberByFromStationName(@RequestParam("fromStationName")String fromStationName) {
        List<Fiber> fibers = fiberService.getCableByFromStationName(fromStationName);
        return ApiResponse.success(fibers);
    }
    @PostMapping("/fiber/getFiberBetweenStationUndirection")
    public ApiResponse<List<Fiber>> getFiberBetweenStationUndirection(
            @RequestParam("station1") String sn1, @RequestParam("station2") String sn2) {
        List<Fiber> fibers = fiberService.getCableBetweenStationUndirection(sn1, sn2);
        return ApiResponse.success(fibers);
    }
    
    @PostMapping("/fiber/getFiberBetween")
    public ApiResponse<List<Fiber>> getFiberBetween(
            @RequestParam("fromStationName") String fromStationName,
            @RequestParam("toStationName") String toStationName) {
        List<Fiber> fibers = fiberService.getCableBetweenStationIndirection(fromStationName, toStationName);
        return ApiResponse.success(fibers);
    }
    
}
