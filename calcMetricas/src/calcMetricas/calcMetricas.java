/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package calcMetricas;

import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
//import java.util.Arrays;
//import java.util.Vector;



/**
 * @file calcMetricas.java
 * @brief fichero que realiza el cálculo de las métricas para regresión y clasificación
 * @author Juan Carlos Gámez
 * @version 0.1
 * @date sept 2014
 * @note Implementación del cálculo de las métricas para regresión y clasificación
 */
public class calcMetricas {

  private static boolean debug=false;
  private static DecimalFormat format= new DecimalFormat("##0.000");
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {

      String sintaxis= writeSyntax("calcMetricas");
      String[] files= checkSyntaxAndFiles(args.length, args, sintaxis);
      String resultString= "";
      
      if (files[0].equals("")){
          System.out.println(files[1]);
      }      
      else{
        resultString= executeCalcMetricas(files[0], files[1], files[2]);
        System.out.println(resultString);
      }
    
  }
  
    /**
     * execute the calcMetricas 
     * @param resultAlgorithm file with results of algorithm (see read... for more details)
     * @param resultMetrics file with metrics calculed by application (see write... for more details)
     */
    public static String executeCalcMetricas(String fileResAlgorithm, String fileMetrics, String betaString){

        String auxString="";
        String resultString= "";
        double beta= Double.parseDouble(betaString);

        // creamos el fichero de salida
        if (initResFile(fileMetrics) == -1){
            resultString= "ERROR: Init File: " + fileMetrics;
            return resultString;
        }
        System.out.println("Fichero Resultados: " + fileMetrics);

        // leemos los datos del fichero de resultados
        double[][] results= readFileData(fileResAlgorithm); //[ejemplo][0] -> real; [ejemplo][1] -> estimado; 
        // damos valores a las matrices de resultados y de límites de las clases
        int numClases= (int) results[0][0]; // indicará el número de clases
        int regresion= (int) results[0][1]; // indicará si es están dando valores reales (de regresión) o directamente las clases
        int numEjemplos=0;
        if (regresion == 1){
            numEjemplos= results.length - numClases - 1;
        }
        else{
            numEjemplos= results.length - 1;            
        }
        if (numClases == 0){
            resultString= "ERROR: number of classes == 0 \n";
            return resultString;
        }
        if (numEjemplos == 0){
            resultString= "ERROR: number of examples == 0 \n";
            return resultString;
        }
        double[][] defClases= new double[numClases][2];        
        double[][] values= new double[numEjemplos][2];
        double[][] clases= new double[numEjemplos][2];
        double[][] confusion= new double[numClases+1][numClases+1];// matriz de confusion (con las filas y columnas de las sumas)
        for (int j= 0; j < numClases+1; j++){
            for (int k= 0; k < numClases+1; k++){
                confusion[j][k]= 0;
            }
        }
        if (regresion == 1){
            for (int j= 0; j < numClases; j++){
                defClases[j][0]= results[j+1][0];
                defClases[j][1]= results[j+1][1];
            }
            for (int j= 0; j < numEjemplos; j++){
                values[j][0]= results[j+numClases+1][0];
                values[j][1]= results[j+numClases+1][1];
                //identificar a qué clase pertenece el valor real y predicho
                int clasReal=  getClass(values[j][0], defClases);
                if (clasReal == -1){
                    resultString= "ERROR: define class of example: " + j +" - real value:" + values[j][0];
                    return resultString;
                }
                clases[j][0]= clasReal;
                int clasPred=  getClass(values[j][1], defClases);
                if (clasPred == -1){
                    resultString= "ERROR: define class of example: " + j +" - predicted value:" + values[j][1];
                    return resultString;
                }
                clases[j][1]= clasPred;

                confusion[clasReal][clasPred]++; // n_i_j
                confusion[clasReal][numClases]++; // n_i*
                confusion[numClases][clasPred]++; //n*_j            
                confusion[numClases][numClases]++; //N
            }
        }// if (regresion == 1){
        else{
            for (int j= 0; j < numEjemplos; j++){
                int clasReal=  (int) results[j+1][0];
                int clasPred=  (int) results[j+1][1];
                if (clasReal < 0){
                    resultString= "ERROR: RealClass must be > 0 ; example: " + j +" - value:" + clasReal;
                    return resultString;                  
                }
                if (clasPred < 0){ // no ha encontrado predicción posible -> lo cuento como el máximo error
                  if (clasReal < (numClases / 2)){
                    clasPred= numClases-1;
                  }
                  else{
                    clasPred= 0;
                  }
                }
                clases[j][0]= clasReal;
                clases[j][1]= clasPred;

                confusion[clasReal][clasPred]++; // n_i_j
                confusion[clasReal][numClases]++; // n_i*
                confusion[numClases][clasPred]++; //n*_j            
                confusion[numClases][numClases]++; //N            
            }
        }// else ... if (regresion == 1){
        
        // presentar la matriz de confusión
        auxString="-- Confusion Matrix -- \n";
        for (int j=0; j < numClases; j++){
            for (int k=0; k < numClases; k++){
                auxString+= (int) confusion[j][k] + "\t";
            }
            
            auxString+= "|\t" + (int) confusion[j][numClases] + "\n";
        }
        for (int j=0; j < numClases; j++){
            auxString+= "------\t";
        }
        auxString+="|\t------\n";
        for (int j=0; j < numClases; j++){
           auxString+= (int) confusion[numClases][j] + "\t";
        }
        auxString+= "|\t" + (int) confusion[numClases][numClases] + "\n";
        writeResFile(fileMetrics, auxString);
        System.out.println(auxString);

        // comprobación matriz confusión
        int sumaFila=0, sumaColumna=0;
        for (int j=0; j < numClases; j++){
            sumaFila+= confusion[j][numClases];
            sumaColumna+= confusion[numClases][j];            
        }
        if (sumaFila != sumaColumna || sumaFila != confusion[numClases][numClases] || sumaFila != numEjemplos){
            resultString= "ERROR: confusion matrix sumRow: " + sumaFila +", sumCol: " 
                    + sumaColumna + ", N: " + confusion[numClases][numClases]
                    + ", Examples: " + numEjemplos;
            return resultString;            
        }
        
        // presentar los datos de los resultados
        auxString="#numberOfClasses \n" +
                "#RegressionValues (if RegressionValues == 0 -> there aren't definitions of class, directly with @data)\n" +
                "#Class1 inittial\n" +
                "#Class2 inittial\n" +
                "#...\n" +
                "#ClassN inittial final \n";
        auxString+= numClases + "\n";
        auxString+= regresion + "\n";
        if (regresion == 1){
            for (int j=0; j < numClases-1; j++){
                auxString+= format.format(defClases[j][0]) + "\n";
            }
            auxString+= format.format(defClases[numClases-1][0]) + "\t" + format.format(defClases[numClases-1][1]) + "\n";
        }// if (regresion == 1){
        auxString+= "@metrics\n";
        writeResFile(fileMetrics, auxString);
        System.out.println(auxString);
        
        
        //comenzamos con las métricas
        //métricas clasificacion
        // CCR
        auxString="-- Classification metrics -- \n";
        double CCR=0;
        for (int j=0; j < numClases; j++){
            CCR+= confusion[j][j];            
        }
        CCR= CCR / confusion[numClases][numClases];
        auxString+="CCR:\t" + format.format(CCR) + "\n";

        // TPR, Smin, MM, RECALL
        double[] TPR= new double[numClases];
        double TPRMedia=0, TPRMin=1;
        int TPRMinIndex= 0;
        for (int j=0; j < numClases; j++){
//            if (confusion[j][j] != 0 && confusion[j][numClases] == 0 ){
//                TPR[j]= -0;
//            }
            if (confusion[j][numClases] == 0 ){
                TPR[j]= 0;
            }
            else{
                TPR[j]= confusion[j][j] / confusion[j][numClases];
            }
            TPRMedia+= TPR[j];
            if (TPR[j] < TPRMin){
                TPRMin= TPR[j];
                TPRMinIndex= j;
            }
        }        
        TPRMedia= TPRMedia / (double) numClases;        
        auxString+="Recall or Sensibility (S:) or True Positive Rate (TPR:): \n";
        auxString+="\tRecallMin:(MS:)\t" + format.format(TPRMin) + "\n";
        auxString+="\tRecallMinIndex:\t" + TPRMinIndex + "\n";
        auxString+="\tRecallMacro-Media (MM):\t" + format.format(TPRMedia) + "\n";
        auxString+="\tRecallClasses:\t";
        for (int j=0; j < numClases; j++){
            auxString+= format.format(TPR[j]) + "\t";            
        }
        auxString+="\n";
        
        // Precision 
        double[] Prec= new double[numClases];
        double PrecMedia=0, PrecMin=1;
        int PrecMinIndex= 0;
        for (int j=0; j < numClases; j++){
//            if (confusion[j][j] != 0 && confusion[j][numClases] == 0 ){
//                TPR[j]= -0;
//            }
            if (confusion[numClases][j] == 0 ){
                Prec[j]= 0;
            }
            else{
                Prec[j]= confusion[j][j] / confusion[numClases][j];
            }
            PrecMedia+= Prec[j];
            if (Prec[j] < PrecMin){
                PrecMin= Prec[j];
                PrecMinIndex= j;
            }
        }        
        PrecMedia= PrecMedia / (double) numClases;        
        auxString+="Precision: \n";
        auxString+="\tPrecisionMin:(MS:)\t" + format.format(PrecMin) + "\n";
        auxString+="\tPrecisionMinIndex:\t" + PrecMinIndex + "\n";
        auxString+="\tPrecisionMacro-Media:\t" + format.format(PrecMedia) + "\n";
        auxString+="\tPrecisionClasses:\t";
        for (int j=0; j < numClases; j++){
            auxString+= format.format(Prec[j]) + "\t";            
        }
        auxString+="\n";

        // F1-Score 
        double[] F1= new double[numClases];
        double F1Media=0, F1Min=1;
        int F1MinIndex= 0;
        for (int j=0; j < numClases; j++){
//            if (confusion[j][j] != 0 && confusion[j][numClases] == 0 ){
//                TPR[j]= -0;
//            }
            if (Prec[j] + TPR[j] == 0 ){
                F1[j]= 0;
            }
            else{
                F1[j]= 2 * ((Prec[j] * TPR[j]) / (Prec[j] + TPR[j]));
            }
            F1Media+= F1[j];
            if (F1[j] < F1Min){
                F1Min= F1[j];
                F1MinIndex= j;
            }
        }        
        F1Media= F1Media / (double) numClases;        
        auxString+="F1-Score: \n";
        auxString+="\tF1-ScoreMin:(MS:)\t" + format.format(F1Min) + "\n";
        auxString+="\tF1-ScoreMinIndex:\t" + F1MinIndex + "\n";
        auxString+="\tF1-ScoreMacro-Media:\t" + format.format(F1Media) + "\n";
        auxString+="\tF1-ScoreClasses:\t";
        for (int j=0; j < numClases; j++){
            auxString+= format.format(F1[j]) + "\t";            
        }
        auxString+="\n";

        // Sp, FPR
        double[] TNR= new double[numClases];
        double[] FPR= new double[numClases];
        double TNRMedia=0, FPRMedia=0;
        for (int j=0; j < numClases; j++){
            TNR[j]= 0;
        }
        for (int j=0; j < numClases; j++){
            for (int k=0; k < numClases; k++){
                for (int l=0; l < numClases; l++){
                    if (j != k && j != l){
                        TNR[j]+= confusion[k][l];
                    }
                }
            }
//            if (TNR[j] != 0 && (confusion[numClases][numClases] - confusion[j][numClases]) == 0){
//                TNR[j]= -0;
//            }
            if ((confusion[numClases][numClases] - confusion[j][numClases]) == 0){
                TNR[j]= 0;
            }
            else{
                TNR[j]= TNR[j] / (confusion[numClases][numClases] - confusion[j][numClases]);                                
            }
        }
        for (int j=0; j < numClases; j++){
            TNRMedia+= TNR[j];
            FPR[j]= 1 - TNR[j];
        }
        TNRMedia= TNRMedia / (double) numClases;
        FPRMedia= 1 - TNRMedia;        
        auxString+="Specificity (Sp) or True Negative Rate (TNR): \n";
        auxString+="\tTNRMedia: \t" + format.format(TNRMedia) + "\n";
        auxString+="\tTNRClasses:\t";
        for (int j=0; j < numClases; j++){
            auxString+= format.format(TNR[j]) + "\t";
        }
        auxString+="\n";
        auxString+="False Positive Rate (FPR): \n";
        auxString+="\tFPRMedia:\t" + format.format(FPRMedia) + "\n";
        auxString+="\tFPRClasses:\t";
        for (int j=0; j < numClases; j++){
            auxString+= format.format(FPR[j]) + "\t";
        }
        auxString+="\n";
        
        // Kappa Cohen
        double kappa=0, P0=0, Pe=0;
        for (int j=0; j < numClases; j++){
            Pe+= confusion[j][numClases]*confusion[numClases][j];
        }
        Pe= Pe / (confusion[numClases][numClases]*confusion[numClases][numClases]);
        P0= CCR;
//        if ((P0-Pe != 0) && (double) 1 - Pe == 0){
//            kappa= -0;
//        }
        if ((double) 1 - Pe == 0){
            kappa= 0;
        }
        else{
            kappa= (P0 - Pe) / ((double) 1 - Pe);            
        }
        auxString+="Kappa Cohen:\t" + format.format(kappa) + "\n";

        //AUC
        double[] AUC= new double[numClases];
        double AUCMedia=0;
        for (int j=0; j < numClases; j++){
            AUC[j]= (1 + TPR[j] - FPR[j]) / 2.0;
            AUCMedia+= AUC[j];
        }
        AUCMedia= AUCMedia / (double) numClases;
        auxString+="Area Under Curve ROC (AUC):\n";
        auxString+="\tAUCMedia:\t" + format.format(AUCMedia) + "\n";
        auxString+="\tAUCClasses:\t";
        for (int j=0; j < numClases; j++){
            auxString+= format.format(AUC[j]) + "\t";
        }
        auxString+="\n";

        writeResFile(fileMetrics, auxString);
        System.out.println(auxString);
        
        
        //métricas regresion
        if (regresion == 1){
            auxString="-- Regression metrics -- \n";
            // MSE, RMSE, MAE
            double MSE=0, RMSE=0, MAE=0;
            for (int j=0; j < numEjemplos; j++){
                MSE+= (values[j][0] - values[j][1]) * (values[j][0] - values[j][1]);
                MAE+= Math.abs(values[j][0] - values[j][1]);
            }
            MSE= MSE / (double) numEjemplos;
            RMSE= Math.sqrt(MSE);
            MAE= MAE / (double) numEjemplos;
            auxString+="MSE:\t" + format.format(MSE) + "\n";
            auxString+="RMSE:\t" + format.format(RMSE) + "\n";
            auxString+="R-MAE:\t" + format.format(MAE) + "\n";

            writeResFile(fileMetrics, auxString);
            System.out.println(auxString);       
        }// if (regresion == 1){
        
        //métricas clasificacion ordinal
        auxString="-- Ordinal metrics -- \n";
        // MAEOrd, AMAE, MMAE
        double[] MAEOrd= new double[numClases];
        double MAETotal=0, AMAE=0, mMAE= confusion[numClases][numClases], MMAE=0;
        int MMAEIndex= 0, mMAEIndex=0;
        for (int j=0; j < numClases; j++){
            MAEOrd[j]=0;
        }
        for (int j= 0; j < numClases; j++){
            for (int k= 0; k < numClases; k++){
                MAEOrd[j]+= Math.abs(j-k)*confusion[j][k];
            }
//            if (MAEOrd[j] != 0 && confusion[j][numClases] == 0){
//                MAEOrd[j]= -0;
//            }
            if (confusion[j][numClases] == 0){
                MAEOrd[j]= 0;
            }
            else{
                MAEOrd[j]= MAEOrd[j] / confusion[j][numClases];
            }
            MAETotal+= confusion[j][numClases] * MAEOrd[j];
            AMAE+= MAEOrd[j];
            if (MMAE < MAEOrd[j]){
                MMAE= MAEOrd[j];
                MMAEIndex= j;
            }
            if (mMAE > MAEOrd[j]){
                mMAE= MAEOrd[j];
                mMAEIndex= j;
            }
        }
        MAETotal= MAETotal / confusion[numClases][numClases];
        AMAE= AMAE / numClases;
        auxString+="O-MAE: \n";
        auxString+="\tO-MAEAllClasses:\t" + format.format(MAETotal) + "\n";
// OJO - nuevos cálculos
//        auxString+="\tO-MAEAllClasses:\t" + format.format(1-(MAETotal/(double)(numClases-1))) + "\n";
        auxString+="\tO-AMAE:\t"+ format.format(AMAE) + "\n";
        auxString+="\tO-MMAE:\t"+ format.format(MMAE) + "\n";
        auxString+="\tO-MMAEIndex:\t" + MMAEIndex + "\n";
        auxString+="\tO-mMAE:\t"+ format.format(mMAE) + "\n";
        auxString+="\tO-mMAEIndex:\t"+ mMAEIndex + "\n";
        auxString+="\tO-MAEClasses:\t";
        for (int j=0; j < numClases; j++){
            auxString+= format.format(MAEOrd[j]) + "\t";            
        }
        auxString+="\n";
        
        //Rs
        double Rs=0;
        double ORealMedia=0, OPredMedia=0;
        double numerador= 0, denominador1=0, denominador2=0;
        int clasReal, clasPred;
        int indNoCero=0; // para que los índices de las clases no comiencen con 0 y se pueda hacer los cálculos
        
        // calculos con datos de regresión (numEjemplos)
        if (regresion == 1){
            ORealMedia= OPredMedia= 0;
            numerador= denominador1= denominador2= 0;
            for (int j= 0; j < numEjemplos; j++){
                clasReal=  getClass(values[j][0], defClases);        
                clasPred=  getClass(values[j][1], defClases);
                ORealMedia+= (clasReal+indNoCero);
                OPredMedia+= (clasPred+indNoCero);
            }
            ORealMedia= ORealMedia / (double) numEjemplos;
            OPredMedia= OPredMedia / (double) numEjemplos;

            for (int j= 0; j < numEjemplos; j++){
                clasReal=  getClass(values[j][0], defClases);        
                clasPred=  getClass(values[j][1], defClases);
                numerador+= ((clasReal+indNoCero) - ORealMedia) * ((clasPred+indNoCero) - OPredMedia);
                denominador1+= ((clasReal+indNoCero) - ORealMedia) * ((clasReal+indNoCero) - ORealMedia);
                denominador2+= ((clasPred+indNoCero) - OPredMedia) * ((clasPred+indNoCero) - OPredMedia);
            }
            denominador1= Math.sqrt(denominador1);
            denominador2= Math.sqrt(denominador2);        
//            if (numerador != 0 && denominador1 * denominador2 == 0){
//                Rs= -0;
//            }
            if (denominador1 * denominador2 == 0){
                Rs= 0;
            }
            else{
                Rs= numerador / (denominador1 * denominador2);            
            }

            // para la comprobación
            System.out.println("ORealMedia +   + OPredMedia +  + numerador +  + denominador1 +  + denominador2 +  + Rs");
            System.out.println(format.format(ORealMedia) + "\t" + format.format(OPredMedia) + "\t" + 
                    format.format(numerador) + "\t" + format.format(denominador1) + "\t" + 
                    format.format(denominador2) + "\t" + format.format(Rs));
            // FIN - para la comprobación
        }// if (regresion == 1){

        // calculos con matriz de confusion (numClases)
        ORealMedia= OPredMedia= 0;
        numerador= denominador1= denominador2= 0;
        Rs= 0;
        for (int j= 0; j < numClases; j++){
            ORealMedia+= (j+indNoCero)*confusion[j][numClases];
            OPredMedia+= (j+indNoCero)*confusion[numClases][j];
        }
        ORealMedia= ORealMedia / confusion[numClases][numClases];        
        OPredMedia= OPredMedia / confusion[numClases][numClases];        

        for (int j= 0; j < numClases; j++){
            for (int k=0; k < numClases; k++){
                numerador+= (confusion[j][k])*((j+indNoCero) - ORealMedia)*((k+indNoCero) - OPredMedia);
            }
            denominador1+= confusion[j][numClases] * (((j+indNoCero) - ORealMedia)*((j+indNoCero) - ORealMedia));
            denominador2+= confusion[numClases][j] * (((j+indNoCero) - OPredMedia)*((j+indNoCero) - OPredMedia));
        }            
        denominador1= Math.sqrt(denominador1);
        denominador2= Math.sqrt(denominador2);        
//        if (numerador != 0 && denominador1 * denominador2 == 0){
//            Rs= -0;
//        }
        if (denominador1 * denominador2 == 0){
            Rs= 0;
        }
        else{
            Rs= numerador / (denominador1 * denominador2);            
        }
        
        // para la comprobación
        System.out.println("ORealMedia +   + OPredMedia +  + numerador +  + denominador1 +  + denominador2 +  + Rs");
        System.out.println(format.format(ORealMedia) + "\t" + format.format(OPredMedia) + "\t" + 
                format.format(numerador) + "\t" + format.format(denominador1) + "\t" + 
                format.format(denominador2) + "\t" + format.format(Rs));
        // FIN - para la comprobación


        auxString+="Rs:\t" + format.format(Rs) + "\n";
        auxString+="\tnumerador:\t" + format.format(numerador) + "\n";
        auxString+="\tdenominador:\t" + format.format(denominador1*denominador2) + "\n";



        //Taub
        double Taub=0;
        int clasRealJ, clasRealK, clasPredJ, clasPredK;
        int cRealJK, cPredJK;
        
        // calculos con datos de regresión (numEjemplos)
        if (regresion == 1){
            numerador= denominador1= denominador2= 0;
            for (int j= 0; j < numEjemplos; j++){
                clasRealJ=  getClass(values[j][0], defClases);        
                clasPredJ=  getClass(values[j][1], defClases);
                for (int k= 0; k < numEjemplos; k++){
                    clasRealK=  getClass(values[k][0], defClases);        
                    clasPredK=  getClass(values[k][1], defClases);

                    if (clasPredJ > clasPredK){
                        cPredJK= 1;
                    }
                    else if (clasPredJ < clasPredK){
                        cPredJK= -1;
                    }                
                    else{
                        cPredJK= 0;
                    }
                    if (clasRealJ > clasRealK){
                        cRealJK= 1;
                    }
                    else if (clasRealJ < clasRealK){
                        cRealJK= -1;
                    }                
                    else{
                        cRealJK= 0;
                    }

                    numerador= numerador + (cPredJK * cRealJK);
                    denominador1= denominador1 + (cPredJK*cPredJK);
                    denominador2= denominador2 + (cRealJK*cRealJK);
                }
            }
//            if (numerador != 0 && denominador1 * denominador2 == 0){
//                Taub= -0;
//            }
            if (denominador1 * denominador2 == 0){
                Taub= 0;
            }
            else{            
                Taub= numerador / (Math.sqrt(denominador1*denominador2));
            }

            // para la comprobación
            System.out.println("numerador +  + denominador1 +  + denominador2 +  + Taub");
            System.out.println(format.format(numerador) + "\t" + format.format(denominador1) + "\t" + 
                    format.format(denominador2) + "\t" + format.format(Taub));
            // FIN - para la comprobación    
        }// if (regresion == 1){

        // calculos con matriz de confusion (numClases)
        numerador= denominador1= denominador2= 0;
        Taub= 0;
        for (int j1= 0; j1 < numClases; j1++){
            for (int j2= 0; j2 < numClases; j2++){
                double parcial=0;
                for (int k1= 0; k1 < numClases; k1++){
                    for (int k2= 0; k2 < numClases; k2++){ // no se utiliza la fila y columna del que estoy considerando
                        if (k1 < j1){           // estoy en la parte superior al que estoy considerando (j)
                            if (k2 < j2){       // estoy en la parte izda al que estoy considerando (j)
                                parcial= parcial + confusion[k1][k2];                                
                            }
                            else if (k2 > j2){  // estoy en la parte derecha
                                parcial= parcial - confusion[k1][k2];
                            }
                        }
                        else if (k1 > j1){      // estoy en la parte inferior al que estoy considerando (j)
                            if (k2 < j2){       // estoy en la parte izda al que estoy considerando (j)
                                parcial= parcial - confusion[k1][k2];                                
                            }
                            else if (k2 > j2){  // estoy en la parte derecha
                                parcial= parcial + confusion[k1][k2];
                            }
                        }
                    }//for (int k2= 0; k2 < numClases; k2++){ // no se utiliza la fila y columna del que estoy considerando
                }//for (int k1= 0; k1 < numClases; k1++){
                numerador+= confusion[j1][j2] * parcial;
            }//for (int j2= 0; j2 < numClases; j2++){
            denominador1+= confusion[numClases][j1] * (confusion[numClases][numClases] - confusion[numClases][j1]);
            denominador2+= confusion[j1][numClases] * (confusion[numClases][numClases] - confusion[j1][numClases]);
        }//for (int j1= 0; j1 < numClases; j1++){

//        if (numerador != 0 && denominador1 * denominador2 == 0){
//            Taub= -0;
//        }
        if (denominador1 * denominador2 == 0){
            Taub= 0;
        }
        else{
            Taub= numerador / Math.sqrt(denominador1 * denominador2);            
        }
        
        // para la comprobación
        System.out.println("numerador +  + denominador1 +  + denominador2 +  + Taub");
        System.out.println(format.format(numerador) + "\t" + format.format(denominador1) + "\t" + 
                format.format(denominador2) + "\t" + format.format(Taub));
        // FIN - para la comprobación


        auxString+="Taub:\t" + format.format(Taub) + "\n";
        auxString+="\tnumerador:\t" + format.format(numerador) + "\n";
        auxString+="\tdenominador:\t" + format.format(Math.sqrt(denominador1*denominador2)) + "\n";


        //OC
        double[][] nrc_absnrc= new double[numClases+1][numClases+1];
        double[][] W= new double[numClases][numClases];
        double[][] w= new double[numClases][numClases];
        double OC= -1;
        double sumaParcial=0, sumaTotal=0;
        beta= beta / (confusion[numClases][numClases]*(numClases-1));
        
        for (int j= 0; j < numClases; j++){
            sumaParcial=0;
            for (int k= 0; k < numClases; k++){
                nrc_absnrc[j][k]= confusion[j][k] * Math.abs(j-k);
                sumaParcial+= nrc_absnrc[j][k];
            }
            nrc_absnrc[j][numClases]= sumaParcial;
            sumaTotal+= sumaParcial;
        }
        nrc_absnrc[numClases][numClases]= sumaTotal;
        
        for (int j= 0; j < numClases; j++){
            for (int k= 0; k < numClases; k++){
                w[j][k]= - ((confusion[j][k]) / 
                           (confusion[numClases][numClases] + nrc_absnrc[numClases][numClases]) )
                         + beta*nrc_absnrc[j][k];                        
            }
        }

        W[0][0]= 1 + w[0][0];
        for (int j=1; j < numClases; j++){
            W[j][0]= w[j][0] + getMinWrc(W,j,0);
        }
        for (int j=1; j < numClases; j++){
            W[0][j]= w[0][j] + getMinWrc(W,0,j);
        }
        for (int j= 1; j < numClases; j++){
            for (int k= 1; k < numClases; k++){
                W[j][k]= w[j][k] + getMinWrc(W,j,k);
            }
        }

        // para la comprobación
        System.out.println( " - nrc_absnrc - ");
        for (int j=0; j < numClases+1; j++){
            for (int k= 0; k < numClases+1; k++){
                System.out.print(nrc_absnrc[j][k] + " ");
            }
            System.out.println();
        }        
        System.out.println( " - w - ");
        for (int j=0; j < numClases; j++){
            for (int k= 0; k < numClases; k++){
                System.out.print(w[j][k] + " ");
            }
            System.out.println();
        }        
        System.out.println( " - W - ");
        for (int j=0; j < numClases; j++){
            for (int k= 0; k < numClases; k++){
                System.out.print(W[j][k] + " ");
            }
            System.out.println();
        }        
        // FIN - para la comprobación

        OC= W[numClases-1][numClases-1];

        auxString+="OC_beta:\t" + format.format(OC) + "\n";
        auxString+="\tbeta:\t" + format.format(beta) + "\n";


        // imprimir la "nueva métrica" (1-CCR) * OMAE
//        double myMetric= 1.0 - ((1.0 - CCR) * (MAETotal / (numClases-1)));
// OJO - nuevos cálculos
        double myMetric= (CCR + (1-(MAETotal / (numClases-1)))) / 2.0;
        auxString+="myMetric:\t" + format.format(myMetric) + "\n";
                               
        writeResFile(fileMetrics, auxString);
        System.out.println(auxString);
        
        // imprimir los datos y su clasificación
        if (regresion == 1){
            auxString= "@data (realValue\tpredictedValue\trealClass\tpredictedClass\n";
            writeResFile(fileMetrics, auxString);
            System.out.println(auxString);

            auxString="";
            for (int j=0; j < numEjemplos; j++){
                clasReal=  getClass(values[j][0], defClases);        
                clasPred=  getClass(values[j][1], defClases);
                auxString+= format.format(values[j][0]) + "\t" + 
                        format.format(values[j][1]) + "\t" + clasReal + "\t" + 
                        clasPred + "\n";

            }
        }//if (regresion == 1){
        else{
            auxString= "@data (realClass\tpredictedClass\n";
            writeResFile(fileMetrics, auxString);
            System.out.println(auxString);

            auxString="";
            for (int j=0; j < numEjemplos; j++){
                auxString+= (int) clases[j][0] + "\t" + (int) clases[j][1] + "\n";

            }            
        }// else ... if (regresion == 1){
            
        writeResFile(fileMetrics, auxString);
        System.out.println(auxString);

        return resultString;
    }

