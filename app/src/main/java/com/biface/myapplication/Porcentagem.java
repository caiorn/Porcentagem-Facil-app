package com.biface.myapplication;
import android.text.TextUtils;


import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * V. 1.1
 * [x]correção regex de medidas
 * [x] novos atalhos
 *      +|-10% = 20  retornara qual é o valor inicial
 *      10 para 20   retorna a variacao
 * [x] reconhecer  mesmas medidas (kg e g  ou ml e Litros são msm medidas)
 * @author Caio Souza Santos 2021
 */
public class Porcentagem {


    boolean PERGUNTA_FALADA = true; //false = digitada
    public final int SIMPLE = 0; //quanto e 10% de 10
    public final int SIMPLE2 = 10; //15 e qual porcentagem de 10
    //ADICAO E REDUCAO PERCENTUAL
    public final int ADD = 1; //aumento percentual
    public final int SUB = 2; //reducao percentual
    //VARIACAO PERCENTUAL
    public final int VARIACAO = 4;

    public final int REGRA3 = 7; //quanto % um valor diminiu para outro
    //DESCOBRIR VALOR INICIAL E FINAL regra3
    public final int VL_INICIAL_AOAUMENTAR = 8; //valor que apos reduzio 12% foi para 10 qual o valor inicial
    public final int VL_INICIAL_AODIMINUIR = 9;

    public final int COMPARATIVO_PRECO = 11;

    //REGEXS find
    String regex_floatNumbers = "-?\\.?\\d(.*?)(?!(\\d|\\.\\d|,\\d))";
    String regex_negativeNumbersPercent = "-\\s*\\d+((,|.)\\d+)?\\s*%";
    String regex_sinonimosIgualdade = "(est(á|a) para|(que\\s*)?(é?|for?)\\s*(=|igual|(equivale|corresponde|representa)\\w*)?\\s*(é?a?o?|(ao\\s*(valor|numero)?";
    String regex_estrairValores = "(?<!\\d)((R|r)?(\\$\\s*)?)?"+regex_floatNumbers+"(\\s*((%)|("+getRegexMedidas(true)[0]+")\\b))?";
    //String regex_valor_atribuido_old ="-?\\d+((,|\\.)\\d+)?%?\\s+(que\\s*)?(é?|for?)\\s*(=|igual|(equivale|corresponde|representa)\\w*)?\\s*(é?a?o?|(ao\\\\s*(valor|numero)?))"+regex_estrairValores;
    String regex_valor_atribuido = "-?\\d+((,|\\.)\\d+)?%?\\s+"+regex_sinonimosIgualdade+")))"+regex_estrairValores;
    String regex_sinonimoEquivalencia ="\\s*"+regex_sinonimosIgualdade+")))\\s*";
    String regex_temValorInicial = "inici\\w+\\s*?(=?)(de\\s*numero\\s*)?\\s*"+regex_sinonimosIgualdade+")))\\s*?(-\\s*)?\\d+";
    String regex_valorAtribuidoParaVariacao = "tenho\\s*\\d+|\\d+\\s+é?\\s*(equivale|igual|ser|corresponde)?\\w*(?!(.*?)\\d+)";
    String regex2_valorAtribuidoParaVariacao2 = "de\\s*(?<!\\d)-?\\.?\\d(.*?)(\\s*%)?(?!(\\d|\\.\\d|,\\d))";

    //REGEX Complex for replace all
    String regex_porcento = "(\\s*((po)\\S*(to+s?)\\b|\\bpor cento\\b|\\b(per|por)centual\\b))+|(\\s+%+)+";
    String regex_quanto = "\\b(qu|cu|co)\\S+((to|tu)+s?)\\b";
    String regex_treeDigitsIsNotDecimal = "(?<=\\d\\d|[1-9])\\.(?!(\\d{1,2}|\\d{4,})\\b)";
    String regex_sinonimo_100per = "montante|total|final|fim|original|(de qual (n(ú|u)mero|valor)\\s*!?\\??\\.?$)";
    String regex_trocarEporPonto = "(?<=\\d)(\\s*e\\s*)(?=\\d+)";
    String regex_trocarDoisPontoPorPonto = "(?<=\\d)(:)(?=\\d+)";
    String allExceptLastDot = "(\\.|,)(?=.*(,|\\.))";


    public Porcentagem(){

    }

    public final Double[] VAR_TEST_CONTINUE = {-12.33d};

    //receber a perguta
    public String perguntar(String perguntaClient, boolean perguntaFalada){
        try {
            perguntaClient = tratarPerguntas(perguntaClient, perguntaFalada);
            String[] valores = extrairValores(perguntaClient);
            int tipoCalc = classificarTipoPergunta(perguntaClient, valores);
            String[] numeros = organizarSequenciaValores(tipoCalc, perguntaClient, valores);
            BigDecimal[] resultadoBigDecimal = calcular(tipoCalc, numeros);
            String resposta = construirTexto(tipoCalc, resultadoBigDecimal, valores);
            //return perguntaFalada ? "PF:"+resposta: resposta;
            return resposta;
        }catch (Exception e){
            return perguntaFalada ? "Desculpe, não entendi o que você falou, talvez se você escrever eu possa entender melhor.": "Desculpe, não entendi.";
        }
    }

