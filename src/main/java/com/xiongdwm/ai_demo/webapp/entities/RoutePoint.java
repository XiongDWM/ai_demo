package com.xiongdwm.ai_demo.webapp.entities;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xiongdwm.ai_demo.embedding.annotaion.AiVectorize;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.Date;

@Entity
@Table(name = "route_point")
@AiVectorize(name = "route_point", description = "机房站点表，用来记录站点信息", type = AiVectorize.AiVectorizeType.ENTITY)
public class RoutePoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @AiVectorize(name = "id", description = "bigint,机房主键ID,自增且不重复", type = AiVectorize.AiVectorizeType.FIELDS)
    private Long id;

    @Column
    @AiVectorize(name = "name", description = "varchar,站点名称", type = AiVectorize.AiVectorizeType.FIELDS)
    private String name;

    @Column
    @AiVectorize(name = "lat", description = "double,站点纬度", type = AiVectorize.AiVectorizeType.FIELDS)
    private Double lat;

    @Column
    @AiVectorize(name = "lng", description = "double,站点经度", type = AiVectorize.AiVectorizeType.FIELDS)
    private Double lng;

    @Column
    @AiVectorize(name = "address", description = "varchar,站点地址", type = AiVectorize.AiVectorizeType.FIELDS)
    private String address;

    @Column
    @Enumerated(EnumType.STRING)
    @AiVectorize(name = "type", description = "varchar,站点类型, 枚举值，有类型机房，环网柜，开关柜", type = AiVectorize.AiVectorizeType.FIELDS)
    private RoutePointType type;

    @Transient
    private String typeName;

    @AiVectorize(name = "gps84", description = "varchar,GPS坐标WGS84", type = AiVectorize.AiVectorizeType.FIELDS)
    @Column
    private String gps84;

    @Column
    private String icon;

    @Column
    private String remarks;//备注

    @Column
    private String bcrl;//标称容量

    @Column
    private String pointId;//ID

    @Column
    private int rowSize;//行数

    @Column
    private int colSize;//列数

    @Column
    private int sideSize;//面数

    @Column
    private String street;

    @Column
    @AiVectorize(name = "area", description = "区县，行政区，用来记录站点所属的区县，比如青羊区、高新区", type = AiVectorize.AiVectorizeType.FIELDS)
    private String area;

    @Column
    private String pics;

    @Column
    private String userName; 

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column
    private Date time;//操作时间

    @Column
    private String files;//变电站设计图 pdf文件


    
    public enum RoutePointType {
        STATION("机房"),
        ODN("光交箱"),
        BOX("分线盒"),
        TERMINAL("终端盒"),
        SWITCH_BOX("开关柜"),
        Ring_Main_Unit("环网柜"),
        TYPE_T("T节点"),
        TRANSFORMER_STATION("变电站"),
        TERMINAL_POLE("终端杆"),
        PYLON("铁塔"),
        Pi_NODE("Π节点"),
        TYPE_UNDEFINED("未知节点类型");


        RoutePointType(String name) {
            this.name = name;
        }

        private String name;

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public void setTypeName() {
        this.typeName = this.type.getName();
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

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

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public RoutePointType getType() {
        return type;
    }

    public void setType(RoutePointType type) {
        this.type = type;
    }

    public String getGps84() {
        return gps84;
    }

    public void setGps84(String gps84) {
        this.gps84 = gps84;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

//    @JsonIgnore()
    public String getTypeName() {
        return this.type.getName();
    }
    @JsonIgnore
    public static RoutePointType typeByName(String name) {
        for (RoutePointType type : RoutePointType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    public String getRemarks() {
        return this.remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getBcrl() {
        return this.bcrl;
    }

    public void setBcrl(String bcrl) {
        this.bcrl = bcrl;
    }

    public String getPointId() {
        return this.pointId;
    }

    public void setPointId(String pointId) {
        this.pointId = pointId;
    }

    public String getPics() {
        return this.pics;
    }

    public void setPics(String pics) {
        this.pics = pics;
    }

    public int getRowSize() {
        return rowSize;
    }

    public void setRowSize(int rowSize) {
        this.rowSize = rowSize;
    }

    public int getColSize() {
        return colSize;
    }

    public void setColSize(int colSize) {
        this.colSize = colSize;
    }

    public int getSideSize() {
        return sideSize;
    }

    public void setSideSize(int sideSize) {
        this.sideSize = sideSize;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getFiles() {
        return files;
    }

    public void setFiles(String files) {
        this.files = files;
    }

    @Override
    public String toString() {
        return "RoutePoint{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", address='" + address + '\'' +
                ", type=" + type +
                ", gps84='" + gps84 + '\'' +
                ", icon='" + icon + '\'' +
                ", remarks='" + remarks + '\'' +
                ", bcrl='" + bcrl + '\'' +
                ", pointId='" + pointId + '\'' +
                ", rowSize=" + rowSize +
                ", colSize=" + colSize +
                ", sideSize=" + sideSize +
                ", street='" + street + '\'' +
                ", area='" + area + '\'' +
                ", pics='" + pics + '\'' +
                ", userName='" + userName + '\'' +
                ", time=" + time +
                ", files='" + files + '\'' +
                ", typeName='" + typeName + '\'' +
                '}';
    }
}

