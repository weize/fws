/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.utility;

import cc.mallet.optimize.Optimizable;
import static edu.umass.ciir.fws.eval.PrfNewEvaluator.safelyNormalize;
import edu.umass.ciir.fws.utility.Utility;
import java.util.Arrays;

/**
 *
 * @author wkong
 */
public class GmPRFMaximizable implements Optimizable.ByGradientValue {

    double alphaSquare, betaSquare;
    double c = 0; // regulization, PRF - c/2 * w^2 - c/2 * u^2

    private double[] tParams; // term params: w_k
    private double[] pParams; // pair params: u_k
    Instance[] instances;
    int nTf, nPf; // number of term features, and number of pair features, including bias
    int nf; // nTf + nPf
    int nInstances;

    int paramStamp;
    int paramCacheStamp;
    int valueStamp;
    int valueCacheStamp;
    int gradientStamp;
    int gradientCacheStamp;
    double[] cachedTGradient;
    double[] cachedPGradient;
    double cachedValue;

    // sigmoid
    double[][] pT; // pt[instanceIndex][termIndex, or i] = sigmoid(w^T f(term_i))
    double[][] pP; // pP[instanceIndex][pairIndex, or i,j] = sigmoid(u^T f(pair_i,j))
    //expected counts
    double[] Ts; // term_system
    double[] Tc; // term_correct
    double[] Ps; // pair_system
    double[] Pc; // pair_correct
    double[] Pg; // pair_groundtruth
    //
    // PRF = 2* (alpha^2 + beta^2 + 1)* Tc * Pc / D
    // D = 2*alpha^2*Ts*Pc + 2*beta^2*Tg*Pc + Tc*Ps + Tc*Pg
    double[] D;

    public GmPRFMaximizable(Instance[] instances, double alpha, double beta, double c) {
        this.alphaSquare = alpha * alpha;
        this.betaSquare = beta * beta;
        this.c = c;
        this.instances = instances;
        nInstances = instances.length;
        nTf = instances[0].tFeatures[0].length;
        nPf = instances[0].pFeatures[0].length;
        nf = nTf + nPf;
        tParams = new double[nTf];
        pParams = new double[nPf];

        Utility.info(String.format("alpha=%f, beta=%f, c=%f", alpha, beta, c));
        paramCacheStamp = -1;
        paramStamp = 0;
        valueCacheStamp = -1;
        valueStamp = 0;
        gradientCacheStamp = -1;
        gradientStamp = 0;

        cachedTGradient = new double[nTf];
        cachedPGradient = new double[nPf];
        cachedValue = Double.NEGATIVE_INFINITY;

        pT = new double[nInstances][];
        pP = new double[nInstances][];
        Ts = new double[nInstances];
        Tc = new double[nInstances];
        Ps = new double[nInstances];
        Pc = new double[nInstances];
        Pg = new double[nInstances];
        D = new double[nInstances];
        for (int i = 0; i < nInstances; i++) {
            Instance ins = instances[i];
            pT[i] = new double[ins.nT];
            pP[i] = new double[ins.nP];
        }
    }

    @Override
    public void getParameters(double[] buffer) {
        int i = 0;
        for (int k = 0; k < nTf; k++) {
            buffer[i++] = tParams[k];
        }

        for (int k = 0; k < nPf; k++) {
            buffer[i++] = pParams[k];
        }
    }

    @Override
    public double getParameter(int index) {
        return index < nTf ? tParams[index] : pParams[index - nTf];
    }

    @Override
    public void setParameters(double[] params) {
        int i = 0;
        for (int k = 0; k < nTf; k++) {
            tParams[k] = params[i++];
        }

        for (int k = 0; k < nPf; k++) {
            pParams[k] = params[i++];
        }

        updateParamStamp();
    }

    @Override
    public void setParameter(int index, double value) {
        if (index < nTf) {
            tParams[index] = value;
        } else {
            pParams[index - nTf] = value;
        }
        updateParamStamp();
    }

    private void updateParamStamp() {
        paramStamp++;
        valueStamp++;
        gradientStamp++;
    }

    @Override
    public int getNumParameters() {
        return this.nf;
    }

    @Override
    public void getValueGradient(double[] buffer) {
        checkOutParamsUpdates();
        if (gradientCacheStamp != gradientStamp) {
            updateGradient();
            gradientCacheStamp = gradientStamp;
        }
        System.arraycopy(cachedTGradient, 0, buffer, 0, nTf);
        System.arraycopy(cachedPGradient, 0, buffer, nTf, nPf);
    }

