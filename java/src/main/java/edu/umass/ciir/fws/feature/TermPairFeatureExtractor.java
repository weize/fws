/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.crawl.RankedDocument;
import edu.umass.ciir.fws.crawl.QuerySetResults;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class TermPairFeatureExtractor {

    public final static int numTopScoredItems = 1000;
    final static int textContextDist = 25;
    String clistDir;
    long topNum;
    String rankedListFile;
    String docDir;
    String qid;

    QuerySetResults querySetResults;
    List<CandidateList> clists;
    List<RankedDocument> docs;
    List<ScoredItem> items;

    HashMap<String, Integer> itemIdMap; // item -> id
    HashMap<String, ItemPairFeature> itemPairFeatures;

    public TermPairFeatureExtractor(Parameters p) throws Exception {
        clistDir = p.getString("clistDir");
        topNum = p.getLong("topNum");
        rankedListFile = p.getString("rankedListFile");
        docDir = p.getString("docDir");

        loadQuerySetResults();
    }

    public void extract(List<ScoredItem> items, String qid) throws IOException {
        this.qid = qid;
        loadCandidateLists();
        loadDocuments();
        loadItemsAndSetIds(items);
        generateItemPairs();

        extractLengthDiff();
        extractListFreq();
        extractContextListSim();
        extractContextTextSim();
    }

    public void extract(File termPredictFile, String qid) throws IOException {
        extract(loadItemsFromPredictFile(termPredictFile), qid);
    }

    private void extractLengthDiff() {
        int[] lens = new int[items.size()];
        for (int i = 0; i < lens.length; i++) {
            lens[i] = TextProcessing.countWords(items.get(i).item);
        }

        for (int i = 0; i < lens.length; i++) {
            for (int j = i + 1; j < lens.length; j++) {
                int diff = Math.abs(lens[i] - lens[j]);
                String pairId = getItemPairId(i, j);
                itemPairFeatures.get(pairId).setFeature(diff, ItemPairFeature._lenDiff);
            }
        }
    }

    private void extractListFreq() {
        // intialized to 0
        for (String pid : itemPairFeatures.keySet()) {
            itemPairFeatures.get(pid).setFeature(0, ItemPairFeature._listFreq);
        }

        for (CandidateList clist : clists) {
            for (int i = 0; i < clist.items.length; i++) {
                String item1 = clist.items[i];
                if (itemIdMap.containsKey(item1)) {
                    for (int j = i + 1; j < clist.items.length; j++) {
                        String item2 = clist.items[j];
                        if (itemIdMap.containsKey(item2)) {
                            String pid = getItemPairId(item1, item2);
                            itemPairFeatures.get(pid).incFeature(ItemPairFeature._listFreq);

                        }
                    }
                }
            }
        }
    }

    private void extractContextListSim() {
        // build Context
        HashMap<String, Double>[] itemContexts = (HashMap<String, Double>[]) new HashMap<?, ?>[items.size()];

        for (int i = 0; i < itemContexts.length; i++) {
            itemContexts[i] = new HashMap<>();
        }

        for (CandidateList clist : clists) {
            for (int i = 0; i < clist.items.length; i++) {
                String item1 = clist.items[i];
                if (itemIdMap.containsKey(item1)) {
                    HashMap<String, Double> context = itemContexts[itemIdMap.get(item1)];
                    for (int j = 0; j < clist.items.length; j++) {
                        if (i != j) {
                            String item2 = clist.items[j];
                            incraseContextCount(context, item2);
                        }
                    }
                }
            }
        }

        setContextSim(itemContexts, ItemPairFeature._contextListSim);

    }

    protected void extractContextTextSim() {
        HashMap<String, Double>[] itemContexts = (HashMap<String, Double>[]) new HashMap<?, ?>[items.size()];

        for (int i = 0; i < itemContexts.length; i++) {
            itemContexts[i] = new HashMap<>();
        }

        for (RankedDocument doc : docs) {
            List<String> words = doc.terms;
            for (int i = 0; i < words.size(); i++) { // start of the idx
                StringBuilder itemBuilder = new StringBuilder();
                for (int j = i; j < i + CandidateList.MAX_TERM_SIZE && j < words.size(); j++) {
                    itemBuilder.append(itemBuilder.length() == 0 ? words.get(j) : " " + words.get(j));
                    String item = itemBuilder.toString();
                    if (itemIdMap.containsKey(item)) {
                        HashMap<String, Double> context = itemContexts[itemIdMap.get(item)];
                        // textContextDist left
                        for (int k = i - 1; k >= 0 && k >= i - textContextDist; k--) {
                            incraseContextCount(context, words.get(k));
                        }
                        for (int k = j + 1; k < words.size() && k <= j + textContextDist; k++) {
                            incraseContextCount(context, words.get(k));
                        }
                    }
                }
            }
        }

        setContextSim(itemContexts, ItemPairFeature._contextTextSim);
    }

    private void loadCandidateLists() throws IOException {
        File clistFile = new File(Utility.getCandidateListFileName(clistDir, qid, "clean.clist"));
        clists = CandidateList.loadCandidateLists(clistFile, topNum);
    }

    private void loadQuerySetResults() throws Exception {
        querySetResults = new QuerySetResults(rankedListFile, topNum);
    }

    private void loadDocuments() throws IOException {
        docs = RankedDocument.loadDocumentsFromFiles(querySetResults.get(qid), docDir, qid);
    }

    private List<ScoredItem> loadItemsFromPredictFile(File termPredictFile) throws IOException {
        BufferedReader reader = Utility.getReader(termPredictFile);
        String line;
        ArrayList<ScoredItem> allItems = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            // 0.00720595275504        -1      BIR_101118      cuttings over its lifetime
            String[] elems = line.split("\t");
            double score = Double.parseDouble(elems[0]);
            String item = elems[3];
            allItems.add(new ScoredItem(item, score));
        }
        reader.close();
        Collections.sort(allItems);
        return allItems.subList(0, Math.min(numTopScoredItems, allItems.size()));
    }

    private void loadItemsAndSetIds(List<ScoredItem> items) {
        this.items = items;
        itemIdMap = new HashMap<>();
        // item -> id
        for (int i = 0; i < items.size(); i++) {
            itemIdMap.put(items.get(i).item, i);
        }
    }

    private void generateItemPairs() {
        itemPairFeatures = new HashMap<>();

        for (int i = 0; i < items.size(); i++) {
            ScoredItem item1 = items.get(i);
            for (int j = i + 1; j < items.size(); j++) {
                ScoredItem item2 = items.get(j);
                ItemPairFeature pairFeature = new ItemPairFeature(item1.item, item2.item);
                String pairId = getItemPairId(i, j);
                itemPairFeatures.put(pairId, pairFeature);
            }
        }

    }

    public String getItemPairId(String item1, String item2) {
        return getItemPairId(itemIdMap.get(item1), itemIdMap.get(item2));
    }

    public String getItemPairId(int a, int b) {
        return a < b ? a + "_" + b : b + "_" + a;
    }

    private void incraseContextCount(HashMap<String, Double> context, String key) {
        double count = 1;
        if (context.containsKey(key)) {
            count += context.get(key);
        }
        context.put(key, count);
    }

    private void setContextSim(HashMap<String, Double>[] contexts, int index) {
        // normalize
        for (HashMap<String, Double> context : contexts) {
            double sum = 0;
            for (String item : context.keySet()) {
                double val = context.get(item);
                sum += val * val;
            }

            if (sum > Utility.epsilon) {
                sum = Math.sqrt(sum);
                for (String item : context.keySet()) {
                    double val = context.get(item) / sum;
                    context.put(item, val);
                }
            }
        }

        for (int i = 0; i < items.size(); i++) {
            HashMap<String, Double> context1 = contexts[i];
            for (int j = i + 1; j < items.size(); j++) {
                HashMap<String, Double> context2 = contexts[j];
                double sim = dotProduct(context1, context2); // cos sim in fact
                String pid = getItemPairId(i, j);
                itemPairFeatures.get(pid).setFeature(sim, index);
            }
        }
    }

    private double dotProduct(HashMap<String, Double> context1, HashMap<String, Double> context2) {
        double score = 0;
        for (String item : context1.keySet()) {
            if (context2.containsKey(item)) {
                score += context1.get(item) * context2.get(item);
            }
        }
        return score;
    }

    public void output(File dataFile) throws IOException {
        BufferedWriter writer = Utility.getWriter(dataFile);
        for (ItemPairFeature pair : itemPairFeatures.values()) {
            int rating = -1;
            writer.write(String.format("%d\t%s\t#%d\t%s\t%s\n", rating,
                    pair.featuresToString(), rating, qid, pair.itemPairToString()));
        }
        writer.close();

    }

    public void output(File dataFile, FacetAnnotation fa) throws IOException {
        HashSet<String> facetPairs = new HashSet<>();
        if (fa != null) {
            for (AnnotatedFacet f : fa.facets) {
                if (f.isValid()) {
                    List<String> terms = f.terms;
                    for (int i = 0; i < terms.size(); i++) {
                        for (int j = i + 1; j < terms.size(); j++) {
                            String a = terms.get(i);
                            String b = terms.get(j);
                            if (itemIdMap.containsKey(a) && itemIdMap.containsKey(b)) {
                                String pid = getItemPairId(a, b);
                                facetPairs.add(pid);
                            }
                        }
                    }
                }
            }
        }

        BufferedWriter writer = Utility.getWriter(dataFile);
        for (String pid : itemPairFeatures.keySet()) {
            ItemPairFeature pair = itemPairFeatures.get(pid);
            int rating = facetPairs.contains(pid) ? 1 : -1;
            writer.write(String.format("%d\t%s\t#%d\t%s\t%s\n", rating,
                    pair.featuresToString(), rating, qid, pair.itemPairToString()));
        }
        writer.close();

    }

    static class ItemPairFeature {

        public String item1;
        public String item2;
        public Object[] features;
        public static final int _lenDiff = 0;
        public static final int _listFreq = 1;
        public static final int _contextListSim = 2;
        public static final int _contextTextSim = 3;
        public static final int size = 4;

        public ItemPairFeature() {
        }

        public ItemPairFeature(String item1, String item2) {
            this.item1 = item1;
            this.item2 = item2;
            this.features = new Object[size];
        }

        public void setFeature(Object value, int idx) {
            this.features[idx] = value;
        }

        public Object getFeature(int idx) {
            return this.features[idx];
        }

        public String itemPairToString() {
            return item1 + "|" + item2;
        }

        public String featuresToString() {
            return TextProcessing.join(features, "\t");
        }

        @Override
        public String toString() {
            return itemPairToString() + "\t" + featuresToString();
        }

        public void incFeature(int idx) {
            features[idx] = (Integer) features[idx] + 1;
        }
    }
}
