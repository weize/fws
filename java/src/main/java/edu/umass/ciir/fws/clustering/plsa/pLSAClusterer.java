/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.plsa;

/**
 *
 * @author wkong
 */
//The code is taken from:
//http://code.google.com/p/mltool4j/source/browse/trunk/src/edu/thu/mltool4j/topicmodel/plsa
//I noticed some difference with original Hofmann concept in computation of P(z). It is 
//always even and actually not involved, that makes this algorithm non-negative matrix 
//factoring and not PLSA.
//Found and tested by Andrew Polar. 
//My version can be found on semanticsearchart.com or ezcodesample.com
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

class Feature {

    public int dim; // starts from 0
    public double weight;

    public Feature(int initDim, double initWeight) {
        dim = initDim;
        weight = initWeight;
    }
}

class Data {

    private int id = -1;
    private List<Feature> features = null;
    private int label = Integer.MIN_VALUE;
    private int size = -1;

    //Create a Data object from string data line. The data line is in svmlight
    //and libsvm format but with feature index starts from 0. For example:
    //"-1 0:0.43 1:0.12 9284:0.2". please be care that the index here starts
    // from 0 rather than 1 which used in svmlight and libsvm, and no comment
    // part is included.
    //
    // @param initID
    //            the data id
    // @param line
    //            the data line in svmlight and libsvm format with feature index
    //            starts from 0
    //
    public Data(int initID, String line) {
        this.id = initID;
        this.features = readData(line);
        this.size = this.features.size();
    }

    public Data(int initID, CandidateList clist, HashMap<String, Integer> vocabulary) {
        this.id = initID;
        this.features = readData(clist, vocabulary);
        this.size = this.features.size();
    }

    private ArrayList<Feature> readData(String line) {
        StringTokenizer stk = new StringTokenizer(line);
        try {
            // get label
            this.label = Integer.parseInt(stk.nextToken());
            // get features
            ArrayList<Feature> fs = new ArrayList<Feature>();
            while (stk.hasMoreTokens()) {
                String pair[] = stk.nextToken().split(":");
                int dim = Integer.parseInt(pair[0]);
                double value = Double.parseDouble(pair[1]);
                fs.add(new Feature(dim, value));
            }
            return fs;
        } catch (NumberFormatException nfe) {
            System.out.println("Error readData");
            return null;
        }
    }

    public int getID() {
        return this.id;
    }

    public Feature getFeatureAt(int position) {
        return features.get(position);
    }

    public int getLabel() {
        return this.label;
    }

    public List<Feature> getAllFeature() {
        return features;
    }

    public int size() {
        return this.size;
    }

    private List<Feature> readData(CandidateList clist, HashMap<String, Integer> vocabulary) {
        ArrayList<Feature> fs = new ArrayList<>();
        int vSize = vocabulary.keySet().size();
        for (String item : clist.items) {
            if (!vocabulary.containsKey(item)) {
                vocabulary.put(item, vSize++);
            }
            int dim = vocabulary.get(item);
            double value = 1.0;
            Feature f = new Feature(dim, value);
            fs.add(f);
        }
        return fs;
    }
}

class Dataset {

    private ArrayList<Data> datas;
    // total number of data
    private int dataNum = -1;
    // total number of distinct features
    private int featureNum = -1;
    // dictionary: word id -> word
    String[] dict;

    public Dataset() {
        this.datas = new ArrayList<Data>();
        refreshStatistics();
    }

    public Dataset(ArrayList<Data> initDatas) {
        this.datas = initDatas;
        refreshStatistics();
    }

// this is data matrix        
//	{9.0, 2.0, 1.0, 1.0, 1.0, 0.0},   
//	{8.0, 3.0, 2.0, 1.0, 0.0, 0.0},   
//	{3.0, 0.0, 0.0, 1.0, 2.0, 8.0},
//	{0.0, 1.0, 0.0, 2.0, 4.0, 7.0},
//	{2.0, 1.0, 1.0, 0.0, 1.0, 3.0},        
    public Dataset(File datafile) {
        this.datas = new ArrayList<Data>();
        try {
            //this is format for data matrix
//            String[] lines = {
//                "-1 0:9 1:2 2:1 3:1 4:1",
//                "-2 0:8 1:3 2:2 3:1",
//                "-3 0:3 3:1 4:2 5:8",
//                "-4 1:1 3:2 4:4 5:7",
//                "-5 0:3 1:2 2:2 4:1 5:6"
//            };
            //is replaced by hard coded data for testing 
            //List<String> datalines = Arrays.asList(lines);
            //old operation reading from file
            BufferedReader reader = Utility.getReader(datafile);

            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                datas.add(new Data(i, line));
                i++;
            }
            refreshStatistics();
        } catch (Exception e) {
            System.out.println("Dataset(File datafile): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Dataset(List<CandidateList> clists) {
        this.datas = new ArrayList<>();
        HashMap<String, Integer> vocabulary = new HashMap<>();
        int i = 0;
        for (CandidateList clist : clists) {
            datas.add(new Data(i++, clist, vocabulary));
        }

        dict = new String[vocabulary.size()];
        for (String item : vocabulary.keySet()) {
            dict[vocabulary.get(item)] = item;
        }
    }

    private void refreshStatistics() {
        this.dataNum = this.datas.size();
        HashSet<Integer> calculator = new HashSet<Integer>();
        for (Data d : datas) {
            for (Feature f : d.getAllFeature()) {
                calculator.add(f.dim);
            }
        }
        this.featureNum = calculator.size();
    }

    public Data getDataAt(int index) {
        return datas.get(index);
    }

    public ArrayList<Data> getAllData() {
        return datas;
    }

    public int size() {
        return this.dataNum;
    }

    public int getFeatureNum() {
        return this.featureNum;
    }
}

class Posting {

