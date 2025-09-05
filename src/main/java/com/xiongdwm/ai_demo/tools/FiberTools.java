package com.xiongdwm.ai_demo.tools;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.xiongdwm.ai_demo.utils.http.HttpClientManager;
import com.xiongdwm.ai_demo.utils.http.ServerChosen;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class FiberTools {
    // @Autowired
    // private CableService fiberService;

    private final WebClient webClient = WebClient.create("http://192.168.0.77:18081");
    @Autowired
    private HttpClientManager httpClientManager;

    // @Tool(name = "findCableByFromStationName", description = "根据起始站名称查询光缆信息")
    // public List<Fiber> findCableByFromStationName(
    // @ToolParam(required = true, description = "起始站名称") String fromStationName) {
    // System.out.println("findCableByFromStationName: " + fromStationName);
    // List<Fiber> fibers = fiberService.getCableByFromStationName(fromStationName);
    // if (fibers.isEmpty())
    // return Collections.emptyList();
    // System.out.println(fibers.size());
    // return fibers;
    // }

    @Tool(name = "currentDate", description = "用于获取当前日期")
    public String getCurrentTime() {
        System.out.println("获取日期");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    
    @Tool(name = "findCableIdByName", description = "根据光缆名称查询光缆ID（编号/唯一标识符），如问题包含‘id是多少’‘编号是多少’等，返回-1表示查询失败。问题示例：‘光缆金牛局~110kV洞子口站48芯普通光缆01的id是多少？’")
    public String findCableIdByName(@ToolParam(required = true, description = "光缆名称") String fiberName) {
        System.out.println("findCableIdByName: " + fiberName);
        String response = httpClientManager.post(ServerChosen.FIBER_SERVER_DEV_1, "/mcp/fiber/getIdByName")
                .param(Map.of("name", fiberName))
                .retrieve();
        System.out.println("接口返回：" + response);
        return response;
    }

    @Tool(name = "locateGlitch", description = "根据光缆，站点名称以及故障距离，获取故障发生gps位置,问题示例：金牛局~110kV洞子口站48芯普通光缆距离金牛局1500米处的故障位置定位在哪")
    public String locateGlitch(
            @ToolParam(required = true, description = "光缆名称") String fiberName,
            @ToolParam(required = true, description = "站点名称") String stationName,
            @ToolParam(required = true, description = "故障距离（单位：米）") double distance) {
        System.out.println("locateGlitch: " + fiberName + ", " + stationName + ", " + distance);
        return "光缆" + fiberName + "距离高新局端" + distance + "米处故障的准确gps为：104.062728，30.63568596296296"
                + "且根据推断，故障类型可能是纤芯劣化";
    }

    // @Tool(name = "findCableByToStationName", description = "根据终止站名称查询光纤信息")
    // public List<Fiber> findCableByToStationName(
    // @ToolParam(required = true, description = "终止站名称") String toStationName) {
    // return fiberService.getCableByToStationName(toStationName);
    // }

    // @Tool(name = "findCableByStation", description = "查询以目标站点为起终点的所有光缆信息")
    // public List<Fiber> findCableByStation(@ToolParam(required = true, description
    // = "站点名称") String stationName) {
    // System.out.println("findCableByStation: " + stationName);
    // return fiberService.getCableByStation(stationName);
    // }

    // @Tool(name = "findStationByName", description = "根据站点名称查询站点详细信息")
    // public RoutePoint findStationByName(@ToolParam(required = true, description =
    // "站点名称") String stationName) {
    // return routePointService.getRoutePointByName(stationName);
    // }

    @Tool(name = "findPath", description = "查询站点之间的通路,输出结果为多条路径")
    public String findPath(
            @ToolParam(required = true, description = "起始站名称") String fromStationName,
            @ToolParam(required = true, description = "终止站名称") String toStationName,
            @ToolParam(required = false, description = "单条路径最大跳数") int maxHops,
            @ToolParam(required = false, description = "路径的条数") int count,
            @ToolParam(required = false, description = "最大接入距离（单位：米）") double maxDistance) {
        System.out.println("findpath");

        var weight = maxHops == 0 ? 10.0d : maxHops * 2.0d;
        System.out.println(weight);
        try {
            SearchRouteParam searchParam = new SearchRouteParam(fromStationName, toStationName, weight, count,
                    maxDistance);
            return Mono.fromCallable(() -> webClient.post()
                    .uri("/rel/searchRouteByStationName")
                    .bodyValue(searchParam)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .map(list -> {
                        if (list == null || list.isEmpty())
                            return "无可用路径";
                        StringBuilder sb = new StringBuilder();
                        int i = 1;
                        for (Map<String, Object> path : list) {
                            sb.append("路径").append(i++).append(": ")
                                    .append(path.get("routes")).append("; 接入距离: ")
                                    .append(path.get("buildDistance")).append("米\n");
                        }
                        return sb.toString();
                    }).block(Duration.ofSeconds(30))).subscribeOn(Schedulers.boundedElastic())
                    .block();
        } catch (Exception e) {
            return "工具调用异常：" + e.getMessage();
        }
    }

    // @Tool(name = "findCableBetweenStationUndirection", description =
    // "查询两个站点之间的光缆信息（无向）")
    // public List<Fiber> findCableBetweenStationUndirection(
    // @ToolParam(required = true, description = "站点1名称") String sn1,
    // @ToolParam(required = true, description = "站点2名称") String sn2) {
    // return fiberService.getCableBetweenStationUndirection(sn1, sn2);
    // }

    // @Tool(name = "findCableBetweenStationIndirection", description =
    // "查询两个站点之间的光缆信息（有向）")
    // public List<Fiber> findCableBetweenStationIndirection(
    // @ToolParam(required = true, description = "起始站名称") String fromStationName,
    // @ToolParam(required = true, description = "终止站名称") String toStationName) {
    // return fiberService.getCableBetweenStationIndirection(fromStationName,
    // toStationName);
    // }

    // @Tool(name = "findCableByDateRangeClassifyByType", description =
    // "根据日期范围查询光缆信息并按类型分类，用于分析数据之前查询")
    // public Map<String, Long> findCableByDateRangeClassifyByType(
    // @ToolParam(required = true, description = "开始日期，格式：yyyy-MM-dd") String
    // startDate,
    // @ToolParam(required = true, description = "结束日期，格式：yyyy-MM-dd") String
    // endDate) {
    // return fiberService.getCableByDateRangeClassifyByType(startDate, endDate);
    // }

}