    public Double[] perguntarTest(String perguntaClient){
//        if(!perguntaClient.equals("10 é 25 porcentual quanto aumentou para 5")){
//            return VAR_TEST_CONTINUE;
//        }
        try{
            perguntaClient = tratarPerguntas(perguntaClient, true);
            String[] valores = extrairValores(perguntaClient);
            int tipoCalc = classificarTipoPergunta(perguntaClient, valores);
            String[] numeros = organizarSequenciaValores(tipoCalc, perguntaClient, valores);
            BigDecimal[] resultado = calcular(tipoCalc, numeros);
            Double[] returned = new Double[]{
                    Double.valueOf(tipoCalc),
                    resultado[0].doubleValue()
            };
            return returned;
        }catch(Exception e) {
            System.out.println(e.toString());
            return null;
        }
    }

    public String[] organizarSequenciaValores(int tipoCalc, String perguntaClient, String[] valores){
        if((tipoCalc == ADD || tipoCalc == SUB || tipoCalc == SIMPLE) && valores[1].contains("%")){
            invertDoubleArray(valores);
        }else if (tipoCalc == SIMPLE2){
            ordenar_simple2(perguntaClient, valores);
        }else if(tipoCalc == VARIACAO){
            ordernar_variacao(perguntaClient, valores);
        }else  if (tipoCalc == REGRA3 || tipoCalc == COMPARATIVO_PRECO){
            String[] valoresConverted = valores.clone();
            if(true)// 2 dos tipos nao sao sao iguais
            {
                if(true) //checkar se é a mesma medida tipo tempo peso, volume
                {
                    if (tipoCalc == REGRA3){
                        //o número que deverá ser descorberto retornara na ultima posicao do array [2].
                        ordenar_regra3(perguntaClient, valores);
                        valoresConverted = valores.clone();
                        converterMedidas(valoresConverted);

                    }else  if(tipoCalc == COMPARATIVO_PRECO){
                        //converte medidas para bases iguais de comparação
                        converterMedidas(valoresConverted);
                        //ordena pela menor quantidade e deixa mesma posicao valores com valoresConverted
                        ordernar_comparativo4valores(valoresConverted, valores);
                    }

                    return valoresConverted;
                }
            }
        }
        return valores;
    }

    //array sempre são passados por referencia, nÃo necessita de retorno
    public String tratarPerguntas(String pergunta, boolean perguntaFalada){
        //remove duplos espaços se tiver
        pergunta = pergunta.replaceAll("\\s{2,}", " ").trim();
        pergunta = corrigirEventuaisErrosPortugues(pergunta);
        pergunta = new ExtensoParaNumero().Converter(pergunta);

        //FUNCAO: corrige casas decimais ex: 96:10--> 96.10
        //pergunta = pergunta.replaceAll("(?<=\\d)(:)(?=\\d+)", ".");
        pergunta = pergunta.replaceAll(regex_trocarDoisPontoPorPonto, ".");

        if(perguntaFalada){
            //verica alguns casos especificos que troca o "e" por "."
            if(!temSinonimosVARIACAO(pergunta)){
                pergunta = pergunta.replaceAll(regex_trocarEporPonto, ".");
            }
        }
        if(jaTemAlgumValorAtribuido(pergunta)){//é regra de 3
            pergunta = pergunta.replaceFirst(regex_sinonimo_100per, "100%");
        }
        return pergunta;
    }

    //classifica qual tipo da pergunta
    public int classificarTipoPergunta(String perguntaClient, String[] valores){
        //SHORTCUS
        String shortcut_vlInicio  = "^(\\+|-)\\s*"+regex_floatNumbers+"\\s*%?\\s*=\\s*"+regex_floatNumbers;
        String shortcut_variacao  = "\\d\\s*("+getRegexMedidas(true)[0]+")?\\s*para\\s*(R?\\$\\s*)?-?\\d";
        //verifica se tem algum atalho antes das verificacoes shortcuts
        if(matcheAllString(perguntaClient, shortcut_variacao)){
            return VARIACAO;
        }else if(matcheAllString(perguntaClient, shortcut_vlInicio)){
            if(perguntaClient.startsWith("-")){
                return VL_INICIAL_AODIMINUIR;
            }else if(perguntaClient.startsWith("+")){
                return VL_INICIAL_AOAUMENTAR;
            }
        }

        boolean contemPorcentagem = valores[0].contains("%") || valores[1].contains("%");
        if(valores.length == 1 || valores == null){
            return -1;
        }else if(valores.length == 2){
            //logica se tiver 2 numerosInteiros é variacao
            if(!contemPorcentagem){
                if(temTempoVerbais(perguntaClient) || temSinonimosADICIONAR(perguntaClient) || temSinonimosDIMINUIR(perguntaClient, null) || temSinonimosVARIACAO(perguntaClient)){
                    return VARIACAO;
                }else if(false){
                    //check se tem sinonimo "variacao"...

                }else{
                    return SIMPLE2;
                }
            }//se tiver 1 porcentagem e 1 numero inteiro
            else if(contemPorcentagem){
                if(temSinonimosDIMINUIR(perguntaClient, valores)){
                    if(temSinonimoINICIO(perguntaClient) &&
                            !temValorDoNumeroInicial(perguntaClient)){
                        return VL_INICIAL_AODIMINUIR;
                    }else{
                        return SUB;
                    }
                }else if(temSinonimosADICIONAR(perguntaClient)){
                    if(temSinonimoINICIO(perguntaClient) && !temValorDoNumeroInicial(perguntaClient)){
                        return VL_INICIAL_AOAUMENTAR;
                    }else{
                        return ADD;
                    }
                }else if(temSinonimoINICIO(perguntaClient)){
                    return VL_INICIAL_AOAUMENTAR;
                }
                else{
                    return SIMPLE;
                }
            }
        }else
        {
            if(valores.length == 4){
                return COMPARATIVO_PRECO;
            }else if(valores.length == 3){
                return REGRA3;
            }
        }
        return -1;
    }