    int docID; // the doc where the word occur
    int pos; // the position of the word in this document

    public Posting(int id, int position) {
        this.docID = id;
        this.pos = position;
    }
}

public class pLSAClusterer {

    private Dataset dataset = null;
    private Posting[][] invertedIndex = null;
    private int M = -1; // number of data
    private int V = -1; // number of words
    private int K = -1; // number of topics

    public pLSAClusterer() {
    }

    public static void main(String[] args) {

        pLSAClusterer plsa = new pLSAClusterer();
        //the file is not used, the hard coded data is used instead, but file name should be valid,
        //just replace the name by something valid.
        plsa.doPLSA("C:\\Users\\APolar\\workspace\\PLSA\\src\\data.txt", 2, 60);
        System.out.println("end PLSA");
    }

    public boolean doPLSA(String datafileName, int ntopics, int iters) {
        File datafile = new File(datafileName);
        if (datafile.exists()) {
            if ((this.dataset = new Dataset(datafile)) == null) {
                System.out.println("doPLSA, dataset == null");
                return false;
            }
            this.M = this.dataset.size();
            this.V = this.dataset.getFeatureNum();
            this.K = ntopics;

            //build inverted index
            this.buildInvertedIndex(this.dataset);
            //run EM algorithm
            this.EM(iters);
            return true;

        } else {
            System.out.println("ProbabilisticLSA(String datafileName), datafile: " + datafileName + " doesn't exist");
            return false;
        }
    }

    //Build the inverted index for M-step fast calculation. Format:
    //invertedIndex[w][]: a unsorted list of document and position which word w
    // occurs. 
    //@param ds the dataset which to be analysis
    @SuppressWarnings("unchecked")
    private boolean buildInvertedIndex(Dataset ds) {
        ArrayList<Posting>[] list = new ArrayList[this.V];
        for (int k = 0; k < this.V; ++k) {
            list[k] = new ArrayList<Posting>();
        }

        for (int m = 0; m < this.M; m++) {
            Data d = ds.getDataAt(m);
            for (int position = 0; position < d.size(); position++) {
                int w = d.getFeatureAt(position).dim;
                // add posting
                list[w].add(new Posting(m, position));
            }
        }
        // convert to array
        this.invertedIndex = new Posting[this.V][];
        for (int w = 0; w < this.V; w++) {
            this.invertedIndex[w] = list[w].toArray(new Posting[0]);
        }
        return true;
    }

    private boolean EM(int iters) {
        // p(z), size: K
        double[] Pz = new double[this.K];

        // p(d|z), size: K x M
        double[][] Pd_z = new double[this.K][this.M];

        // p(w|z), size: K x V
        double[][] Pw_z = new double[this.K][this.V];

        // p(z|d,w), size: K x M x doc.size()
        double[][][] Pz_dw = new double[this.K][this.M][];

        // L: log-likelihood value
        double L = -1;

        // run EM algorithm
        this.init(Pz, Pd_z, Pw_z, Pz_dw);
        for (int it = 0; it < iters; it++) {
            // E-step
            if (!this.Estep(Pz, Pd_z, Pw_z, Pz_dw)) {
                System.out.println("EM,  in E-step");
            }

            // M-step
            if (!this.Mstep(Pz_dw, Pw_z, Pd_z, Pz)) {
                System.out.println("EM, in M-step");
            }

            L = calcLoglikelihood(Pz, Pd_z, Pw_z);
            //System.err.println("[" + it + "]" + "\tlikelihood: " + L);
        }

        //print result
//        for (int m = 0; m < this.M; m++) {
//            double norm = 0.0;
//            for (int z = 0; z < this.K; z++) {
//                norm += Pd_z[z][m];
//            }
//            if (norm <= 0.0) {
//                norm = 1.0;
//            }
//            for (int z = 0; z < this.K; z++) {
//                System.out.format("%10.4f", Pd_z[z][m] / norm);
//            }
//            System.out.println();
//        }
        for (int z = 0; z < this.K; z++) {
            ArrayList<Prob> ps = new ArrayList<Prob>();
            for (int w = 0; w < this.V; w++) {
                ps.add(new Prob(w, Pw_z[z][w]));
            }
            Collections.sort(ps);
            for (int i = 0; i < 20; i++) {
                System.out.format("%d:%.4f\t", ps.get(i).w, ps.get(i).score);
            }
            System.out.println();
        }
        return false;
    }

