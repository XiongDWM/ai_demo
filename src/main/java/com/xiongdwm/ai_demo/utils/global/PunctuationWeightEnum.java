package com.xiongdwm.ai_demo.utils.global;

import java.util.HashMap;
import java.util.Map;

public enum PunctuationWeightEnum {
    COMMAS("，,",0.5d),
    FULL_POINTS("。！？.!?",2.0d),
    SEMICOLON("；;",0.8d),
    COLON("：:",1.0d);

    private final String symbols;
    private final double value;
    PunctuationWeightEnum(String symbols, double value) {
        this.symbols=symbols;
        this.value=value;
    }

    public String getSymbols() {
        return symbols;
    }

    public double getValue() {
        return value;
    }

    public static Map<Character,Double> getSymbolToValue(){
        Map<Character,Double>map=new HashMap<>();
        for(PunctuationWeightEnum e:PunctuationWeightEnum.values()){
            var symbols=e.getSymbols();
            var value=e.getValue();
            for(int i=0;i<symbols.length();i++){
                var symbol=symbols.charAt(i);
                map.put(symbol,value);
            }
        }
        return map;
    }
    public static Map<Integer,Double>getSymbolToValueAsKeyInInteger(){
        Map<Integer,Double>map=new HashMap<>();
        for(PunctuationWeightEnum e:PunctuationWeightEnum.values()){
            var symbols=e.getSymbols();
            var value=e.getValue();
            for(int i=0;i<symbols.length();i++){
                char symbol=symbols.charAt(i);
                map.put(((int) symbol),value);
            }
        }
        return map;
    }
}