    private boolean matcheAllString(String perguntaClient, String regex_shortcut){
        Matcher m = Pattern.compile(regex_shortcut, Pattern.CASE_INSENSITIVE).matcher(perguntaClient);
        if(m.find()){
            String finded = m.group();
            //if(finded == perguntaClient){
            return true;
            //}
        }
        return false;
    }

    public BigDecimal[] calcular(int tipoCalc, String[] valores){

        //limitar 16 casas decimais com valores apos a virgula, caso o resultado exato tenha um número infinito de decimais apos a divisao.
        MathContext mc = new MathContext(16, RoundingMode.HALF_UP) ;

        BigDecimal[] result = new BigDecimal[2];
        BigDecimal um = new BigDecimal("1");
        BigDecimal cem = new BigDecimal("100");

        BigDecimal[] all = extrairNumeros(null, valores);
        BigDecimal[] number = extrairNumeros(false, valores);
        //extrai apenas os que contem %
        BigDecimal[] percent = extrairNumeros(true, valores);
        //String a = number[1].toString();

        switch(tipoCalc){
            case SIMPLE:
                result[0] = number[0].multiply((percent[0]).divide(cem, mc)); //number[0] * (percent[0] / 100)
                break;
            case SIMPLE2:
                result[0] = (cem.multiply(number[0])).divide(number[1], mc);//(100 * number[0])/ number[1]
                break;
            case ADD:
                result[0] = number[0].multiply(um.add(percent[0].divide(cem, mc)));//number[0] * (1+ percent[0] / 100)
                break;
            case SUB:
                result[0] = number[0].multiply(um.subtract(percent[0].divide(cem, mc)));//number[0] * (1- percent[0] / 100)
                break;
            case VARIACAO:
                result[0] = (number[1].divide(number[0], mc).subtract(um)).multiply(cem);//(number[1] / number[0]  - 1)* 100
                break;
            case VL_INICIAL_AOAUMENTAR:
                result[0] = number[0].divide(um.add((percent[0].divide(cem))),mc);//number[0] /(1+(percent[0]/100));
                break;
            case VL_INICIAL_AODIMINUIR:
                result[0] = number[0].divide(um.subtract(percent[0].divide(cem)),mc);//number[0] /(1-(percent[0]/100));
                break;
            case REGRA3:
                //venho de shortcut ex: 10% = 30
                if(all.length == 2){
                    result[0] = (cem.multiply(all[0])).divide(all[1], mc); //(all[2] * all[0])/ all[1];6 5 3
                }

                if(percent.length == 2){
                    result[0] = (all[2].multiply(all[0])).divide(all[1], mc); //(all[2] * all[0])/ all[1];
                }else{
                    result[0] = (all[2].multiply(all[1])).divide(all[0], mc); //(all[2] * all[1])/ all[0]; (20 * 10)/ 100
                }
                break;
            case COMPARATIVO_PRECO:
                double[] precoPorUnidade =  {
                        all[1].doubleValue()/ all[0].doubleValue(),// price / qnt
                        all[3].doubleValue()/ all[2].doubleValue(),// price / qnt
                };
                Arrays.sort(precoPorUnidade);
                double percentEconomy = 100 - (100 * precoPorUnidade[0]) / precoPorUnidade[1];

                //menor_qtdA / precoA x maior_qtdB - precoB
                result[0] = all[1].divide(all[0], mc).multiply(all[2]).subtract(all[3]);
                result[1] = BigDecimal.valueOf(percentEconomy);
                break;
        }
        return result;
    }

    private String formatarValores(String[] valores){
        BigDecimal[] numbers = extrairNumeros(null, valores);
        DecimalFormat df = new DecimalFormat("#,###.##########");
        DecimalFormat dm = new DecimalFormat("#,##0.00");
        String[] medidas = new String[valores.length];
        Arrays.fill(medidas, "");
        String simbolResult = "";
        for (int i = 0; i < numbers.length; i++) {
            medidas[i] = valores[i].replaceAll("[0-9,.-]+", "");
            if(medidas[i].contains("$")){
                valores[i] = valores[i].replaceAll("[0-9,.-]+", dm.format(numbers[i]));
            }else{
                valores[i] = valores[i].replaceAll("[0-9,.-]+", df.format(numbers[i]));
            }
        }
        if(medidas.length > 2){
            if(medidas[0].equalsIgnoreCase(medidas[1])){
                simbolResult = medidas[2];}
            else if(medidas[0].equalsIgnoreCase(medidas[2])){
                simbolResult = medidas[1];}
            else if(medidas[1].equalsIgnoreCase(medidas[2])){
                simbolResult = medidas[0];
            }else{
                simbolResult = medidas[1];
            }
        }else if(medidas.length <= 2){
            if(medidas[0].equals("%") && !medidas[1].equals("")){
                simbolResult = medidas[1];
            }else if (medidas[1].equals("%") && !medidas[0].equals("")){
                simbolResult = medidas[0];
            }
        }
        return simbolResult;
    }

