package org.hupo.psi.mi.psicquic.model;

import org.apache.commons.lang.ArrayUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import psidev.psi.mi.calimocho.solr.converter.SolrFieldName;
import psidev.psi.mi.tab.PsimiTabReader;

import java.util.HashMap;
import java.util.Map;

/**
 * Psicquic solr server that is wrapping a solrServer
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>12/06/12</pre>
 */

public class PsicquicSolrServer {

    private final Logger logger = LoggerFactory.getLogger(PsicquicSolrServer.class);

    // static final variables
    private final static String STORED_FIELD_EXTENSION="_o";
    private static final String RETURN_TYPE_XML25 = "psi-mi/xml25";
    private static final String RETURN_TYPE_MITAB25 = "psi-mi/tab25";
    private static final String RETURN_TYPE_MITAB26 = "psi-mi/tab26";
    private static final String RETURN_TYPE_MITAB27 = "psi-mi/tab27";
    private static final String RETURN_TYPE_COUNT = "count";
    private static final String RETURN_TYPE_DEFAULT = RETURN_TYPE_MITAB25;
    private final static String DISMAX_PARAM_NAME = "qf";
    private final static String DEFAULT_PARAM_NAME = "df";
    private final static String DISMAX_TYPE = "edismax";
    private final static String QUERY_TYPE = "defType";

    /**
     * solr server
     */
    private SolrServer solrServer;
    /**
     * MITAB reader
     */
    private PsimiTabReader mitabReader;
    /**
     * Map of SOLR fields that can be returned for a specific format
     */
    private Map<String, String []> solrFields;
    
    public PsicquicSolrServer(SolrServer solrServer){
        this.solrServer = solrServer;

        if (this.solrServer == null){
            throw new IllegalArgumentException("Cannot create a new PsicquicSolrServer if the SolrServer is null");
        }

        // initialise default solr field map
        initializeSolrFieldsMap();

        // create new mitab reader without header
        mitabReader = new PsimiTabReader(false);
    }

