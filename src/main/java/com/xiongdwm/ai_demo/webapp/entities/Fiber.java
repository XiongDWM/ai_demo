package com.xiongdwm.ai_demo.webapp.entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xiongdwm.ai_demo.embedding.annotaion.AiVectorize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fiber")
@AiVectorize(name = "fiber", description = "光缆表，用来记录光缆信息，光缆两端连接站点", type = AiVectorize.AiVectorizeType.ENTITY)
public class Fiber {
    @Id
    @Column
    @AiVectorize(name = "id", description = "bigint,光缆主键ID,自增且不重复", type = AiVectorize.AiVectorizeType.FIELDS)
    private Long id = 0L;

    @Column
    @AiVectorize(name = "name", description = "varchar,光缆名称", type = AiVectorize.AiVectorizeType.FIELDS)
    private String name; // 名称

    @Column
    private String no; // 编号

    @Column
    @AiVectorize(name = "level", description = "varchar,光缆级别, 枚举值，有主网一级，主网二级，主网三级，配网光缆等级别", type = AiVectorize.AiVectorizeType.FIELDS)
    private String level; // 级别

    @Column
    private String net; // 拓扑类型

    @Column
    private String format; // 规格

    @Column
    private String type; // 类型

    @Column(name = "maintain_by")
    private String maintainBy; // 维护单位/生产厂家

    @Column(name = "fibercd")
    private String fiberCD; // 成端

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date layAt = new Date(); // 铺设时间
    
    @Column
    private Long parent; // 总线

    @Column
    private Long parentOrder; // 总线点位

    @Column
    private String belong = "自建自用"; // 属主

    @Column
    @AiVectorize(name = "dis", description = "double,光缆长度", type = AiVectorize.AiVectorizeType.FIELDS)
    private Double dis = 0.0; // 长度

    @Column(name = "area_range")
    @AiVectorize(name = "area_range", description = "varchar,光缆管辖区域", type = AiVectorize.AiVectorizeType.FIELDS)
    private String areaRange; // 管辖区域

    @Column(name = "fb_total")
    private Integer fbTotal = 0; // 纤芯总数

    @Column
    @AiVectorize(name = "from_station_id", description = "bigint,光缆起始站点ID，对应站点表中站点的主键", type = AiVectorize.AiVectorizeType.FIELDS)
    private Long fromStationId; // 本段起始站点

    @Column
    @AiVectorize(name = "to_station_id", description = "bigint,光缆结束站点ID，对应站点表中站点的主键", type = AiVectorize.AiVectorizeType.FIELDS)
    private Long toStationId; // 本段结束站点

    @Column(name = "check")
    private String check; // 审核

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getNet() {
        return net;
    }

    public void setNet(String net) {
        this.net = net;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMaintainBy() {
        return maintainBy;
    }

    public void setMaintainBy(String maintainBy) {
        this.maintainBy = maintainBy;
    }

    public String getFiberCD() {
        return fiberCD;
    }

    public void setFiberCD(String fiberCD) {
        this.fiberCD = fiberCD;
    }

    public Date getLayAt() {
        return layAt;
    }

    public void setLayAt(Date layAt) {
        this.layAt = layAt;
    }

    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    public Long getParentOrder() {
        return parentOrder;
    }

    public void setParentOrder(Long parentOrder) {
        this.parentOrder = parentOrder;
    }

    public String getBelong() {
        return belong;
    }

    public void setBelong(String belong) {
        this.belong = belong;
    }

    public Double getDis() {
        return dis;
    }

    public void setDis(Double dis) {
        this.dis = dis;
    }

    public String getAreaRange() {
        return areaRange;
    }

    public void setAreaRange(String areaRange) {
        this.areaRange = areaRange;
    }

    public Integer getFbTotal() {
        return fbTotal;
    }

    public void setFbTotal(Integer fbTotal) {
        this.fbTotal = fbTotal;
    }

    public Long getFromStationId() {
        return fromStationId;
    }

    public void setFromStationId(Long fromStationId) {
        this.fromStationId = fromStationId;
    }

    public Long getToStationId() {
        return toStationId;
    }

    public void setToStationId(Long toStationId) {
        this.toStationId = toStationId;
    }

    public String getCheck() {
        return check;
    }

    public void setCheck(String check) {
        this.check = check;
    }

    
}
