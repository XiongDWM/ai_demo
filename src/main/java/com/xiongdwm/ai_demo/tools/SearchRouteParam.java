package com.xiongdwm.ai_demo.tools;

import org.springframework.lang.Nullable;

public record SearchRouteParam(
        @Nullable String fromStation,
        @Nullable String toStation,
        double weight,
        int routeCount,
        double maxDistance
  ) {

        @Override
        public String fromStation() {
            return fromStation;
        }

        @Override
        public String toStation() {
            return toStation;
        }

        @Override
        public double weight() {
            return weight==0.0?5.0d:weight;
        }
        @Override
        public int routeCount() {
            return routeCount==0?5:routeCount;
        }

        @Override
        public double maxDistance() {
            return maxDistance==0.0?100.0d:maxDistance;
        }

}