    @Override
    public double getValue() {
        checkOutParamsUpdates();
        if (valueCacheStamp != valueStamp) {
            cachedValue = avgPRF();
            valueCacheStamp = valueStamp;
        }
        return cachedValue;
    }

    public double getPRFValue() {
        getValue(); // update value
        return cachedValue + getRegulizationCost();
    }

    public void checkOutParamsUpdates() {
        if (paramCacheStamp != paramStamp) {
            updateTermPairProb();
            updateExpectedCounts();
            paramCacheStamp = paramStamp;
        }
    }

    private void updateTermPairProb() {
        for (int i = 0; i < nInstances; i++) {
            Instance ins = instances[i];
            for (int j = 0; j < ins.nT; j++) {
                pT[i][j] = sigmoid(ins.tFeatures[j], tParams);
            }

            for (int j = 0; j < ins.nP; j++) {
                pP[i][j] = sigmoid(ins.pFeatures[j], pParams);
            }
        }
    }

    private void updateExpectedCounts() {
        Arrays.fill(Ts, 0);
        Arrays.fill(Tc, 0);
        Arrays.fill(Ps, 0);
        Arrays.fill(Pc, 0);
        Arrays.fill(Pg, 0);

        for (int i = 0; i < nInstances; i++) {
            Instance ins = instances[i];
            double[] termProb = pT[i];
            double[] pairProb = pP[i];

            for (int j = 0; j < ins.nT; j++) {
                Ts[i] += termProb[j];
                if (ins.Ys[j]) {
                    Tc[i] += termProb[j];
                }
            }

            // postive term id
            for (int pTid1 = 0; pTid1 < ins.nPosTerms; pTid1++) {
                for (int pTid2 = pTid1 + 1; pTid2 < ins.nPosTerms; pTid2++) {
                    int pid = ins.getPidWithSmallBigPosIds(pTid1, pTid2);
                    int tid1 = ins.posIdToTid[pTid1];
                    int tid2 = ins.posIdToTid[pTid2];

                    double expection = termProb[tid1] * termProb[tid2] * pairProb[pid];
                    Ps[i] += expection;
                    if (ins.Zs[pid]) {
                        Pc[i] += expection;
                        Pg[i] += termProb[tid1] * termProb[tid2];
                    }
                }
            }

            // g = 2*alpha^2*Ts*Pc + 2*beta^2*Tg*Pc + Tc*Ps + Tc*Pg
            D[i] = 2 * alphaSquare * Ts[i] * Pc[i] + 2 * betaSquare * ins.nPosTerms * Pc[i]
                    + Tc[i] * Ps[i] + Tc[i] * Pg[i];
        }
    }

    private double avgPRF() {
        double prf = 0;
        for (int i = 0; i < nInstances; i++) {
            prf += safelyNormalize(2 * (alphaSquare + betaSquare + 1) * Tc[i] * Pc[i], D[i]);
        }
        prf /= nInstances;

        prf -= getRegulizationCost();
        return prf;
    }

    public double getRegulizationCost() {
        //PRF - c/2 * w^2 - c/2 * u^2
        double regulization = 0;
        for (double a : tParams) {
            regulization += a * a;
        }

        for (double a : pParams) {
            regulization += a * a;
        }
        return c * regulization * 0.5;
    }

    /**
     * 1/(1 + exp{- features * weights})
     *
     * @param features
     * @param weights
     * @return
     */
    private double sigmoid(double[] features, double[] weights) {
        double x = 0;
        for (int i = 0; i < weights.length; i++) {
            x -= features[i] * weights[i];
        }
        return 1 / (1 + Math.exp(x));
    }

