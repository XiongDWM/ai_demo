package com.xiongdwm.ai_demo.multi_modal;

import org.springframework.stereotype.Component;

import com.xiongdwm.ai_demo.utils.GeometryUtils;

@Component
public class MultiModalService {
    
    public double calAngle(String coordsString){
        var parts = coordsString.split(";");
        var topCoords = parts[0].replace("top:", "").split(",");                
        var bottomCoords = parts[1].replace("bottom:", "").split(",");
        double[] top = {Double.parseDouble(topCoords[0]), Double.parseDouble(topCoords[1])};
        double[] bottom = {Double.parseDouble(bottomCoords[0]), Double.parseDouble(bottomCoords[1])};
        var angle = GeometryUtils.calAngle(top[0], top[1], bottom[0], bottom[1]);
        return angle;
    }
}