    /**
    * Devuelve una variable con la sintaxis de la aplicación
    * @param applicationName -> nombre de la aplicación
    * @return sintaxis de la aplicación junto con ejemplos de llamadas a la misma
    */
    private static String writeSyntax(String applicationName){

        String sintax;

        sintax= "sintax: \n";
        sintax= sintax + applicationName + " resultAlgorithm resultMetrics [beta(OCI)]\n";

        sintax= sintax + "\n   Example: " + applicationName + " result.txt metrics.txt [0.25]\n";

        return sintax;
    }
    
    /**
    * Chequea la sintaxis y los ficheros de la aplicación 
    * @param argc -> número de argumentos
    * @param argv -> matriz de argumentos
    * @param sintax -> la sintax de la aplicación
    * @return String con [0]=resultAlgorithm, [1]=resultMetrics
     *          Si ERROR -> [0]="", [1]=MENSAJE ERROR;
    */
    private static String[] checkSyntaxAndFiles(int argc, String argv[], String sintax){

        String[] FileNamesArguments= new String[3];

        FileNamesArguments[0]= "";
        FileNamesArguments[1]= "";
        FileNamesArguments[2]= "";

        if (argc < 1 || argc > 3){
            FileNamesArguments[1]= "SINTAX ERROR - argument number: ";
            FileNamesArguments[1]= FileNamesArguments[1] + argc;
            FileNamesArguments[1]= FileNamesArguments[1] + "\n" + sintax;
            return FileNamesArguments;
        }
        
        if (argc == 2){
            FileNamesArguments[2]= "0.25";
        }
        else if (argv[2].matches("-?\\d+(\\.\\d+)?") == false){  //match a number with optional '-' and decimal.
            FileNamesArguments[1]= "SINTAX ERROR - argument 'beta' is not a number: ";
            FileNamesArguments[1]= FileNamesArguments[1] + argv[2];
            FileNamesArguments[1]= FileNamesArguments[1] + "\n" + sintax;
            return FileNamesArguments;                        
        }
        else{
            FileNamesArguments[2]= argv[2];        
        }
       
        //comprobación de existencia de ficheros
        File f= new File(argv[0]);
        if (!f.exists()){
            FileNamesArguments[1]= "ERROR - File Not Found: ";
            FileNamesArguments[1]= FileNamesArguments[1] + argv[0];
            FileNamesArguments[1]= FileNamesArguments[1] + "\n\n" + sintax;
            return FileNamesArguments;
        }
       
        // comprobación de la posibilidad de crear ficheros
        f= new File(argv[1]);
        try{
            f.createNewFile();
        } catch (IOException e) {
            FileNamesArguments[1]= "ERROR - Create File: ";
            FileNamesArguments[1]= FileNamesArguments[1] + argv[1];
            FileNamesArguments[1]= FileNamesArguments[1] + "\n\n" + sintax;
            return FileNamesArguments;
        }      
                
        FileNamesArguments[0]= argv[0];
        FileNamesArguments[1]= argv[1];
                
        return FileNamesArguments;
    }

    
    