    class Prob implements Comparable<Prob> {

        int w;
        double score;

        private Prob(int w, double s) {
            this.w = w;
            this.score = s;
        }

        @Override
        public int compareTo(Prob t) {
            if (Math.abs(this.score - t.score) < 0.00000000001) {
                return 0;
            } else if (this.score > t.score) {
                return -1;
            } else {
                return 1;
            }
        }

        @Override
        public String toString() {
            return w + ":" + score;
        }
    }

    private boolean init(double[] Pz, double[][] Pd_z, double[][] Pw_z, double[][][] Pz_dw) {
        // p(z), size: K
        double zvalue = (double) 1 / (double) this.K;
        for (int z = 0; z < this.K; z++) {
            Pz[z] = zvalue;
        }

        // p(d|z), size: K x M
        for (int z = 0; z < this.K; z++) {
            double norm = 0.0;
            for (int m = 0; m < this.M; m++) {
                Pd_z[z][m] = Math.random();
                norm += Pd_z[z][m];
            }

            for (int m = 0; m < this.M; m++) {
                Pd_z[z][m] /= norm;
            }
        }

        // p(w|z), size: K x V
        for (int z = 0; z < this.K; z++) {
            double norm = 0.0;
            for (int w = 0; w < this.V; w++) {
                Pw_z[z][w] = Math.random();
                norm += Pw_z[z][w];
            }

            for (int w = 0; w < this.V; w++) {
                Pw_z[z][w] /= norm;
            }
        }

        // p(z|d,w), size: K x M x doc.size()
        for (int z = 0; z < this.K; z++) {
            for (int m = 0; m < this.M; m++) {
                Pz_dw[z][m] = new double[this.dataset.getDataAt(m).size()];
            }
        }
        return false;
    }

    private boolean Estep(double[] Pz, double[][] Pd_z, double[][] Pw_z, double[][][] Pz_dw) {
        for (int m = 0; m < this.M; m++) {
            Data data = this.dataset.getDataAt(m);
            for (int position = 0; position < data.size(); position++) {
                // get word(dimension) at current position of document m
                int w = data.getFeatureAt(position).dim;

                double norm = 0.0;
                for (int z = 0; z < this.K; z++) {
                    double val = Pz[z] * Pd_z[z][m] * Pw_z[z][w];
                    Pz_dw[z][m][position] = val;
                    norm += val;
                }

                // normalization
                for (int z = 0; z < this.K; z++) {
                    Pz_dw[z][m][position] /= norm;
                }
            }
        }
        return true;
    }

    private boolean Mstep(double[][][] Pz_dw, double[][] Pw_z, double[][] Pd_z, double[] Pz) {
        // p(w|z)
        for (int z = 0; z < this.K; z++) {
            double norm = 0.0;
            for (int w = 0; w < this.V; w++) {
                double sum = 0.0;

                Posting[] postings = this.invertedIndex[w];
                for (Posting posting : postings) {
                    int m = posting.docID;
                    int position = posting.pos;
                    double n = this.dataset.getDataAt(m).getFeatureAt(position).weight;
                    sum += n * Pz_dw[z][m][position];
                }
                Pw_z[z][w] = sum;
                norm += sum;
            }

            // normalization
            for (int w = 0; w < this.V; w++) {
                Pw_z[z][w] /= norm;
            }
        }

        // p(d|z)
        for (int z = 0; z < this.K; z++) {
            double norm = 0.0;
            for (int m = 0; m < this.M; m++) {
                double sum = 0.0;
                Data d = this.dataset.getDataAt(m);
                for (int position = 0; position < d.size(); position++) {
                    double n = d.getFeatureAt(position).weight;
                    sum += n * Pz_dw[z][m][position];
                }
                Pd_z[z][m] = sum;
                norm += sum;
            }

            // normalization
            for (int m = 0; m < this.M; m++) {
                Pd_z[z][m] /= norm;
            }
        }

        //This is definitely a bug
        //p(z) values are even, but they should not be even
        double norm = 0.0;
        for (int z = 0; z < this.K; z++) {
            double sum = 0.0;
            for (int m = 0; m < this.M; m++) {
                sum += Pd_z[z][m];
            }
            Pz[z] = sum;
            norm += sum;
        }

        // normalization
        for (int z = 0; z < this.K; z++) {
            Pz[z] /= norm;
            //System.out.format("%10.4f", Pz[z]);  //here you can print to see
        }
        //System.out.println();

        return true;
    }

    private double calcLoglikelihood(double[] Pz, double[][] Pd_z, double[][] Pw_z) {
        double L = 0.0;
        for (int m = 0; m < this.M; m++) {
            Data d = this.dataset.getDataAt(m);
            for (int position = 0; position < d.size(); position++) {
                Feature f = d.getFeatureAt(position);
                int w = f.dim;
                double n = f.weight;

                double sum = 0.0;
                for (int z = 0; z < this.K; z++) {
                    sum += Pz[z] * Pd_z[z][m] * Pw_z[z][w];
                }
                L += n * Math.log10(sum);
            }
        }
        return L;
    }
}
