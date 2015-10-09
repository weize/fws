/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.query.QueryTopicSubtopicMap;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Facets Created for one query by annotator
 *
 * @author wkong
 */
public class FacetAnnotation {

    public String annotatorID;
    public String qid;
    public ArrayList<AnnotatedFacet> facets;

    public FacetAnnotation(String annotatorID, String queryID) {
        this.annotatorID = annotatorID;
        this.qid = queryID;
        facets = new ArrayList<>();
    }

    public void orderFacets() {
        ArrayList<AnnotatedFacet> oredered = new ArrayList<>();
        for(AnnotatedFacet af : facets) {
            if (af.rating == 2) {
                oredered.add(af);
            }
        }
        
        for(AnnotatedFacet af : facets) {
            if (af.rating == 1) {
                oredered.add(af);
            }
        }
        facets = oredered;
    }
    
    public Parameters toParameters() {
        this.orderFacets();
        Parameters fa = new Parameters();
        fa.put("number", qid);
      
        List<Parameters> facetsParams = new ArrayList<>();
        for(AnnotatedFacet f : facets) {
            facetsParams.add(f.toParameters());
        }
        
        fa.put("facets", facetsParams);
        return fa;
    }

    public int getTermSize() {
        int count = 0;
        for (AnnotatedFacet f : facets) {
            count += f.size();
        }
        return count;
    }

    public void addFacet(AnnotatedFacet facet) {
        this.facets.add(facet);
    }

    public void sortFacets() {
        Collections.sort(facets);
    }

    public String listAsString() {
        StringBuilder lists = new StringBuilder();
        for (AnnotatedFacet f : facets) {
            lists.append(String.format("%s\t%s\t%s\n", this.annotatorID, this.qid, f.toString()));
        }
        return lists.toString();
    }

    public static FacetAnnotation parseFromJson(String jsonDataString) throws IOException {
        return parseFromJson(jsonDataString, true);
    }

    /**
     * Need to use a clean json format for facet annotation
     * @param jsonDataString
     * @param filter
     * @return
     * @throws IOException 
     */
    public static FacetAnnotation parseFromJson(String jsonDataString, boolean filter) throws IOException {
        Parameters data = Parameters.parseString(jsonDataString);
        String annotatorID = data.getString("annotatorID");
        String queryID = data.getString("aolUserID");
        Parameters facetMap = data.getMap("explicitlySaved");

        if (filter && annotatorID.startsWith("test-")) {
            return null;
        }
        FacetAnnotation fa = new FacetAnnotation(annotatorID, queryID);

        // to sort ids
        ArrayList<Integer> facetIds = new ArrayList<>();
        for (String fid : facetMap.getKeys()) {
            facetIds.add(Integer.parseInt(fid));
        }
        Collections.sort(facetIds);

        for (Integer fid : facetIds) {
            Parameters facet = facetMap.getMap(fid.toString());
            int rating = mapRating(Integer.parseInt(facet.getString("rating")));
            String description = facet.getString("description").replaceAll("\\s+", " ");

            AnnotatedFacet f = new AnnotatedFacet(rating, fid.toString(), description);

            // to sort ids
            ArrayList<Integer> itemIds = new ArrayList<>();
            for (String tid : facet.getKeys()) {
                if (!tid.equals("rating") && !tid.equals("description")) {
                    itemIds.add(Integer.parseInt(tid));
                }
            }
            Collections.sort(itemIds);

            for (Integer itemId : itemIds) {
                String item = facet.getMap(itemId.toString()).getAsList("queries", Parameters.class).get(0).getString("query");
                f.addTerm(item);
            }

            if (f.size() > 0) {
                fa.addFacet(f);
            }
        }

        return fa;
    }

    public static void select(File jsonFile, File jsonFilteredFile, QueryTopicSubtopicMap queryMap) throws IOException {
        BufferedReader reader = Utility.getReader(jsonFile);
        BufferedWriter writer = Utility.getWriter(jsonFilteredFile);
        String line;
        while ((line = reader.readLine()) != null) {
            Parameters p = filterJson(line, queryMap);
            if (p != null) {
                writer.write(p.toString());
                writer.newLine();
            }
        }
        reader.close();
        writer.close();
    }

    public static Parameters filterJson(String jsonDataString, QueryTopicSubtopicMap queryMap) throws IOException {
        Parameters data = Parameters.parseString(jsonDataString);
        String annotatorID = data.getString("annotatorID");
        String queryID = data.getString("aolUserID");

        if (!annotatorID.startsWith("test-") && queryMap.hasTopic(queryID)) {
            return data;
        } else {
            return null;
        }
    }

    public static List<FacetAnnotation> load(File jsonFile) throws IOException {
        ArrayList<FacetAnnotation> annotations = new ArrayList<>();
        BufferedReader reader = Utility.getReader(jsonFile);
        String line;
        while ((line = reader.readLine()) != null) {
            FacetAnnotation facetAnnotation = FacetAnnotation.parseFromJson(line);
            if (facetAnnotation != null) {
                annotations.add(facetAnnotation);
            }
        }
        reader.close();
        return annotations;
    }

    public static HashMap<String, FacetAnnotation> loadAsMap(File jsonFile) throws IOException {
        HashMap<String, FacetAnnotation> map = new HashMap<>();
        List<FacetAnnotation> annotation = load(jsonFile);
        for (FacetAnnotation f : annotation) {
            map.put(f.qid, f);
        }
        return map;
    }

    public static void outputAsFacet(FacetAnnotation fa, File outfile) throws IOException {
        List<ScoredFacet> facets = new ArrayList<>();
        Collections.sort(fa.facets);//sort by rating then id
        for (AnnotatedFacet f : fa.facets) {
            if (f.isValid()) {
                facets.add(f.toScoredFacet());
            }
        }
        ScoredFacet.outputAsFacets(facets, outfile);
    }

    /**
     * convert rating in databse to real rating
     *
     * @param orgRating
     * @return
     */
    private static int mapRating(int orgRating) throws IOException {
        switch (orgRating) {
            case 0: // not assigned
                return 0;
            case 1: // bad
                return -1;
            case 2: // fair
                return 1;
            case 3: // good
                return 2;
            default:
                throw new IOException("rating invalid");
        }
    }
}