    /**
     * Associates for each format the fields that are expected to be returned
     */
    private void initializeSolrFieldsMap(){
        solrFields = new HashMap<String, String[]>(5);

        String[] DATA_FIELDS_25 = new String[] {
                SolrFieldName.idA+STORED_FIELD_EXTENSION, SolrFieldName.idB+STORED_FIELD_EXTENSION, SolrFieldName.altidA+STORED_FIELD_EXTENSION,
                SolrFieldName.altidB+STORED_FIELD_EXTENSION, SolrFieldName.aliasA+STORED_FIELD_EXTENSION, SolrFieldName.aliasB+STORED_FIELD_EXTENSION,
                SolrFieldName.detmethod+STORED_FIELD_EXTENSION, SolrFieldName.pubauth+STORED_FIELD_EXTENSION, SolrFieldName.pubid+STORED_FIELD_EXTENSION,
                SolrFieldName.taxidA+STORED_FIELD_EXTENSION, SolrFieldName.taxidB+STORED_FIELD_EXTENSION, SolrFieldName.type+STORED_FIELD_EXTENSION,
                SolrFieldName.source+STORED_FIELD_EXTENSION,SolrFieldName.interaction_id+STORED_FIELD_EXTENSION, SolrFieldName.confidence+STORED_FIELD_EXTENSION
        };

        String[] DATA_FIELDS_26 = new String[] {
                SolrFieldName.idA+STORED_FIELD_EXTENSION, SolrFieldName.idB+STORED_FIELD_EXTENSION, SolrFieldName.altidA+STORED_FIELD_EXTENSION,
                SolrFieldName.altidB+STORED_FIELD_EXTENSION, SolrFieldName.aliasA+STORED_FIELD_EXTENSION, SolrFieldName.aliasB+STORED_FIELD_EXTENSION,
                SolrFieldName.detmethod+STORED_FIELD_EXTENSION, SolrFieldName.pubauth+STORED_FIELD_EXTENSION, SolrFieldName.pubid+STORED_FIELD_EXTENSION,
                SolrFieldName.taxidA+STORED_FIELD_EXTENSION, SolrFieldName.taxidB+STORED_FIELD_EXTENSION, SolrFieldName.type+STORED_FIELD_EXTENSION,
                SolrFieldName.source+STORED_FIELD_EXTENSION,SolrFieldName.interaction_id+STORED_FIELD_EXTENSION, SolrFieldName.confidence+STORED_FIELD_EXTENSION,
                SolrFieldName.complex+STORED_FIELD_EXTENSION, SolrFieldName.pbioroleA+STORED_FIELD_EXTENSION, SolrFieldName.pbioroleB+STORED_FIELD_EXTENSION,
                SolrFieldName.pexproleA+STORED_FIELD_EXTENSION, SolrFieldName.pexproleB+STORED_FIELD_EXTENSION, SolrFieldName.ptypeA+STORED_FIELD_EXTENSION,
                SolrFieldName.ptypeB+STORED_FIELD_EXTENSION, SolrFieldName.pxrefA+STORED_FIELD_EXTENSION, SolrFieldName.pxrefB+STORED_FIELD_EXTENSION,
                SolrFieldName.xref+STORED_FIELD_EXTENSION, SolrFieldName.pexproleA+STORED_FIELD_EXTENSION, SolrFieldName.annotA+STORED_FIELD_EXTENSION,
                SolrFieldName.annotB+STORED_FIELD_EXTENSION, SolrFieldName.annot+STORED_FIELD_EXTENSION, SolrFieldName.taxidHost+STORED_FIELD_EXTENSION,
                SolrFieldName.param+STORED_FIELD_EXTENSION, SolrFieldName.cdate+STORED_FIELD_EXTENSION, SolrFieldName.udate+STORED_FIELD_EXTENSION,
                SolrFieldName.checksumA+STORED_FIELD_EXTENSION, SolrFieldName.checksumB+STORED_FIELD_EXTENSION, SolrFieldName.checksumI+STORED_FIELD_EXTENSION,
                SolrFieldName.negative+STORED_FIELD_EXTENSION
        };

        String[] DATA_FIELDS_27 = new String[] {
                SolrFieldName.idA+STORED_FIELD_EXTENSION, SolrFieldName.idB+STORED_FIELD_EXTENSION, SolrFieldName.altidA+STORED_FIELD_EXTENSION,
                SolrFieldName.altidB+STORED_FIELD_EXTENSION, SolrFieldName.aliasA+STORED_FIELD_EXTENSION, SolrFieldName.aliasB+STORED_FIELD_EXTENSION,
                SolrFieldName.detmethod+STORED_FIELD_EXTENSION, SolrFieldName.pubauth+STORED_FIELD_EXTENSION, SolrFieldName.pubid+STORED_FIELD_EXTENSION,
                SolrFieldName.taxidA+STORED_FIELD_EXTENSION, SolrFieldName.taxidB+STORED_FIELD_EXTENSION, SolrFieldName.type+STORED_FIELD_EXTENSION,
                SolrFieldName.source+STORED_FIELD_EXTENSION,SolrFieldName.interaction_id+STORED_FIELD_EXTENSION, SolrFieldName.confidence+STORED_FIELD_EXTENSION,
                SolrFieldName.complex+STORED_FIELD_EXTENSION, SolrFieldName.pbioroleA+STORED_FIELD_EXTENSION, SolrFieldName.pbioroleB+STORED_FIELD_EXTENSION,
                SolrFieldName.pexproleA+STORED_FIELD_EXTENSION, SolrFieldName.pexproleB+STORED_FIELD_EXTENSION, SolrFieldName.ptypeA+STORED_FIELD_EXTENSION,
                SolrFieldName.ptypeB+STORED_FIELD_EXTENSION, SolrFieldName.pxrefA+STORED_FIELD_EXTENSION, SolrFieldName.pxrefB+STORED_FIELD_EXTENSION,
                SolrFieldName.xref+STORED_FIELD_EXTENSION, SolrFieldName.pexproleA+STORED_FIELD_EXTENSION, SolrFieldName.annotA+STORED_FIELD_EXTENSION,
                SolrFieldName.annotB+STORED_FIELD_EXTENSION, SolrFieldName.annot+STORED_FIELD_EXTENSION, SolrFieldName.taxidHost+STORED_FIELD_EXTENSION,
                SolrFieldName.param+STORED_FIELD_EXTENSION, SolrFieldName.cdate+STORED_FIELD_EXTENSION, SolrFieldName.udate+STORED_FIELD_EXTENSION,
                SolrFieldName.checksumA+STORED_FIELD_EXTENSION, SolrFieldName.checksumB+STORED_FIELD_EXTENSION, SolrFieldName.checksumI+STORED_FIELD_EXTENSION,
                SolrFieldName.negative+STORED_FIELD_EXTENSION, SolrFieldName.ftypeA+STORED_FIELD_EXTENSION, SolrFieldName.ftypeB+STORED_FIELD_EXTENSION,
                SolrFieldName.stcA+STORED_FIELD_EXTENSION, SolrFieldName.stcB+STORED_FIELD_EXTENSION, SolrFieldName.pmethodA+STORED_FIELD_EXTENSION,
                SolrFieldName.pmethodB+STORED_FIELD_EXTENSION
        };

        solrFields.put(RETURN_TYPE_MITAB25, DATA_FIELDS_25);
        solrFields.put(RETURN_TYPE_MITAB26, DATA_FIELDS_26);
        solrFields.put(RETURN_TYPE_MITAB27, DATA_FIELDS_27);
        solrFields.put(RETURN_TYPE_XML25, DATA_FIELDS_27);
        solrFields.put(RETURN_TYPE_COUNT, new String[] {});
    }