    /**
     * 
     * @param strFile
     * @return matriz con los valores reales y los predichos. Primero van el 
     * número de clases y los margenes de las mismas y después los valores
     */
    public static double[][] readFileData(String strFile){
        
      int numClases=0;
      int numEjemplos=0;
      int regresion= 1;
      double[][] values, clases;
      boolean lineData= true;
      values= clases= new double[0][0];
      
      BufferedReader reader;
      String line = null;
      String[] tokens;
      // recorrer el fichero para obtener el número de ejemplos (al mismo tiempo se obtienen las clases)
      try{
        reader = new BufferedReader(new FileReader(strFile));
        line= reader.readLine(); 
//        while (line != null && (line.contains("#") == true || line.equalsIgnoreCase("@data") == true)){    // saltar los comentarios
        while (line != null && (line.contains("#") == true || line.contains("@data") == true)){    // saltar los comentarios
          line= reader.readLine(); 
        }
        // coger los límites de las clases
        numClases= Integer.parseInt(line);
        line= reader.readLine(); 
        regresion= Integer.parseInt(line);
        clases= new double[numClases][2];
        if (regresion == 1){
            for (int i=0; i < numClases - 1; i++){
                clases[i][0]= clases[i][1]=0;
                line= reader.readLine();
//                if (line == null || line.equalsIgnoreCase("@data") == true){
                if (line == null || line.contains("@data") == true){
                    lineData= false;
                }
                else{
                    clases[i][0]= Double.parseDouble(line);
                }
            }
            line= reader.readLine();
//            if (line == null || line.equalsIgnoreCase("@data") == true){
            if (line == null || line.contains("@data") == true){
                lineData= false;
            }
            else{
                tokens= line.split(" ");
                if (tokens.length >= 2){
                    clases[numClases-1][0]= Double.parseDouble(tokens[0]);
                    clases[numClases-1][1]= Double.parseDouble(tokens[1]);
                }
                else{
                    tokens= line.split("\t");
                    if (tokens.length >= 2){
                        clases[numClases-1][0]= Double.parseDouble(tokens[0]);
                        clases[numClases-1][1]= Double.parseDouble(tokens[1]);
                    }
                    else{
                        lineData= false;
                    }
                }
            }
        }// if (regresion == 1){

        // comienzan los datos, contar cuántos patrones/ejemplos hay
        line= reader.readLine();
//        if (line == null || line.equalsIgnoreCase("@data") == false){
        if (line == null || line.contains("@data") == false){
            lineData= false;
        }
        while ((line = reader.readLine()) != null){
          if (line.equalsIgnoreCase("") == false){
              numEjemplos++;
          }
        }
        
      } catch (IOException e) {
        System.out.println("File I/O error! - procesando parámetros");
      }      
      
      if (lineData == false){
        return new double[0][0];
      }

      //reservar espacio para los ejemplos y los límites de las clases
      if (regresion == 1){
        values= new double[numClases+numEjemplos+1][2];
      }
      else{
        values= new double[numEjemplos+1][2];          
      }
      int i=0;
      values[i][0]= numClases;
      values[i][1]= regresion;
      i++;
      if (regresion == 1){
        for (int j=0; j < numClases - 1; j++){
            values[j+1][0]= clases[j][0];
            values[j+1][1]= clases[j+1][0];
        }
        i+= numClases - 1;
        values[i][0]= clases[numClases-1][0];
        values[i][1]= clases[numClases-1][1];
        i++;
      }// if (regresion == 1){        
      // volver a recorrer el fichero para obtener los ejemplos
      try{
        reader = new BufferedReader(new FileReader(strFile));
        line= reader.readLine(); 
        while (line.equalsIgnoreCase("@data") == false && line != null){
          line= reader.readLine(); 
        }
        line= reader.readLine();
        while (line != null){
          if (line.equalsIgnoreCase("") == false){
            tokens= line.split(" ");          
            if (tokens.length < 2){
                tokens= line.split("\t");
            }
            values[i][0]= Double.parseDouble(tokens[0]);
            values[i][1]= Double.parseDouble(tokens[1]);
            i++;
          }
          line= reader.readLine();
        }
        
      } catch (IOException e) {
       System.out.println("File I/O error! - procesando parámetros");
      }      
      
      return values;
    }

