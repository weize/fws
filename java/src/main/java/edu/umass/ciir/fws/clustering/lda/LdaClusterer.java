/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.lda;

/**
 * Parts of the codes are taken
 * from:http://code.google.com/p/mltool4j/source/browse/trunk/src/edu/thu/mltool4j/topicmodel/plsa
 * . The original code has a bug, in E-step when computing P(z). It is fixed
 * here.
 *
 * @author wkong
 */
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.umass.ciir.fws.clustering.plsa.*;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.types.QueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

@Verified
@InputClass(className = "edu.umass.ciir.fws.types.QueryParameters")
public class LdaClusterer implements Processor<QueryParameters> {

    String clusterDir;
    String clistDir;
    long topNum;
    long iterNum;

    public LdaClusterer(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        clusterDir = p.getString("ldaClusterDir");
        iterNum = p.getLong("ldaIterNum");
        clistDir = p.getString("clistDir");
        topNum = p.getLong("topNum");
    }

    @Override
    public void process(QueryParameters queryParameters) throws IOException {
        String qid = queryParameters.id;
        System.err.println(String.format("Processing qid:%s parameters:%s", qid, queryParameters.parameters));

        int topicNum = Integer.parseInt(queryParameters.parameters);

        // load candidate lists
        File clistFile = new File(Utility.getCandidateListCleanFileName(clistDir, qid));
        List<CandidateList> clist = CandidateList.loadCandidateLists(clistFile, topNum);

        // clustering
        Lda lda = new Lda(clist, iterNum);
        List<ScoredFacet> facets = lda.cluster(topicNum);

        // output
        String clusterFileName = Utility.getPlsaClusterFileName(clusterDir, qid, topicNum);
        Utility.createDirectoryForFile(clusterFileName);
        ScoredFacet.output(facets, new File(clusterFileName));
        System.err.println("Written in " + clusterFileName);
    }

    @Override
    public void close() throws IOException {
    }

    public static class Lda {

        final Pattern tokenPattern = Pattern.compile("[^\\|]+");

        long iterNum;
        InstanceList instances; // documents (candidate lists) data

        public Lda(List<CandidateList> clists, long iterNum) {
            this.iterNum = iterNum;
            loadData(clists);
        }

        private List<ScoredFacet> cluster(int topicNum) throws IOException {
            ParallelTopicModel topicModel;

            double alpha = 50.0;
            double beta = 0.01;
            topicModel = new ParallelTopicModel(topicNum, alpha, beta);
            topicModel.addInstances(instances);
            topicModel.setNumIterations((int) iterNum);
            topicModel.setSymmetricAlpha(false);

            /**
             * This is the default setting for OptimizeInterval in mallet
             * train-topics. Specified here again b/c it is different from
             * ParallelTopicModel constructor.
             */
            topicModel.setOptimizeInterval(0);
            topicModel.estimate();

            
            // output
            ArrayList<ScoredFacet> facets = new ArrayList<>();

            for (int topic = 0; topic < topicModel.numTopics; topic++) {

                List<ScoredItem> items = new ArrayList<>();
                double weightSum = 0;
                for (int type = 0; type < topicModel.numTypes; type++) {

                    int[] topicCounts = topicModel.typeTopicCounts[type];

                    double weight = beta;

                    int index = 0;
                    while (index < topicCounts.length
                            && topicCounts[index] > 0) {

                        int currentTopic = topicCounts[index] & topicModel.topicMask;

                        if (currentTopic == topic) {
                            weight += topicCounts[index] >> topicModel.topicBits;
                            break;
                        }

                        index++;
                    }

                    String item = topicModel.alphabet.lookupObject(type).toString();
                    items.add(new ScoredItem(item, weight));
                    weightSum += weight;
                }

                // normalization for P(word|topic)
                for (ScoredItem item : items) {
                    item.score /= weightSum;
                }
                Collections.sort(items);
                items = items.subList(0, Math.min(items.size(), 50));

                // normalization for P(topic)
                double score = topicModel.alpha[topic];

                facets.add(new ScoredFacet(items, score));
            }

            return facets;
        }

        private void loadData(List<CandidateList> clists) {
            ArrayList<Pipe> pipeList = new ArrayList<>();
            // Add the tokenizer
            pipeList.add(new CharSequence2TokenSequence(tokenPattern));
            // --keep-sequence
            pipeList.add(new TokenSequence2FeatureSequence());

            Pipe instancePipe = new SerialPipes(pipeList);

            instances = new InstanceList(instancePipe);

            instances.addThruPipe(new CandidateListIterator(clists));

        }

        static class CandidateListIterator implements Iterator<Instance> {

            Iterator<CandidateList> clistIterator;
            int clistNum;

            private CandidateListIterator(List<CandidateList> clists) {
                clistIterator = clists.iterator();
                clistNum = 0;
            }

            @Override
            public boolean hasNext() {
                return clistIterator.hasNext();
            }

            @Override
            public Instance next() {
                // next clist
                CandidateList clist = clistIterator.next();
                clistNum++;

                // construct instance
                String data = clist.itemList;
                String uri = "clistnum:" + clistNum;

                return new Instance(data, null, uri, null);
            }

            @Override
            public void remove() {
                throw new IllegalStateException("This Iterator<Instance> does not support remove().");
            }
        }

    }
}
