package org.hupo.psi.mi.psicquic.ws.model;

import com.google.common.primitives.Ints;
import org.apache.commons.lang.ArrayUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.hupo.psi.mi.psicquic.*;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import psidev.psi.mi.calimocho.solr.converter.SolrFieldName;
import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.converter.tab2xml.Tab2Xml;
import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;
import psidev.psi.mi.xml254.jaxb.Attribute;
import psidev.psi.mi.xml254.jaxb.AttributeList;
import psidev.psi.mi.xml254.jaxb.Entry;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import java.util.*;

/**
 * Psicquic solr server that is wrapping a solrServer
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>12/06/12</pre>
 */

public class PsicquicSolrServer {

    private final Logger logger = LoggerFactory.getLogger(SolrBasedPsicquicService.class);

    // static final variables
    private final static String STORED_FIELD_EXTENSION="_o";
    private static final String RETURN_TYPE_XML25 = "psi-mi/xml25";
    private static final String RETURN_TYPE_MITAB25 = "psi-mi/tab25";
    private static final String RETURN_TYPE_MITAB26 = "psi-mi/tab26";
    private static final String RETURN_TYPE_MITAB27 = "psi-mi/tab27";
    private static final String RETURN_TYPE_COUNT = "count";
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String COLUMN_SEPARATOR = "\t";
    private static final String FIELD_SEPARATOR = "|";
    private static final String FIELD_EMPTY = "-";
    private static final String RETURN_TYPE_DEFAULT = RETURN_TYPE_MITAB25;
    private final static String DISMAX_PARAM_NAME = "qf";
    private final static String DISMAX_TYPE = "edismax";

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
        // TODO when we have a proper converter tab27-xml, we should return tab27
        solrFields.put(RETURN_TYPE_XML25, DATA_FIELDS_25);
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
     * @throws PsicquicServiceException
     */
    public QueryResponse search(String q, Integer firstResult, Integer maxResults, String returnType, String queryFilter) throws PsicquicServiceException {
        if (q == null) throw new NullPointerException("Null query");

        // format wildcard query
        if ("*".equals(q)){
            q = "*:*";
        }

        SolrQuery solrQuery = new SolrQuery(q);
        
        // use dismax parser for querying default fields
        solrQuery.setParam(DISMAX_PARAM_NAME, SolrFieldName.identifier.toString(), SolrFieldName.pubid.toString(), SolrFieldName.pubauth.toString(), SolrFieldName.species.toString(), SolrFieldName.detmethod.toString(), SolrFieldName.type.toString(), SolrFieldName.interaction_id.toString());
        solrQuery.setQueryType(DISMAX_TYPE);

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
                q+=" AND "+queryFilter;
                q = q.trim();
            }
        }

        // use normal query so we can use dismax parser to define multiple default field names in case of free text searches. The filter query fq parameter does not work with
        // dismax parser
        solrQuery.setQuery(q);

        return search(solrQuery, returnType);
    }

    private QueryResponse search(SolrQuery originalQuery, String returnType) throws PsicquicServiceException {

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

        QueryResponse queryResponse = null;

        try {
            queryResponse = executeQuery(originalQuery, returnType);
        } catch (SolrServerException e) {
            throw new PsicquicServiceException("Problem executing the query", e);
        } catch (NotSupportedTypeException e) {
            throw new PsicquicServiceException("Problem executing the query", e);
        }
        return queryResponse;
    }

    private QueryResponse executeQuery(SolrQuery query, String returnType) throws SolrServerException, PsicquicServiceException, NotSupportedTypeException {
        org.apache.solr.client.solrj.response.QueryResponse solrResponse = solrServer.query(query);

        if (solrResponse == null){
           return null;
        }

        SolrDocumentList docs = solrResponse.getResults();
                
        // preparing the response
        QueryResponse queryResponse = new QueryResponse();
        ResultInfo resultInfo = new ResultInfo();
        resultInfo.setBlockSize(query.getRows());
        resultInfo.setFirstResult(query.getStart());
        resultInfo.setResultType(returnType);
        // convert safely long to int using Google Guava's Ints
        try{
            resultInfo.setTotalResults(Ints.checkedCast(docs.getNumFound()));
        }
        catch (IllegalArgumentException e){
            throw new PsicquicServiceException("Impossible to retrieve the number of results from SOLR", e);
        }

        queryResponse.setResultInfo(resultInfo);

        ResultSet resultSet = createResultSet(query, docs, returnType, resultInfo.getTotalResults());
        queryResponse.setResultSet(resultSet);
        
        return queryResponse;
    }

    protected ResultSet createResultSet(SolrQuery query, SolrDocumentList docList, String returnType, int resultCount) throws PsicquicServiceException,
            NotSupportedTypeException {
        ResultSet resultSet = new ResultSet();

        String resultType = returnType != null ? returnType : RETURN_TYPE_DEFAULT;

        if (RETURN_TYPE_MITAB25.equals(resultType) || RETURN_TYPE_MITAB26.equals(resultType) || RETURN_TYPE_MITAB27.equals(resultType)) {
            if (logger.isDebugEnabled()) logger.debug("Creating PSI-MI TAB");
            String mitab = createMitabResults(docList, returnType, resultCount);

            resultSet.setMitab(mitab);
        } else if (RETURN_TYPE_XML25.equals(resultType)) {
            if (logger.isDebugEnabled()) logger.debug("Creating PSI-MI XML");
            String mitab = createMitabResults(docList, returnType, resultCount);

            EntrySet jEntrySet = createEntrySet(mitab);
            resultSet.setEntrySet(jEntrySet);

            // add some annotations
            if (!jEntrySet.getEntries().isEmpty()) {
                AttributeList attrList = new AttributeList();

                Entry entry = jEntrySet.getEntries().iterator().next();

                Attribute attr = new Attribute();
                attr.setValue("Data retrieved using the PSICQUIC service. Query: "+query.getQuery());
                attrList.getAttributes().add(attr);

                Attribute attr2 = new Attribute();
                attr2.setValue("Total results found: "+resultCount);
                attrList.getAttributes().add(attr2);

                // add warning if the batch size requested is higher than the maximum allowed
                if (query.getRows() > SolrBasedPsicquicService.BLOCKSIZE_MAX && SolrBasedPsicquicService.BLOCKSIZE_MAX < resultCount) {
                    Attribute attrWarning = new Attribute();
                    attrWarning.setValue("Warning: The requested block size (" + query.getRows() + ") was higher than the maximum allowed (" + SolrBasedPsicquicService.BLOCKSIZE_MAX + ") by PSICQUIC the service. " +
                            SolrBasedPsicquicService.BLOCKSIZE_MAX + " results were returned from a total found of "+resultCount);
                    attrList.getAttributes().add(attrWarning);

                }

                entry.setAttributeList(attrList);
            }

        } else if (RETURN_TYPE_COUNT.equals(resultType)) {
            if (logger.isDebugEnabled()) logger.debug("Count query");
            // nothing to be done here
        } else {
            throw new NotSupportedTypeException("Not supported return type: "+resultType+" - Supported types are: "+SolrBasedPsicquicService.SUPPORTED_SOAP_RETURN_TYPES);
        }

        return resultSet;
    }

    protected String createMitabResults(SolrDocumentList docList, String mitabType, int resultCount) throws PsicquicServiceException {

        String [] fieldNames = solrFields.get(mitabType);

        if (fieldNames == null){
            throw new PsicquicServiceException("The format " + mitabType + " is not a recognised MITAB format");
        }
        
        StringBuilder sb = new StringBuilder(resultCount * 512);

        Iterator<SolrDocument> iter = docList.iterator();
        while (iter.hasNext()) {
            SolrDocument doc = iter.next();

            int size = fieldNames.length;
            int index = 0;
            // one field name is one column
            for (String fieldName : fieldNames){
                // only one value is expected because it should not be multivalued
                Collection<Object> fieldValues = doc.getFieldValues(fieldName);
                
                if (fieldValues == null || fieldValues.isEmpty()){
                    sb.append(FIELD_EMPTY);
                }
                else {
                    Iterator<Object> valueIterator = fieldValues.iterator();
                    while (valueIterator.hasNext()){
                        sb.append(String.valueOf(valueIterator.next()));
                        
                        if (valueIterator.hasNext()){
                            sb.append(FIELD_SEPARATOR);
                        }
                    }
                }

                if (index < size){
                    sb.append(COLUMN_SEPARATOR);
                }
                index++;
            }
            
            if (iter.hasNext()){
                sb.append(NEW_LINE);
            }
        }
        return sb.toString();
    }

    private EntrySet createEntrySet(String mitab) throws PsicquicServiceException {
        if (mitab == null) {
            return new EntrySet();
        }

        Tab2Xml tab2Xml = new Tab2Xml();
        try {
            psidev.psi.mi.xml.model.EntrySet mEntrySet = tab2Xml.convert(mitabReader.read(mitab));

            EntrySetConverter converter = new EntrySetConverter();
            converter.setDAOFactory(new InMemoryDAOFactory());

            return converter.toJaxb(mEntrySet);

        } catch (Exception e) {
            throw new PsicquicServiceException("Problem converting results to PSI-MI XML", e);
        }
    }
}
