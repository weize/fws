/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.plsa;

/**
 * Parts of the codes are taken
 * from:http://code.google.com/p/mltool4j/source/browse/trunk/src/edu/thu/mltool4j/topicmodel/plsa
 * . The original code has a bug, in E-step when computing P(z). It is fixed
 * here.
 *
 * @author wkong
 */
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class PlsaClusterer implements Processor<TfQueryParameters> {

    String clusterDir;
    String clistDir;
    long topNum;
    long iterNum;

    public PlsaClusterer(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String runDir = p.getString("plsaRunDir");
        clusterDir = Utility.getFileName(runDir, "cluster");
        iterNum = p.getLong("plsaIterNum");
        clistDir = p.getString("clistDir");
        topNum = p.getLong("topNum");

    }

    @Override
    public void process(TfQueryParameters queryParameters) throws IOException {
        String qid = queryParameters.id;
        System.err.println(String.format("Processing qid:%s parameters:%s", qid, queryParameters.parameters));

        int topicNum = Integer.parseInt(queryParameters.parameters);

        // output
        File clusterFile = new File(Utility.getPlsaClusterFileName(clusterDir, qid, topicNum));
        if (clusterFile.exists()) {
            Utility.infoFileExists(clusterFile);
            return;
        }

        // loadClusters candidate lists
        File clistFile = new File(Utility.getCandidateListCleanFileName(clistDir, qid));
        List<CandidateList> clist = CandidateList.loadCandidateLists(clistFile, topNum);

        // clustering
        Plsa plsa = new Plsa(clist, iterNum);
        List<ScoredFacet> facets = plsa.cluster(topicNum);

        Utility.infoOpen(clusterFile);
        Utility.createDirectoryForFile(clusterFile);
        ScoredFacet.output(facets, clusterFile);
        Utility.infoWritten(clusterFile);
    }

    @Override
    public void close() throws IOException {
    }

    class Plsa {

        // data: documents (or candidate lists in our case)
        ArrayList<Data> datas;
        // total number of data
        int M;
        // total number of distinct features
        int V;
        // number of topics
        int K;
        // dictionary: word id -> word
        String[] dict;

        // inverted 
        Posting[][] invertedIndex;

        // p(z), size: K
        double[] Pz;
        // p(w|z), size: K x V
        double[][] Pw_z;

        long iterNum;

        class Feature {

            public int dim; // starts from 0
            public double weight;

            public Feature(int initDim, double initWeight) {
                dim = initDim;
                weight = initWeight;
            }
        }

        // A document in plsa. In our case, it's a candidate list
        class Data {

            int id;
            List<Feature> features;
            int size;

            public Data(int id, CandidateList clist, HashMap<String, Integer> vocabulary) {
                this.id = id;
                features = new ArrayList<>();
                int vSize = vocabulary.keySet().size();
                for (String item : clist.items) {
                    if (!vocabulary.containsKey(item)) {
                        vocabulary.put(item, vSize++);
                    }
                    int dim = vocabulary.get(item);
                    double value = 1.0;
                    Feature f = new Feature(dim, value);
                    features.add(f);
                }
                size = features.size();
            }

            public Feature getFeatureAt(int position) {
                return features.get(position);
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

        private Plsa(List<CandidateList> clists, long iterNum) {
            this.iterNum = iterNum;

            // loadClusters data
            datas = new ArrayList<>();
            HashMap<String, Integer> vocabulary = new HashMap<>();
            int id = 0;
            for (CandidateList clist : clists) {
                datas.add(new Data(id++, clist, vocabulary));
            }

            M = datas.size();
            V = vocabulary.size();

            // word id -> word
            dict = new String[V];
            for (String item : vocabulary.keySet()) {
                dict[vocabulary.get(item)] = item;
            }

            buildInvertedIndex();
        }

        public List<ScoredFacet> cluster(int topicNum) {
            K = topicNum;
            EM();

            // probs to facets
            ArrayList<ScoredFacet> facets = new ArrayList<>();
            for (int z = 0; z < K; z++) {
                ArrayList<ScoredItem> items = new ArrayList<>();
                for (int w = 0; w < this.V; w++) {
                    items.add(new ScoredItem(dict[w], Pw_z[z][w]));
                }
                Collections.sort(items);
                ScoredFacet facet = new ScoredFacet(items.subList(0, Math.min(items.size(), 50)), Pz[z]);
                facets.add(facet);
            }
            Collections.sort(facets);
            return facets;
        }

        private double EM() {
            // p(z), size: K
            Pz = new double[this.K];
            // p(w|z), size: K x V
            Pw_z = new double[this.K][this.V];
            // p(d|z), size: K x M
            double[][] Pd_z = new double[this.K][this.M];
            // p(z|d,w), size: K x M x doc.size()
            double[][][] Pz_dw = new double[this.K][this.M][];

            // L: log-likelihood value
            double L = -1;

            init(Pz, Pd_z, Pw_z, Pz_dw);
            for (int it = 0; it < iterNum; it++) {
                // E-step
                Estep(Pz, Pd_z, Pw_z, Pz_dw);
                // M-step
                Mstep(Pz_dw, Pw_z, Pd_z, Pz);

                L = calcLoglikelihood(Pz, Pd_z, Pw_z);
                if (it % 100 == 0) {
                    System.err.println("[" + it + "]" + "\tlikelihood: " + L);
                    printCurTopWords();
                    System.err.println();
                }

            }

            return L;
        }

        private void printCurTopWords() {
            for (int z = 0; z < K; z++) {
                ArrayList<ScoredItem> items = new ArrayList<>();
                for (int w = 0; w < this.V; w++) {
                    items.add(new ScoredItem(dict[w], Pw_z[z][w]));
                }
                Collections.sort(items);
                ScoredFacet facet = new ScoredFacet(items.subList(0, Math.min(items.size(), 7)), Pz[z]);
                System.err.println(facet.toString());
            }
        }

        private void Estep(double[] Pz, double[][] Pd_z, double[][] Pw_z, double[][][] Pz_dw) {
            for (int m = 0; m < this.M; m++) {
                Data data = datas.get(m);
                for (int position = 0; position < data.size; position++) {
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
        }

        private void Mstep(double[][][] Pz_dw, double[][] Pw_z, double[][] Pd_z, double[] Pz) {
            // p(w|z)
            for (int z = 0; z < this.K; z++) {
                double norm = 0.0;
                for (int w = 0; w < this.V; w++) {
                    double sum = 0.0;

                    Posting[] postings = this.invertedIndex[w];
                    for (Posting posting : postings) {
                        int m = posting.docID;
                        int position = posting.pos;
                        double n = datas.get(m).getFeatureAt(position).weight;
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
            double[] normsPd_z = new double[K];
            for (int z = 0; z < this.K; z++) {
                double norm = 0.0;
                for (int m = 0; m < this.M; m++) {
                    double sum = 0.0;
                    Data d = datas.get(m);
                    for (int position = 0; position < d.size; position++) {
                        double n = d.getFeatureAt(position).weight;
                        sum += n * Pz_dw[z][m][position];
                    }
                    Pd_z[z][m] = sum;
                    norm += sum;
                }

                // keep it, and normalize later, because unnomalized Pd_z are used for computing Pz below.
                normsPd_z[z] = norm;
            }

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

            // normalize  P(d|z)
            for (int z = 0; z < this.K; z++) {
                for (int m = 0; m < this.M; m++) {
                    Pd_z[z][m] /= normsPd_z[z];
                }
            }
        }

        private double calcLoglikelihood(double[] Pz, double[][] Pd_z, double[][] Pw_z) {
            double L = 0.0;
            for (int m = 0; m < this.M; m++) {
                Data d = datas.get(m);
                for (int position = 0; position < d.size; position++) {
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

        private void init(double[] Pz, double[][] Pd_z, double[][] Pw_z, double[][][] Pz_dw) {
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
                    Pz_dw[z][m] = new double[datas.get(m).size];
                }
            }
        }

        /**
         * Build the inverted index for M-step fast calculation. Format:
         * invertedIndex[w][]: a unsorted list of document and position which
         * word w occurs.
         */
        private void buildInvertedIndex() {
            ArrayList<Posting>[] list = new ArrayList[this.V];
            for (int k = 0; k < this.V; ++k) {
                list[k] = new ArrayList<>();
            }

            for (int m = 0; m < this.M; m++) {
                Data d = datas.get(m);
                for (int position = 0; position < d.size; position++) {
                    int w = d.getFeatureAt(position).dim;
                    // add posting
                    list[w].add(new Posting(m, position));
                }
            }
            // convert to array
            invertedIndex = new Posting[V][];
            for (int w = 0; w < this.V; w++) {
                invertedIndex[w] = list[w].toArray(new Posting[0]);
            }
        }
    }
}