    /**
     * Init  ResFileName the str argument
     * @return -1 if error; 1 otherwise
     */
    public static int initResFile(String fileName){
        try{
            File file = new File(fileName);
            if (file.exists()) {
              file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 1;
    }
    
    /**
     * Write in ResFileName the str argument
     * @param str string for write to ResFileName
     * @return -1 if error; 1 otherwise
     */
    public static int writeResFile(String fileName, String str){
        try {
            File file = new File(fileName);
            FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(str);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 1;
    }

    // ................................
    
    /**
     * devuelve la clase a la que debe pertenecer el ejemplo que se le pasa como argumento
     * @param value valor del ejemplo para el que se desea ver la clase
     * @param defClases definición de límites de las clases
     * @return orden de la clase a la que pertenece el ejemplo (-1 si hay error)
     */
    public static int getClass(double value, double[][] defClases){

        int numClases= defClases.length;
        
        if ((value >= defClases[0][0]) && (value <= defClases[numClases-1][1])){
            for (int j=0; j < numClases; j++){
                if (value >= defClases[j][0] && value < defClases[j][1]){
                    return j;
                }
            }
            if (value == defClases[numClases-1][1]){
                return numClases-1;
            }
        }
        return -1;        
    }
    
    /**
     * devuelve el mínimo de las casillas r-1,c ; r,c-1; r-1,c-1 de la matriz W
     * @param W matriz con los valores de Wrc
     * @param r índice de la fila del elemento que se está considerando
     * @param c índice de la columna del elemento que se está considerando
     * @return el mínimo de las casillas r-1,c ; r,c-1; r-1,c-1 de la matriz W
     */
    public static double getMinWrc(double[][] W,int r,int c){

        double min= 1.1;
        
        if (r > 0 && c > 0 && (W[r-1][c-1] < min)){
            min= W[r-1][c-1];
        }
        if (r > 0 && (W[r-1][c] < min)){
            min= W[r-1][c];
        }
        if (c > 0 && (W[r][c-1] < min)){
            min= W[r][c-1];
        }
        
        return min;
    
    }
        
}
