package com.xiongdwm.ai_demo.embedding.utils;

import org.springframework.ai.document.Document;

import java.util.List;

public class KnowledgeCompressing {

    //PQ compute codebook for labels
    // strategy for codebook computing in periodic update, e.g., every 30 percent new data, recompute codebook

    public void computeCodebook(){

    }

    public List<List<Double>> subspaces(int M,List<Double>origin){
        var subspaceSize = origin.size()/M;
        var subspaces = List.<List<Double>>of();



        return List.of(List.of(0.0d));
    }
}