    /**
     *
     * @param q : query entered by user
     * @param firstResult : first result
     * @param maxResults : max number of results
     * @param returnType : format of the results
     * @param queryFilter : any filter to add to the query
     * @return  the response
     * @throws PsicquicSolrException
     */
    public PsicquicSearchResults search(String q, Integer firstResult, Integer maxResults, String returnType, String queryFilter) throws PsicquicSolrException, SolrServerException {
        if (q == null) throw new NullPointerException("Null query");

        // format wildcard query
        if ("*".equals(q)){
            q = "*:*";
        }

        SolrQuery solrQuery = new SolrQuery(q);
        
        // use dismax parser for querying default fields
        //solrQuery.setParam(DISMAX_PARAM_NAME, SolrFieldName.identifier.toString(), SolrFieldName.pubid.toString(), SolrFieldName.pubauth.toString(), SolrFieldName.species.toString(), SolrFieldName.detmethod.toString(), SolrFieldName.type.toString(), SolrFieldName.interaction_id.toString());
        solrQuery.setParam(DEFAULT_PARAM_NAME, SolrFieldName.identifier.toString()+" "+SolrFieldName.pubid.toString()+" "+SolrFieldName.pubauth.toString()+" "+SolrFieldName.species.toString()+" "+SolrFieldName.detmethod.toString()+" "+SolrFieldName.type.toString()+" "+SolrFieldName.interaction_id.toString());
        solrQuery.setParam(QUERY_TYPE, DISMAX_TYPE);

        // set first result
        if (firstResult != null)
        {
            solrQuery.setStart(firstResult);
        }else {
            solrQuery.setStart(0);
        }

        // set max results
        if (maxResults != null) {
            solrQuery.setRows(maxResults);
        } else {
            solrQuery.setRows(Integer.MAX_VALUE);
        }

        // by default, if no return types is specified, it will return MITAB 2.5
        if (returnType == null){
             returnType = RETURN_TYPE_DEFAULT;
        }

        // apply any filter
        if (queryFilter != null && !queryFilter.isEmpty()) {
            if ("*".equals(queryFilter) || q.trim().isEmpty()) {
                q = queryFilter;
            } else {
                solrQuery.addFilterQuery(queryFilter);
            }
        }

        // use normal query so we can use dismax parser to define multiple default field names in case of free text searches. The filter query fq parameter does not work with
        // dismax parser
        solrQuery.setQuery(q);

        return search(solrQuery, returnType);
    }

    private PsicquicSearchResults search(SolrQuery originalQuery, String returnType) throws PsicquicSolrException, SolrServerException {

        String[] fields = solrFields.containsKey(returnType) ? solrFields.get(returnType) : new String[]{};

        if(originalQuery.getFields()!=null){
            // new array created here so the map solrFields is never changed
            fields = (String[]) ArrayUtils.add(fields, originalQuery.getFields().split(","));
        }

        originalQuery.setFields(fields);
        
        String query = originalQuery.getQuery();
        // if using a wildcard query we convert to lower case
        // as of http://mail-archives.apache.org/mod_mbox/lucene-solr-user/200903.mbox/%3CFD3AFB65-AEC1-40B2-A0A4-7E14A519AB05@ehatchersolutions.com%3E
        if (query.contains("*")) {
            String[] tokens = query.split(" ");

            StringBuilder sb = new StringBuilder(query.length());

            for (String token : tokens) {
                if (token.contains("*")) {
                    sb.append(token.toLowerCase());
                } else {
                    sb.append(token);
                }

                sb.append(" ");
            }

            query = sb.toString().trim();
        }

        // and, or and not should be upper case
        query = query.replaceAll(" and ", " AND ").replaceAll(" or ", " OR ").replaceAll(" not ", " NOT ").replaceAll("\\\"", "\"").replaceAll("\\\\","\\");
        originalQuery.setQuery(query);

        org.apache.solr.client.solrj.response.QueryResponse solrResponse = solrServer.query(originalQuery);

        if (solrResponse == null){
            return null;
        }
        return createSearchResults(solrResponse.getResults(), returnType);
    }

    protected PsicquicSearchResults createSearchResults(SolrDocumentList docList, String returnType) throws PsicquicSolrException {

        String resultType = returnType != null ? returnType : RETURN_TYPE_DEFAULT;
        PsicquicSearchResults results = createMitabResultsForType(docList, returnType);

        return results;
    }

    protected PsicquicSearchResults createMitabResultsForType(SolrDocumentList docList, String mitabType) throws PsicquicSolrException {

        String [] fieldNames = solrFields.get(mitabType);

        if (fieldNames == null){
            throw new PsicquicSolrException("The format " + mitabType + " is not a recognised MITAB format");
        }
        
        PsicquicSearchResults searchResults = new PsicquicSearchResults(docList, fieldNames);
        return searchResults;
    }
}
