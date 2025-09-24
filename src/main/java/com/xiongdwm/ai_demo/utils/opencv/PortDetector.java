package com.xiongdwm.ai_demo.utils.opencv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;

import com.xiongdwm.ai_demo.utils.opencv.exception.OpencvDetectException;

public class PortDetector {

    static class Port {
        int x, y;
        int width, height;
        int portNumber, row;
        
        public Port(int x, int y) {
            this.x = x;
            this.y = y;
            this.row = 1; 
        }
        
        public Port(int x, int y, int portNumber) {
            this.x = x;
            this.y = y;
            this.portNumber = portNumber;
            this.row = 1;
        }
        
        public Port(int x, int y, int portNumber, int row) {
            this.x = x;
            this.y = y;
            this.portNumber = portNumber;
            this.row = row;
        }
        
        public Port(int x, int y, int width, int height, int portNumber, int row) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.portNumber = portNumber;
            this.row = row;
        }
        
        @Override
        public String toString() {
            return "Port{no=" + portNumber + ", row=" + row + ", x=" + x + ", y=" + y + "}";
        }

        // Getters and setters
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public int getPortNumber() { return portNumber; }
        public void setPortNumber(int portNumber) { this.portNumber = portNumber; }
        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
    }

    public static void main(String[] args) {
        String imagePath = "src/main/resources/portexample.jpg";
        int portsCountInLine = 12;
        
        List<Port> redPorts = detectRedPorts(imagePath, portsCountInLine, 0, 0, 0, 0);
        System.out.println("Red ports detected: " + redPorts.size());
        
        List<Integer> detected = redPorts.stream()
            .map(port -> port.portNumber)
            .sorted()
            .collect(Collectors.toList());
            
        System.out.println("Detected ports: " + detected);
        
    }

    public static List<Port> detectRedPorts(String imagePath, int portsCountInLine, double x1, double y1, double x2, double y2) {
        return detectRedPortsWithRowCount(imagePath, portsCountInLine, 0, x1, y1, x2, y2);
    }

    public static List<Port> detectRedPorts(String imagePath, int portsCountInLine, int lineCount, double x1, double y1, double x2, double y2) {
        return detectRedPortsWithRowCount(imagePath, portsCountInLine, lineCount, x1, y1, x2, y2);
    }

    private static List<Port> detectRedPortsWithRowCount(String imagePath, int portsCountInLine, int lineCount, double x1, double y1, double x2, double y2) {
        Mat src = opencv_imgcodecs.imread(imagePath);
        if (src.empty()) {
            System.out.println("Unable to read image: " + imagePath);
            return new ArrayList<>();
        }

        // ROI processing
        Mat workingImage;
        final int roiOffsetX;
        final int roiOffsetY;
        
        if (isValidROI(x1, y1, x2, y2)) {
            int roiX = (int) Math.max(0, Math.min(x1, x2));
            int roiY = (int) Math.max(0, Math.min(y1, y2));
            int roiWidth = (int) Math.abs(x2 - x1);
            int roiHeight = (int) Math.abs(y2 - y1);
            
            roiX = Math.min(roiX, src.cols() - 1);
            roiY = Math.min(roiY, src.rows() - 1);
            roiWidth = Math.min(roiWidth, src.cols() - roiX);
            roiHeight = Math.min(roiHeight, src.rows() - roiY);
            
            if (roiWidth > 0 && roiHeight > 0) {
                Rect roiRect = new Rect(roiX, roiY, roiWidth, roiHeight);
                workingImage = new Mat(src, roiRect);
                roiOffsetX = roiX;
                roiOffsetY = roiY;
            } else {
                workingImage = src.clone();
                roiOffsetX = 0;
                roiOffsetY = 0;
            }
        } else {
            workingImage = src.clone();
            roiOffsetX = 0;
            roiOffsetY = 0;
        }
        
        Mat hsv = new Mat();
        int imageWidth = workingImage.cols();
        int imageHeight = workingImage.rows();
        
        if (lineCount <= 0) lineCount = 1; 
        
        opencv_imgproc.cvtColor(workingImage, hsv, opencv_imgproc.COLOR_BGR2HSV);
        
        System.out.println("Processing area size: " + imageWidth + "x" + imageHeight + "px");

        // Red color detection
        Scalar lowerRed1Scalar = new Scalar(0, 100, 100, 0); 
        Scalar upperRed1Scalar = new Scalar(10, 255, 255, 0);
        Scalar lowerRed2Scalar = new Scalar(170, 100, 100, 0);
        Scalar upperRed2Scalar = new Scalar(180, 255, 255, 0);
        
        Mat lowerRed1 = new Mat(hsv.size(), hsv.type(), lowerRed1Scalar);
        Mat upperRed1 = new Mat(hsv.size(), hsv.type(), upperRed1Scalar);
        Mat lowerRed2 = new Mat(hsv.size(), hsv.type(), lowerRed2Scalar);
        Mat upperRed2 = new Mat(hsv.size(), hsv.type(), upperRed2Scalar);

        Mat mask1 = new Mat();
        Mat mask2 = new Mat();
        opencv_core.inRange(hsv, lowerRed1, upperRed1, mask1);
        opencv_core.inRange(hsv, lowerRed2, upperRed2, mask2);
        Mat mask = new Mat();
        opencv_core.bitwise_or(mask1, mask2, mask);
        
        opencv_imgcodecs.imwrite("debug_final_mask.jpg", mask);

        // Find contours
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        opencv_imgproc.findContours(mask, contours, hierarchy, opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);
        
        System.out.println("Found " + contours.size() + " contours in total");
        
        List<Port> redPorts = new ArrayList<>();
        
        IntStream.range(0, (int) contours.size())
            .forEach(i -> {
                Mat contour = contours.get(i);
                Rect boundingRect = opencv_imgproc.boundingRect(contour);
                
                int width = boundingRect.width();
                int height = boundingRect.height();
                int area = width * height;
                double aspectRatio = (double) width / height;
                
                // Basic filtering for red ports
                // Typical size: width 20-300px, height 20-200px, area 400-50000px, aspect ratio 0.3-3.0
                if (width > 20 && height > 20 && width < 300 && height < 200 && 
                    area > 400 && area < 50000 && aspectRatio > 0.3 && aspectRatio < 3.0) {
                    
                    int centerX = boundingRect.x() + boundingRect.width() / 2;
                    int centerY = boundingRect.y() + boundingRect.height() / 2;
                    
                    int originalCenterX = centerX + roiOffsetX;
                    int originalCenterY = centerY + roiOffsetY;
                    
                    redPorts.add(new Port(originalCenterX, originalCenterY, width, height, 0, 1));
                    System.out.println("Accepted contour: size=" + width + "x" + height + ", area=" + area);
                } 
            });
        
        if (redPorts.isEmpty()) {
            throw new OpencvDetectException("No ports detected!");
        }

        // Calculate actual row count
        int actualRowCount = calculateActualRowCount(redPorts, imageHeight);
        if (actualRowCount != lineCount) {
            System.out.println("Detected row count: " + actualRowCount);
            lineCount = actualRowCount;
        }

        // Assign row numbers and port numbers
        assignRowNumbersAndPortNumbers(redPorts, actualRowCount, portsCountInLine, imageWidth, roiOffsetX);
        
        // Sort by row and port number
        redPorts.sort((p1, p2) -> {
            int rowCompare = Integer.compare(p1.getRow(), p2.getRow());
            if (rowCompare != 0) return rowCompare;
            return Integer.compare(p1.getPortNumber(), p2.getPortNumber());
        });
        
        return redPorts;
    }
    

    private static boolean isValidROI(double x1, double y1, double x2, double y2) {
        if (x1 == 0 && y1 == 0 && x2 == 0 && y2 == 0) return false;
        if (Math.abs(x2 - x1) <= 0 || Math.abs(y2 - y1) <= 0) return false;
        if (x1 < 0 || y1 < 0 || x2 < 0 || y2 < 0) return false;
        return true;
    }


    private static int calculateActualRowCount(List<Port> ports, int imageHeight) {
        if (ports.isEmpty()) return 1;
        
        List<Port> sortedPorts = new ArrayList<>(ports);
        sortedPorts.sort((p1, p2) -> Integer.compare(p1.y, p2.y));
        
        int totalHeight = ports.stream()
            .mapToInt(port -> port.getHeight() > 0 ? port.getHeight() : 20)
            .sum();
        double avgPortHeight = (double) totalHeight / ports.size();
        double clusterThreshold = avgPortHeight * 0.8; 
        
        List<Integer> rowYPositions = new ArrayList<>();
        rowYPositions.add(sortedPorts.get(0).y);
        
        for (int i = 1; i < sortedPorts.size(); i++) {
            Port currentPort = sortedPorts.get(i);
            boolean foundRow = false;
            
            for (int rowY : rowYPositions) {
                if (Math.abs(currentPort.y - rowY) <= clusterThreshold) {
                    foundRow = true;
                    break;
                }
            }
            
            if (!foundRow) {
                rowYPositions.add(currentPort.y);
            }
        }
        
        return Math.max(1, rowYPositions.size());
    }
    
    /**
     * Assign row numbers and port numbers using improved mixed algorithm
     */
    private static void assignRowNumbersAndPortNumbers(List<Port> ports, int rowCount, int portsCountInLine, int imageWidth, int roiOffsetX) {
        // Step 1: Assign row numbers
        if (rowCount <= 1) {
            ports.forEach(port -> port.setRow(1));
        } else {
            // Cluster by Y coordinate for multiple rows
            List<Port> sortedPorts = new ArrayList<>(ports);
            sortedPorts.sort((p1, p2) -> Integer.compare(p1.y, p2.y));
            
            double avgPortHeight = ports.stream().mapToInt(Port::getHeight).filter(h -> h > 0).average().orElse(20.0);
            double clusterThreshold = avgPortHeight * 0.8;
            
            List<List<Port>> rowClusters = new ArrayList<>();
            for (Port port : sortedPorts) {
                boolean assigned = false;
                
                for (List<Port> cluster : rowClusters) {
                    if (!cluster.isEmpty()) {
                        double avgY = cluster.stream().mapToInt(p -> p.y).average().orElse(0);
                        if (Math.abs(port.y - avgY) <= clusterThreshold) {
                            cluster.add(port);
                            assigned = true;
                            break;
                        }
                    }
                }
                
                if (!assigned) {
                    List<Port> newCluster = new ArrayList<>();
                    newCluster.add(port);
                    rowClusters.add(newCluster);
                }
            }
            
            IntStream.range(0, rowClusters.size())
                .forEach(i -> {
                    List<Port> cluster = rowClusters.get(i);
                    int rowNumber = i + 1;
                    cluster.forEach(port -> port.setRow(rowNumber));
                });
        }
        
        // Step 2: Assign port numbers using mixed algorithm
        Map<Integer, List<Port>> portsByRow = ports.stream()
            .collect(Collectors.groupingBy(Port::getRow));
        
        portsByRow.entrySet().forEach(entry -> {
            int rowNumber = entry.getKey();
            List<Port> rowPorts = entry.getValue();
            
            rowPorts.sort((p1, p2) -> Integer.compare(p1.x, p2.x));
            
            System.out.println("Row " + rowNumber + ": processing " + rowPorts.size() + " ports");
            
            Set<Integer> usedPortNumbers = new HashSet<>();
            List<Integer> assignedNumbers = new ArrayList<>();
            
            rowPorts.forEach(port -> {
                double portX = port.x - roiOffsetX;
                
                // Mixed algorithm: percentage + precise position matching
                double relativeX = portX / imageWidth;
                int percentagePortNumber = (int) Math.round(relativeX * portsCountInLine) + 1;
                percentagePortNumber = Math.max(1, Math.min(percentagePortNumber, portsCountInLine));
                
                double slotWidth = (double) imageWidth / portsCountInLine;
                int nearestSlot = -1;
                double minDistance = Double.MAX_VALUE;
                
                for (int i = 0; i < portsCountInLine; i++) {
                    double slotCenter = (i + 0.5) * slotWidth;
                    double distance = Math.abs(portX - slotCenter);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestSlot = i;
                    }
                }
                int precisePortNumber = nearestSlot + 1;
                
                // Choose strategy based on difference
                int difference = Math.abs(percentagePortNumber - precisePortNumber);
                int preferredPortNumber;
                String method;
                
                if (difference <= 1) {
                    preferredPortNumber = precisePortNumber;
                    method = "precise";
                } else {
                    preferredPortNumber = percentagePortNumber;
                    method = "percentage";
                }
                
                // Handle conflicts
                int finalPortNumber = preferredPortNumber;
                if (usedPortNumbers.contains(preferredPortNumber)) {
                    finalPortNumber = preferredPortNumber;
                    while (usedPortNumbers.contains(finalPortNumber) && finalPortNumber <= portsCountInLine) {
                        finalPortNumber++;
                    }
                    if (finalPortNumber > portsCountInLine) {
                        finalPortNumber = preferredPortNumber - 1;
                        while (finalPortNumber > 0 && usedPortNumbers.contains(finalPortNumber)) {
                            finalPortNumber--;
                        }
                        if (finalPortNumber < 1) finalPortNumber = preferredPortNumber;
                    }
                    method += " (adjusted)";
                }
                
                port.setPortNumber(finalPortNumber);
                usedPortNumbers.add(finalPortNumber);
                assignedNumbers.add(finalPortNumber);
                
                System.out.println("  Port at X=" + port.x + " (portX=" + String.format("%.1f", portX) + 
                                 ", percentage=" + percentagePortNumber + ", precise=" + precisePortNumber + 
                                 ", method=" + method + ", diff=" + difference + 
                                 ") -> Port " + finalPortNumber);
            });
            
            assignedNumbers.sort(Integer::compareTo);
            System.out.println("  Row " + rowNumber + " assigned numbers: " + assignedNumbers);
            
            int maxNumber = assignedNumbers.stream().mapToInt(Integer::intValue).max().orElse(0);
            int diff = maxNumber - portsCountInLine;
            System.out.println("  Row " + rowNumber + " max port number: " + maxNumber + 
                             ", portsCountInLine: " + portsCountInLine + 
                             ", difference: " + diff);
            
            if (diff > 0) {
                rowPorts.stream()
                    .peek(port -> port.setPortNumber(port.getPortNumber() - diff))
                    .forEach(port -> System.out.println("  Port at X=" + port.x + " adjusted to Port " + port.getPortNumber()));
            }
        });
    }
}