    private String construirTexto(int tipoCalc, BigDecimal[] resultado, String[] valores) {
        String simbolResult = formatarValores(valores);
        BigDecimal[] numbers = extrairNumeros(null, valores);
        DecimalFormat dfNormal = new DecimalFormat("#,###.##########");
        DecimalFormat dfMoney = new DecimalFormat("#,##0.00");
        String str_result  = simbolResult.contains("$") ? simbolResult + dfMoney.format(resultado[0]): dfNormal.format(resultado[0]) + simbolResult;
        String str_resposta = "", messageFormat = "";
        if(tipoCalc == REGRA3){
            str_resposta = MessageFormat.format("se {0} é {1}, então {2} é {3}", valores[0], valores[1], valores[2], str_result);
        }else if (tipoCalc == COMPARATIVO_PRECO){
            String strmedida = valores[0].replaceAll("[0-9,.-]+", "").trim();
            messageFormat = "Comprando {0} por {1} você economizará {2}\nO preço por {3} é {4, number, #.##}% mais barato!";
            //resultado < 0
            if(resultado[0].compareTo(BigDecimal.ZERO) > 0){
                str_resposta = MessageFormat.format(messageFormat, valores[2], valores[3], str_result, strmedida, resultado[1]);
            }else {
                str_result = str_result.replace("-", "");
                str_resposta = MessageFormat.format(messageFormat, valores[0], valores[1], str_result, strmedida, resultado[1]);
            }
        }else{
            int[] index = {0, 1};
            switch(tipoCalc){
                //[0]&{0} is the percent, 1 = number, 2 = result because organizarValores();
                case SIMPLE:
                    messageFormat = "{0} de {1} é {2}";
                    break;
                case ADD:
                    messageFormat = "{1} + {0} = {2}";
                    break;
                case SUB:
                    messageFormat = "{1} - {0} = {2}";
                    break;
                case VL_INICIAL_AOAUMENTAR:
                    messageFormat = "Se um valor apos aumentar {0} é resultado de {1} entao o valor inicial é {2}";
                    break;
                case VL_INICIAL_AODIMINUIR:
                    messageFormat = "Se um valor apos diminuir {0} é resultado de {1} entao o valor inicial é {2}";
                    break;
                //[0]&{0} is the number, 1 = number, 2 = result
                case SIMPLE2:
                    messageFormat = "{0} é {2}% de {1}";
                    break;
                case VARIACAO:
                    //numbers[0] >= number[1]
                    if(numbers[0].compareTo(numbers[1]) >= 0){
                        messageFormat = "A diminuição percentual de {0} para {1} é de {2}%";

                    }else{
                        messageFormat = "O aumento percentual de {0} para {1} é de {2}%";

                    }
                    break;
            }
            str_resposta = MessageFormat.format(messageFormat, valores[0], valores[1], str_result);
        }
        return str_resposta;
    }

    private boolean temTempoVerbais(String perguntaClient){
        return stringContainsItemFromList(perguntaClient, getTemposVerbais());
    }

    private String[] getTemposVerbais(){
        return new String[]{
                "estava",
                "antes",
                "era",
                "passou de",
                "deixou de ser",
                "pagava",
                "cobrava",
                //Passado: <=6 (indicePassado)
                //Futuro >=7
                "foi para",
                "passou para",
                "passou a ser",
                "depois",
                "agora é",
                "agora",
                "atual",
        };
    }

    private int ordernar_variacao(String perguntaClient, String[] valores){
        int indicePassado = 6;
        String[] temposVerbais = getTemposVerbais();
        //Double[] doubleValores = extrairNumeros(valores, false);
        BigDecimal[] ordemOriginal = extrairNumeros(false, valores);
        BigDecimal[] copiaValores = ordemOriginal.clone();
        boolean temTemposVerbais = stringContainsItemFromList(perguntaClient, temposVerbais);
        if(temSinonimosADICIONAR(perguntaClient)){
            // menor para maior
            Arrays.sort(copiaValores);
        }else if(temSinonimosDIMINUIR(perguntaClient, null)){
            //maior para menor
            Arrays.sort(copiaValores, Collections.reverseOrder());
        }else if(temTemposVerbais){
            String regex_tempoVerbais = TextUtils.join("|", temposVerbais); //TextUtils.join in ANDROID
            String partRegex = regex_tempoVerbais.replaceAll("\\s+", "\\\\s*"); //troca espaços por codigo regex
            Pattern textNumberOrNumberText = Pattern.compile("("+partRegex+")" + "\\s*"+regex_floatNumbers+"|"+regex_floatNumbers+"("+partRegex+")", Pattern.CASE_INSENSITIVE);
            int i = 0;
            boolean achouAlgumaOcorrencia = false;

            while(i++<2 && !achouAlgumaOcorrencia){ //2 loops
                //1° procura sem filtrar, 2° procura com filtro
                Matcher m = textNumberOrNumberText.matcher(perguntaClient);
                if (m.find()) {
                    achouAlgumaOcorrencia = true;
                    String text = m.group(0).replaceAll("[^a-zA-Z]", "");
                    int indice = Arrays.asList(temposVerbais).indexOf(text);
                    //text = m.group(0).replaceAll(",", "\\.");
                    text = extrairValores(m.group())[0];
                    //double value = Double.parseDouble(text.replaceAll("[^0-9.-]", ""));
                    BigDecimal value = extrairNumeros(false, text)[0];
                    if(indice <= indicePassado){
                        //passado é o primeiro item
                        if(value.compareTo(copiaValores[0]) != 0){
                            invertDoubleArray(copiaValores);
                        }
                    }else{
                        //futuro é o primeiro item
                        if(value.compareTo(copiaValores[1]) != 0){
                            invertDoubleArray(copiaValores);
                        }
                    }
                }else//filtra. deixando palavras especificas e numeros na pergunta
                {
                    //new Regex2: \b(?!(antes|agora|\d+)\b)(\w+|\d+|\s+|[^a-zA-Z]+\s+?)
                    perguntaClient = perguntaClient.replaceAll("\\b(?!("+regex_tempoVerbais+"|\\d+)\\b)(\\w+|\\d+|\\s+|[^a-zA-Z]+\\s+?)", "");
                }
            }
        }//senao nada faz
        if(ordemOriginal[0].compareTo(copiaValores[0]) != 0 && ordemOriginal[1].compareTo(copiaValores[1]) != 0){
            invertDoubleArray(valores);
        }
        return 1;
        //return copiaValores[0].doubleValue() >= copiaValores[1].doubleValue()? DIMINUIU: AUMENTOU;
    }

