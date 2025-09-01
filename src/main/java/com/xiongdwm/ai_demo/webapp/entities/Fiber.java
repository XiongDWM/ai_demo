package com.xiongdwm.ai_demo.webapp.entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.xiongdwm.ai_demo.embedding.annotaion.AiVectorize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fiber")
@AiVectorize(name = "fiber", description = "光缆表，用来记录光缆信息，光缆两端连接站点", type = AiVectorize.AiVectorizeType.ENTITY)
public class Fiber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @AiVectorize(name = "id", description = "光缆主键ID,自增且不重复", type = AiVectorize.AiVectorizeType.FIELDS)
    private Long id = 0L;

    @Column
    @AiVectorize(name = "name", description = "光缆名称，唯一索引", type = AiVectorize.AiVectorizeType.FIELDS)
    private String name; // 名称

    @Column
    @AiVectorize(name = "no", description = "光缆编号，光缆的字符编号", type = AiVectorize.AiVectorizeType.FIELDS)
    private String no; // 编号

    @Column
    @AiVectorize(name = "level", description = "光缆级别, 枚举值有主网一级，主网二级，主网三级，配网光缆等级别等，以实际存储为准", type = AiVectorize.AiVectorizeType.FIELDS)
    private String level; // 级别

    @Column
    @AiVectorize(name = "net", description = "光缆拓扑类型, 枚举值有主干网，接入网，接入网分支等，以实际存储为准", type = AiVectorize.AiVectorizeType.FIELDS)
    private String net; // 拓扑类型

    @Column
    @AiVectorize(name = "format", description = "光缆规格", type = AiVectorize.AiVectorizeType.FIELDS)
    private String format; // 规格

    @Column
    @AiVectorize(name = "type", description = "光缆类型, 枚举值有GYTS、GYTA、ADSS等，以实际存储为准", type = AiVectorize.AiVectorizeType.FIELDS)
    private String type; // 类型

    @Column(name = "maintain_by")
    @AiVectorize(name = "maintain_by", description = "维护单位/生产厂家", type = AiVectorize.AiVectorizeType.FIELDS)
    private String maintainBy; // 维护单位/生产厂家

    @Column(name = "fibercd")
    @AiVectorize(name = "fibercd", description = "光缆成端，记录光缆的一端在设备上的接入有多少芯", type = AiVectorize.AiVectorizeType.FIELDS)
    private String fiberCD; // 成端

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd")
    @AiVectorize(name = "lay_at", description = "光缆铺设时间", type = AiVectorize.AiVectorizeType.FIELDS)
    private Date layAt = new Date(); // 铺设时间
    
    @Column
    @AiVectorize(name = "parent", description = "总线", type = AiVectorize.AiVectorizeType.FIELDS)
    private Long parent; // 总线

    @Column
    @AiVectorize(name = "parent_order", description = "总线点位", type = AiVectorize.AiVectorizeType.FIELDS)
    private Long parentOrder; // 总线点位

    @Column
    @AiVectorize(name = "belong", description = "光缆属主", type = AiVectorize.AiVectorizeType.FIELDS)
    private String belong = "自建自用"; // 属主

    @Column
    @AiVectorize(name = "dis", description = "光缆长度，单位米", type = AiVectorize.AiVectorizeType.FIELDS)
    private Double dis = 0.0; // 长度

    @Column(name = "area_range")
    @AiVectorize(name = "area_range", description = "光缆管辖区域", type = AiVectorize.AiVectorizeType.FIELDS)
    private String areaRange; // 管辖区域

    @Column(name = "fb_total")
    @AiVectorize(name = "fb_total", description = "光缆纤芯总数", type = AiVectorize.AiVectorizeType.FIELDS)
    private Integer fbTotal = 0; // 纤芯总数

    @Column(name= "from_station_id")
    @AiVectorize(name = "from_station_id", description = "光缆起始站点ID，对应站点表中站点的主键", type = AiVectorize.AiVectorizeType.FIELDS)
    private Long fromStationId; // 本段起始站点

    @Column(name= "to_station_id")
    @AiVectorize(name = "to_station_id", description = "光缆结束站点ID，对应站点表中站点的主键", type = AiVectorize.AiVectorizeType.FIELDS)
    private Long toStationId; // 本段结束站点

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name = "from_station_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonManagedReference
    RoutePoint fromStation; // 起始站点

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name = "to_station_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonManagedReference
    RoutePoint toStation; // 结束站点

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

    public RoutePoint getFromStation() {
        return fromStation;
    }

    public RoutePoint getToStation() {
        return toStation;
    }

    
}