    private void updateGradient() {
        Arrays.fill(cachedTGradient, 0);

        // term feature weight gradient
        for (int k = 0; k < nTf; k++) {
            for (int i = 0; i < nInstances; i++) {
                if (D[i] < Utility.epsilon) {
                    Utility.info("D[" + i + "]=" + 0);
                    // when D[i] == 0 => Pc = 0 & Tc = 0 => d PRF / d wk = 0  
                    continue;
                }

                Instance ins = instances[i];

                double[] termProb = pT[i];
                double[] pairProb = pP[i];

                double gTsW = 0; // d Ts / d w_k += sigmod'(t_i) * f_i,k  (for all term t_i)
                double gTcW = 0; // d Tc / d w_k += y_i * sigmod'(t_i) * f_i,k (for all term)

                for (int j = 0; j < ins.nT; j++) {
                    double gradient = sigmoidDerivative(termProb[j]) * ins.tFeatures[j][k];
                    gTsW += gradient;
                    if (ins.Ys[j]) {
                        gTcW += gradient;
                    }
                }

                double gPsW = 0; // d Ps / d w_k = sigmod(p_i,j) * (sigmod'(t_i) * sigmod(t_j) * f_i,k + sigmod'(t_j) * sigmod(t_i) * f_j,k) (for al pairs of positive terms p_i,j)
                double gPgW = 0; // d Pg / d w_k = z_i,j * (sigmod'(t_i) * sigmod(t_j) * f_i,k + sigmod'(t_j) * sigmod(t_i) * f_j,k)
                double gPcW = 0; // d Pc / d w_k = z_i,j * sigmod(p_i,j) * (sigmod'(t_i) * sigmod(t_j) * f_i,k + sigmod'(t_j) * sigmod(t_i) * f_j,k)

                // postive term id
                for (int pTid1 = 0; pTid1 < ins.nPosTerms; pTid1++) {
                    for (int pTid2 = pTid1 + 1; pTid2 < ins.nPosTerms; pTid2++) {
                        int pid = ins.getPidWithSmallBigPosIds(pTid1, pTid2);
                        int tid1 = ins.posIdToTid[pTid1];
                        int tid2 = ins.posIdToTid[pTid2];

                        double part = sigmoidDerivative(termProb[tid1]) * termProb[tid2] * ins.tFeatures[tid1][k]
                                + sigmoidDerivative(termProb[tid2]) * termProb[tid1] * ins.tFeatures[tid2][k];

                        gPsW += pP[i][pid] * part;
                        if (ins.Zs[pid]) {
                            gPcW += pP[i][pid] * part;
                            gPgW += part;
                        }
                    }
                }

                double gDW = 2 * alphaSquare * Pc[i] * gTsW + (Ps[i] + Pg[i]) * gTcW + Tc[i] * (gPsW + gPgW)
                        + 2 * (alphaSquare * Ts[i] + betaSquare * ins.nPosTerms) * gPcW;

                double gPrfW = (Pc[i] * gTcW + Tc[i] * gPcW) / D[i] - gDW * Tc[i] * Pc[i] / (D[i] * D[i]);
                gPrfW *= 2 * (alphaSquare + betaSquare + 1);// not necessary 
                cachedTGradient[k] += gPrfW;
            }
            cachedTGradient[k] /= nInstances; // average
            cachedTGradient[k] -= c * tParams[k]; // regulization
        }

        // update pair gradient
        Arrays.fill(cachedPGradient, 0);
        for (int k = 0; k < nPf; k++) {
            for (int i = 0; i < nInstances; i++) {
                if (D[i] < Utility.epsilon) {
                    Utility.info("D[" + i + "]=" + 0);
                    // when D[i] == 0 => Pc = 0 & Tc = 0 => d PRF / d wk = 0  
                    continue;
                }

                Instance ins = instances[i];

                double[] termProb = pT[i];
                double[] pairProb = pP[i];

                double gPcU = 0; // d Pc / d u_k =  Z_i,j * sigmod(t_i)sigmod(t_j)sigmod'(p_i,j)g_i,j,k 
                double gPsU = 0; // sigmod(t_i)sigmod(t_j)sigmod'(p_i,j)g_i,j,k

                // postive term id
                for (int pTid1 = 0; pTid1 < ins.nPosTerms; pTid1++) {
                    for (int pTid2 = pTid1 + 1; pTid2 < ins.nPosTerms; pTid2++) {
                        int pid = ins.getPidWithSmallBigPosIds(pTid1, pTid2);
                        int tid1 = ins.posIdToTid[pTid1];
                        int tid2 = ins.posIdToTid[pTid2];

                        double part = termProb[tid1] * termProb[tid2] * sigmoidDerivative(pairProb[pid]) * ins.pFeatures[pid][k];
                        gPsU += part;
                        if (ins.Zs[pid]) {
                            gPcU += part;
                        }
                    }
                }

                double gDU = 2 * (alphaSquare * Ts[i] + betaSquare * ins.nPosTerms) * gPcU + Tc[i] * gPsU;
                double gPrfU = 2 * (alphaSquare + betaSquare + 1) * Tc[i] * (gPcU / D[i] - gDU * Pc[i] / (D[i] * D[i]));
                cachedPGradient[k] += gPrfU;
            }
            cachedPGradient[k] /= nInstances;
            cachedPGradient[k] -= c * pParams[k]; // regulization
        }
    }

    public double sigmoidDerivative(double sigmoid) {
        return sigmoid * (1 - sigmoid);
    }

}