    //[0] = numero que tenho resultado de porcentagem
    //[1] = resultado da porcentagem do numero 0
    //[2] = numero que tenho, no qual desejo descobri a porcentagem
    private void ordenar_regra3(String perguntaClient, String[] valores){
        //REGEX0 ; \d+%?\s*(est(á|a) para|(que\s*)?(é?|for?)\s*(=|igual|(equivale|corresponde|representa)\w*)?\s*(é?a?o?|(ao\s*(valor|numero)?)))\s*\d+(\s*%)?|[0-9](\s*%)?
        //String regex = "-?\\d+((,|\\.)\\d+)?%?\\s*(est(á|a) para|(que\\s*)?(é?|for?)\\s*(=|igual|(equivale|corresponde|representa)\\w*)?\\s*(é?a?o?|(ao\\s*(valor|numero)?)))\\s*-?(\\.|,)?\\d(.*?)(\\s*%)?(?!(\\d|\\.\\d|,\\d))|[0-9](\\s*%)?";
        String[] grupoDescoberto = new String[2];
        String grupoADescobrir = "";
        //replace scape regex.
        String[] valores_regex = valores.clone();
        for (int i = 0; i < valores.length; i++) {
            valores_regex[i] = valores[i].replaceAll("\\$", "\\\\\\$");
        }

        List<String> regexs = new ArrayList<>();
        /*coloca todas posicoes possiveis de dois números sem repetir as combinaçoes
            int[] array = {1,2,3,4};
            {0,1},{1,0},{0,2},{2,0},{0,3},{3,0},{1,2},{2,1},{1,3},{3,1},{2,3},{3,2}   */
        for (int i = 0; i < valores_regex.length - 1; i++) {
            for (int j = i + 1; j < valores_regex.length; j++) {
                regexs.add(valores_regex[i]+regex_sinonimoEquivalencia+valores_regex[j]);
                regexs.add(valores_regex[j]+regex_sinonimoEquivalencia+valores_regex[i]);
            }
        }

        for (int i = 0; i < regexs.size(); i++){
            Pattern p = Pattern.compile(regexs.get(i), Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(perguntaClient);
            if(m.find()){
                grupoDescoberto = extrairValores(m.group(0));
                if(i < 2) i = 2;
                else if (i < 4) i = 1;
                else if (i >= 4) i = 0;
                grupoADescobrir = valores[i];
                break;
            }
            //acabou
            if(i == regexs.size() -1){
                grupoDescoberto[0] = valores[0];
                grupoDescoberto[1] = valores[1];
                grupoADescobrir = valores[2];
            }
        }

        //
        String medida = grupoADescobrir.replaceAll("[0-9,.%-]+", "");
        String tipo_medida = converterMedidasTipo(medida);


        if(grupoDescoberto[0] != null && grupoDescoberto[1] != null){
            if(!grupoDescoberto[1].contains("%") && grupoDescoberto[0].contains("%")){
                invertDoubleArray(grupoDescoberto);
            }else{
                String medida_descoberto = grupoDescoberto[1].replaceAll("[0-9,.%-]+", "");
                String tipo_medida_descoberto = converterMedidasTipo(medida_descoberto);
                if(!medida.isEmpty() && tipo_medida_descoberto.equals(tipo_medida)){
                    invertDoubleArray(grupoDescoberto);
                }
            }
            valores[0] = grupoDescoberto[0];
            valores[1] = grupoDescoberto[1];
            valores[2] = grupoADescobrir;
        }
    }



    //retorna o clone convertido
    public void ordernar_comparativo4valores(String[] valoresClone, String[] valoresOriginal){

        //tree map ordena os valores apos inserir
        Map<BigDecimal, String[]> mapClone = new TreeMap<BigDecimal, String[]>();
        Map<BigDecimal, String[]> mapOriginal = new TreeMap<BigDecimal, String[]>();

        String[] monetarios = {"$", "reais", "real", "dolar", "dolares", "euro", "euros"};
        for (int i = 0; i < valoresClone.length; i++){
            if(i % 2 == 0){ //se for par
                //inverte a posicao da casa deixando sempre monetario apos a medida ex; {20ml, R$ 30, 40ml, R$10}
                if(stringContainsItemFromList(valoresClone[i], monetarios)){
                    String temp = valoresClone[i];
                    valoresClone[i] = valoresClone[i+1];
                    valoresClone[i+1] = temp;

                    //repete com original
                    String tempOriginal = valoresOriginal[i];
                    valoresOriginal[i] = valoresOriginal[i+1];
                    valoresOriginal[i+1] = tempOriginal;
                }
                //ordenacao pelos valores das medidas. Ex: 20ml, $10, 5ml , $7 >será> 5ml, $7, 20ml, $10

                BigDecimal chave_valor = extrairNumeros(null, valoresClone[i])[0];
                String[] grupoValoresClone = { valoresClone[i], valoresClone[i+1] };

                //repete com original
                String[] grupoValoresOriginal = { valoresOriginal[i], valoresOriginal[i+1] };

                //se ja tem uma chave com o mesmo valor ex: 10 "kg", acrescento alguns milesimos para nao substituir o valor da chave.
                if(mapClone.get(chave_valor) != null){
                    chave_valor = chave_valor.add(new BigDecimal("0.0000000001"));
                }
                mapClone.put(chave_valor, grupoValoresClone);
                mapOriginal.put(chave_valor, grupoValoresOriginal);
            }
        }
        int i = 0;
        for(BigDecimal key : mapClone.keySet()) {
            //print map. test to verify if order by medida;
//            System.out.println(Arrays.toString(map.get(key)));
            String[] values = mapClone.get(key);
            String[] original = mapOriginal.get(key);

            valoresClone[i] = String.valueOf(values[0]);
            valoresOriginal[i++] = String.valueOf(original[0]);

            valoresClone[i] = String.valueOf(values[1]);
            valoresOriginal[i++] = String.valueOf(original[1]);
        }
    }

    private int ordenar_simple2(String perguntaClient, String[] valores){
        BigDecimal[] ordemOriginal = extrairNumeros(false, valores);
        BigDecimal[] copiaValores = ordemOriginal.clone();

        Matcher m = Pattern.compile(regex_valorAtribuidoParaVariacao, Pattern.CASE_INSENSITIVE).matcher(perguntaClient);
        if (m.find()) {
            BigDecimal value = new BigDecimal(m.group(0).replaceAll("[^0-9]", ""));
            if(value.compareTo(copiaValores[1]) != 0){
                invertDoubleArray(copiaValores);
            }
        }else{
            //o numero apos o conector "de" deve ser o final
            m = Pattern.compile(regex2_valorAtribuidoParaVariacao2, Pattern.CASE_INSENSITIVE).matcher(perguntaClient);
            if(m.find()){
                BigDecimal value = extrairNumeros(false, m.group().replaceAll("de\\s*", ""))[0];
                if(value.compareTo(copiaValores[1]) != 0){
                    invertDoubleArray(copiaValores);
                }
            }
        }
        if(ordemOriginal[0].compareTo(copiaValores[0]) != 0 && ordemOriginal[1].compareTo(copiaValores[1]) != 0){
            invertDoubleArray(valores);
        }
        return SIMPLE2;
    }

    //verifica se ja tem valor do numeroInicial
    public boolean temValorDoNumeroInicial(String pergunta){
        //REGEX ORIGINAL: inici\w+\s*?(=?)(de\s*numero\s*)?(\s*que(\s*e)?\s*)?(igual|(equivale|corresponde|representa)\w*)?\s*?(e?a?o?|(ao\s*?(valor|numero)?))\s*?(-\s*)?\d+
        String regex = regex_temValorInicial;
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(pergunta);
        return m.find();
    };

    private void invertDoubleArray(Object[] array){
        Object temp = array[1];
        array[1] = array[0];
        array[0] = temp;
    }

    private String corrigirEventuaisErrosPortugues(String pergunta){
        return corrigirQUANTO(corrigirPORCENTO(pergunta));
    }

    private String corrigirPORCENTO(String pergunta){
        final String source = pergunta;
        final String replacement = "%";
        final String result = Pattern.compile(regex_porcento, Pattern.CASE_INSENSITIVE).matcher(source).replaceAll(Matcher.quoteReplacement(replacement));
        //substitui qualquer palavra "por cento" ou que inicia com PO e termina TO ou TOS
        return result;
        /*Ex:  porcentos, porcento porcento porcerto porceto poncento poncerto pocento
               poceto por cento porsento porsento porseto porsanto porsato, porcentual, percentual, */
    }

    private String corrigirQUANTO
            (String pergunta){
        //substitui qualquer palavra que inicia com QU|CU|CO e termina com TO|TU|TOS|TUS
        return pergunta.replaceAll(regex_quanto, "quanto");
        /*Ex:  quamtos, quantus, cuantos, quantos, quamtus, coantus,quantu ,quamto,
               quamtu ,cuanto, cuantu, cuamto ,coanto  ,cuãnto...*/
    }

    private boolean temSinonimosADICIONAR(String pergunta){
        //+, adicionar, mais, aumentar, somar, colocar, por, incluir
        String[] sinonimosADD = new String[]{
                "+",
                "acresc", //acrescentar, acrescimo
                "adicion", //adicionar, adicione, adiciona
                "aume", //aumetar, aumentar, aumente, aumenta,
                "alme", //forma errada de aumentar
                "som", //somar some, soma
                "colo", //coloca, colocar, coloque
                "inclu", //inclua, incluir, inclue
                "apli", //aplicar, aplique
                "inserir",
                "insira",
                "mais",
                " por ",
                "poe",
                "ponha",
                "suba",
                "subi", //subiu , subir
                "cresce", //crescer, cresceu

        };
        return stringContainsItemFromList(pergunta, sinonimosADD);
    }

    private boolean temSinonimoINICIO(String pergunta){
        String[] sinonimosInicio = new String[]{
                "começ",
                "inici", //inicio, inicial
                "antes",
                "origem",
                "prim", //primeiro, primario
                "base"
        };
        return stringContainsItemFromList(pergunta, sinonimosInicio);
    }

    private boolean temSinonimosDIMINUIR(String pergunta, String[] valores){
        //Math -1% | -11% | -1.1% | -11,111% | - 111.11 %
        boolean temNegativoPorcentagem = Pattern.compile(regex_negativeNumbersPercent).matcher(pergunta).find();
        if(temNegativoPorcentagem){
            return true;
        }

        String[] sinonimosSUB = new String[]{
                //"- ", //confunde com valores negativos
                "retir", //retire, retira, retirar
                "tir", //tire, tirar
                "reduz", //reduza, reduzir
                "diminu", //diminuir, diminua, diminue, diminui
                "deminu", //forma errada de deminuir
                "subtra", //subtrai subtraÃ­a, soma
                "sobre traia", //eventual input do google mic
                "remo", //remocao, remover, remove, remova, removi
                "menos",
                "apag", //apague, apagar, apaguei
                "elimin", //elimine, eliminar, eliminei
                "dispens", //dispensar, dispense, dispensei
                "abat", //abata, abater , abati
                "desce", //desceu, descer,desci
                "abaix", //abaixa/abaixe/abaixar/abaixei
                "delet", //deletei/ detelar
                "descont", //desconto, descontar
        };

        return stringContainsItemFromList(pergunta, sinonimosSUB);
    }

    private boolean temSinonimosVARIACAO(String pergunta){
        String[] sinonimosSUB = new String[]{
                "difere", //difere, diferente, diferença
                "varia", //varia, variacao
        };
        return stringContainsItemFromList(pergunta, sinonimosSUB);
    }

    private boolean jaTemAlgumValorAtribuido(String perguntaClient){
        //REGEX3 = -?\d+((,|\.)\d+)?%?\s+(que\s*)?(é?|for?)\s*(=|igual|(equivale|corresponde|representa)\w*)?\s*(é?a?o?|(ao\\s*(valor|numero)?))\s*-?\d+((,|\.)\d+)?(\s*%)?
        Pattern p = Pattern.compile(regex_valor_atribuido, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(perguntaClient);
        return m.find();//retorna se encontrar
    }

    //Exemplo :["10", "5%"] -> return case; true = 5, false = 10, null = 10 and 5
    private BigDecimal[] extrairNumeros(Boolean percentNumber, String... valores){
        List<BigDecimal> numbers = new ArrayList<BigDecimal>();
        for (String valor : valores) {
            //deixa so numeros, pontuacoes e simbolo
            valor = valor.replaceAll("[^0-9-.,%]+", "");
            //o ultimo simbolo define casa decimal
            if(valor.contains(".") && valor.contains(",")){
                //deixa somente o ultimo . ou ,
                valor = valor.replaceAll(allExceptLastDot, "");
            }else{
                valor = valor.replaceAll(regex_treeDigitsIsNotDecimal, "");
            }
            valor = valor.replaceAll(",", "\\.");

            if(valor.contains("-") && valor.contains("%")){
                valor = valor.replaceAll("-", "");
            }

            if(percentNumber == null || percentNumber && valor.contains("%")){
                numbers.add(new BigDecimal(valor.replace("%", "")));
            }else if(!percentNumber && !valor.contains("%")){
                numbers.add(new BigDecimal(valor));
            }
        }
        return numbers.toArray(new BigDecimal[0]);
    }
    /*
        extrai os valores inteiros ou numeros simbolos porcentagem.
        1000.00 -> 1000.00 || 10.00 -> 10.00 || 1.000,005 -> 1000.005
        1.000.005 ->1000005.00 || 1.000.05 -> 1000.05 ||  1.000.5 -> 1000.50
        1.000.000 -> 1000000.00 || 10 % -> 10%
    */
    private String[] extrairValores(String pergunta){
        //remove todos espaços antes e pois do simbolo % para juntar
        pergunta = pergunta.replaceAll("\\s*%", "%");
        List<String> numbers = new ArrayList<>();
        String r = getRegexMedidas(true)[0];
        //Math 10.40 ou 0.40 ou .40 ou 10.40%
        //Matcher m = Pattern.compile("\\d*\\.?\\d+%?").matcher(pergunta);
        //Math 10.40 or 0.40 or .40 or 10.40% or 1.000,00 or -1.000 % or -.5
        Matcher m = Pattern.compile(regex_estrairValores, Pattern.CASE_INSENSITIVE).matcher(pergunta);
        while (m.find()) {
            String match = m.group(0);
            numbers.add(match);
        }
        return numbers.toArray(new String[0]);
    }

    //ULTIMA MODIFICACAO
    private String[] getRegexMedidas(boolean removeNumberAndLastChar){
        //regex fator_convertao tipo_medida
        String[] tiposMedidas = {
                "s(eg(undo)?s?)?1t", "min(utos?)?60t", "horas?3600t", "hr?3600t", "dias?86400t", "semanas?604800t", "m(e|ê)s(es)?2592000t", "anos?31104000t",
                "m(etros?)?1c", "cm0.01c", "centimetros?0.01c", "quil(ô|ó|o)metros?1000c",  "kms?1000c",  "milhas?1609.34c",
                "g(ramas?)?1p", "t(oneladas?)?1000000p","quilo(gramas)?s?1000p", "kgs?1000p", "miligramas?0.001p", "mgs?0.001p",
                "mililitros?1v", "mls?1v", "l(t|ts?)?1000v", "litros?1000v",
                //1 default, para nao precisar multiplicar/converter
                "rea(l|is)1m", "euros?1m", "dolare?s?1m",
                "amperes1", "a1", "b1","x1","y1", "cº?1", "graus1", "w(atts?)?1", "v(olts?)?1", "j(oules?)?1"
        };

        if(removeNumberAndLastChar){
            String concat = TextUtils.join("|", tiposMedidas);
            concat = concat.replaceAll("[0-9.]+\\w?", "");
            tiposMedidas = new String[] { concat };
        }
        return tiposMedidas;
    }

    //    private static String medida_tempo = "t";
//    private static String medida_comprimento = "c";
//    private static String medida_peso = "p";
//    private static String medida_volume = "v";
//    private static String medida_monetario = "m";
//    private static String medida_todos = "all";
//
//    private String[] getMedidas(String tipo_medida){
//        List<String[]> medidas = new ArrayList<>();
//        String[] tempo = {  "s(eg(undo)?s?)?1", "min(utos?)?60", "horas?3600", "hr?3600", "dias?86400", "semanas?604800", "m(e|ê)s(es)?2592000", "anos?31104000" };
//        String[] comprimento = { "m(etros?)?1", "cm0.01", "centimetros?0.01", "quil(ô|ó|o)metros?1000",  "kms?1000",  "milhas?1609.34" };
//        String[] peso = { "g(ramas?)?1", "t(oneladas?)?1000000","quilo(gramas)?s?1000", "kgs?1000", "miligramas?0.001", "mgs?0.001" };
//        String[] volume = { "mililitros?1", "mls?1", "l(t|ts?)?1000", "litros?1000" };
//        String[] monetario = { "rea(l|is)1", "euros?1", "dolare?s?1" };
//
//        return null;
//    }
    public String converterMedidasTipo(String medida){
        String[] tiposMedidas = getRegexMedidas(false);

        for (String str : tiposMedidas) {
            String cleanRegexNotNumberAndChar = str.replaceAll("[0-9.]+\\w", "");
            Pattern p = Pattern.compile("\\b"+cleanRegexNotNumberAndChar+"\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(medida.trim());
            if(m.find()){
                String tipo_medida = str.replaceAll(".*(?!$)", "");
                return tipo_medida;
            }
        }
        return medida;
    }
    //converter Litros em minilitros e kilos em gramas
    public void converterMedidas(String[] valoresClone){
        String[] tiposMedidas = getRegexMedidas(false);

        String medida1 = valoresClone[0].replaceAll("[0-9,.-]+", "").trim();
        String medida2 = valoresClone[2].replaceAll("[0-9,.-]+", "").trim();

        String menor_medida_do_fator;
        if(!medida1.equals(medida2)){
            boolean m1Ok = false, m2Ok = false;
            for (String str : tiposMedidas) {
                double fator_multiplicador = Double.parseDouble(str.replaceAll("[^0-9.]+", ""));
                String cleanRegexNotNumber = str.replaceAll("[0-9.]+\\w", "");
                Pattern p = Pattern.compile("\\b"+cleanRegexNotNumber+"\\b", Pattern.CASE_INSENSITIVE);
                Matcher m1 = p.matcher(medida1);
                Matcher m2 = p.matcher(medida2);
                if(!m1Ok && m1.find()){
                    BigDecimal valMedida1 = extrairNumeros(false, valoresClone[0])[0];
                    BigDecimal converted = valMedida1.multiply(BigDecimal.valueOf(fator_multiplicador));
                    valoresClone[0] = converted + medida1;
                    m1Ok = true;
                }
                if(!m2Ok && m2.find()){
                    BigDecimal valMedida2 = extrairNumeros(false, valoresClone[2])[0];
                    BigDecimal converted = valMedida2.multiply(BigDecimal.valueOf(fator_multiplicador));
                    //val > 1
                    valoresClone[2] = converted + medida2;

                    m2Ok = true;
                }

                if(m1Ok && m2Ok) break;
            }
        }
    }

    private static boolean stringContainsItemFromList(String inputStr, String[] items) {
        //return Arrays.stream(items).anyMatch(inputStr::contains); // API 24+
        for (int i = 0; i < items.length; i++) {
            if(inputStr.toLowerCase().contains(items[i].toLowerCase())){
                return true;
            }
        }
        return false;
    }
}

/*
10.000.000 = 10 MILHOES
10.000.000.12 = 10 MILHOES E 12 CENTAVOS
10.000.000.120 = 10 BILHOES E 120
10.000.000,120 = 10 MILHOES E 12 CENTAVOS
10.000.000,1200 = 10 MILHOES E 12 CENTAVOS
10.000.000.1200 = 10 MILHOES E 12 CENTAVOS
10000000.120 = 10 MILHOES E 12 CENTAVOS


*/


/*
regex reference:
site tested: https://regexr.com/
https://stackoverflow.com/questions/30960842/how-to-include-accented-words-in-regex

https://stackoverflow.com/questions/2254749/regex-match-word-that-ends-with-id
*/