package com.biface.myapplication;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Classse converte palavras de grandezas nas casas decimais correspondente.
 *  Ex:
 *input:  700 sextilhões 999 quintilhões 15 quadrilhões 12 trilhões 234 bilhões e 4 milhão e 5 mil
 *output: 700.999.015.012.234.004.000.000
 *
 *inputs: 15 quadrilhões 12 trilhões mil 425
 *        15 quadrilhões e 142 bilhoes 1425
 *        15quadrilhões 142bilhoes 25 mil e 425 *
 *output: 15.012.000.000.001.425
 *
 * @author Caio
 */
public class ExtensoParaNumero {

    //Compile this with shift + f6 , debug CTRL+SHIFT+F5
    public static void main(String[] args){
        String[] tests = {
                "um trilhao e dois bilhao e nove milhoes oito mil e 37",

                "98.7 bilhoes e 569.256,955",
                "2.5 quintilhao e 25.3 trilhoes e 98 bilhoes e 569.256,955",
                "15,5% de 2.5 quintilhao e 32 quadrilhao e 25 trilhoes e 98 bilhoes e 569.256,955",
                "1,5 milhoes",
                "1 decilhao e 32 septilhoes e 25 trilhoes e 98 bilhoes e 569 mil",
                "999 decilhoes e 850 nonilhoes e 25 trilhoes e 98 bilhoes e 569 mil e 256?",
                "15,5% de 2 quintilhao e 32 quadrilhao e 25 trilhoes e 98 bilhoes e 569.256,955",
                "15 quadrilhões e 142 bilhoes 1425",
                "15 quadrilhões 142 bilhoes e mil e 425,1232235",
                "15 quadrilhões 142 bilhoes mil 425",
                "quanto é 700 sextilhões 999 quintilhões 15 quadrilhões 12 trilhões e 4 milhão",
        };
        ExtensoParaNumero x = new ExtensoParaNumero();
        for (String test : tests) {
            System.out.println(x.Converter(test));
        }

    }
    public ExtensoParaNumero(){

    }
    public void test(){
        String q = "3.000.000.000.000.000";
        String t = "1.000.000.000.000";
        String m = "25.000.000";

        int beginIndex = q.length() - m.length();
        String original = q.substring(0, beginIndex) + m;
        System.out.println(original);
    }

    private String sumStringNumbers(ArrayList<String> listNumbers){
        //opcional, caso for "1 milhao e 2 bilhoes" arrumara a ordem.
        Collections.sort(listNumbers, (a, b)->Integer.compare(b.length(), a.length()));
        String result = listNumbers.get(0);
        for (int i = 1; i < listNumbers.size(); i++) {
            int beginIndex = result.length() - listNumbers.get(i).length();
            result = result.substring(0, beginIndex) + listNumbers.get(i);
        }
        return result;
    }

    public String Converter(String pergunta){
        pergunta = ConverterExtenso0a10(pergunta);
        pergunta = ConverterGrandezas(pergunta);
        return pergunta;
    }

    private String ConverterExtenso0a10(String pergunta){
        String[] numeros = new String[] {
                "um", "1", //evitar confucao "tenho um valor..."
                "dois", "2",
                "tres", "3",
                "quatro", "4",
                "cinco", "5",
                "seis", "6",
                "sete", "7",
                "oito", "8",
                "nove", "9",
                "dez", "10",
        };
        for (int i = 0; i < numeros.length; i+=2) {
            pergunta = pergunta.replaceAll(numeros[i]+"(?=\\s\\w+il)", numeros[i+1]);
        }
        return pergunta;
    }

    private String ConverterGrandezas(String pergunta){
        String[] GRANDEZASeGRUPOZEROS = new String[] {
                "(?<!\\d\\s)mil\\b", "1 mil", // e mil ... -> e 1 mil...
                "(?<=\\d\\s)mil\\b", "1", // 1 mil ... -> 1.000
                "milh(ã|a)?(o|õ)e?s?", "2",
                "bilh(ã|a)?(o|õ)e?s?", "3",
                "trilh(ã|a)?(o|õ)e?s?", "4",
                "quadrilh(ã|a)?(o|õ)e?s?", "5",
                "quintilh(ã|a)?(o|õ)e?s?", "6", //6x3 = 15 zeros
                "sextilh(ã|a)?(o|õ)e?s?", "7", //7x3 = 21 zeros
                "septilh(ã|a)?(o|õ)e?s?", "8", //8x3 = 24 zeros
                "octilh(ã|a)?(o|õ)e?s?", "9", //9x3 = 27 zeros
                "nonilh(ã|a)?(o|õ)e?s?", "10", //9x3 = 30 zeros
                "\\bdecilh(ã|a)?(o|õ)e?s?", "11",
        };
        ArrayList<String> GrandezasConvertidas = new ArrayList<String>();
        String findWordsKey = "(\\d+?((.|,)\\d+)?\\s*(\\w+lh(ã|a)?(o|õ)e?s?)\\s*e?\\s*)+(\\d*\\s*mil?)?(\\s*e?\\s*\\d+)?";
        Matcher m1 = Pattern.compile(findWordsKey).matcher(pergunta);
        while(m1.find()){
            String TextOriginal = m1.group();
            String TextDecimal = TextOriginal.replaceFirst(GRANDEZASeGRUPOZEROS[0], GRANDEZASeGRUPOZEROS[1]);

            for (int i = 2; i < GRANDEZASeGRUPOZEROS.length; i+=2) {
                //subistitui o primeiro
                String zeros = repeat("000", Integer.parseInt(GRANDEZASeGRUPOZEROS[i+1]));
                TextDecimal = TextDecimal.replaceFirst("\\s*"+GRANDEZASeGRUPOZEROS[i], zeros);

            }

            Matcher m = Pattern.compile("(?<!\\d)-?\\.?\\d(.*?)(\\s*%)?(?!(\\d|\\.\\d|,\\d))").matcher(TextDecimal);
            while(m.find()){
                String parte = m.group();
                if(parte.contains(".") || parte.contains(",")){
                    //remover o ponto do grupo e remover um 0
                    parte = parte.replaceAll(",|\\.|0$", "");
                }

                GrandezasConvertidas.add(parte);
            }
            TextDecimal = sumStringNumbers(GrandezasConvertidas);
            TextDecimal = insertDots2(TextDecimal);
            pergunta = pergunta.replaceFirst(TextOriginal, TextDecimal);
        }
        return pergunta;
    }
    private String insertDots2(String values){
        BigDecimal d = new BigDecimal(values);
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(d);
    }

    private String insertDots(String values){
        int totalDots = values.length() / 3;
        for (int i = 1; i <= totalDots; i++) {
            values = new StringBuilder(values).insert(values.length()-(i*3+(i-1)), ".").toString();
        }
        return values;
    }

    public String repeat(String s, int count) {
        return count > 0 ? s + repeat(s, --count) : "";
    }

    public static String removeDiacriticalMarks(String string) {
        return Normalizer.normalize(string, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
