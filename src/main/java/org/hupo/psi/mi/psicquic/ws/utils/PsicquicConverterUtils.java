package org.hupo.psi.mi.psicquic.ws.utils;

import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import psidev.psi.mi.tab.PsimiTabException;
import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.converter.tab2xml.Tab2Xml;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.xml.converter.ConverterContext;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;
import psidev.psi.mi.xml254.jaxb.Attribute;
import psidev.psi.mi.xml254.jaxb.AttributeList;
import psidev.psi.mi.xml254.jaxb.Entry;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Utility class to convert formats of psicquic
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/07/12</pre>
 */

public class PsicquicConverterUtils {

    public static EntrySet extractJaxbEntrySetFromPsicquicResults(PsicquicSearchResults psicquicSearchResults, String query, int blockSize, int maxSize) throws PsimiTabException, IOException, XmlConversionException, IllegalAccessException, ConverterException {
        // get the entryset from the results and converts to jaxb
        psidev.psi.mi.xml.model.EntrySet psiEntrySet = psicquicSearchResults.createEntrySet();
        EntrySetConverter entryConverter = new EntrySetConverter();
        entryConverter.setDAOFactory(new InMemoryDAOFactory());

        EntrySet jEntrySet = entryConverter.toJaxb(psiEntrySet);

        // add some annotations
        if (!jEntrySet.getEntries().isEmpty()) {
            AttributeList attrList = new AttributeList();

            Entry entry = jEntrySet.getEntries().iterator().next();

            Attribute attr = new Attribute();
            attr.setValue("Data retrieved using the PSICQUIC service. Query: "+query);
            attr.setName("comment");
            attr.setNameAc("MI:0612");
            attrList.getAttributes().add(attr);

            Attribute attr2 = new Attribute();
            attr2.setValue("Total results found: "+psicquicSearchResults.getNumberResults());
            attr2.setName("comment");
            attr2.setNameAc("MI:0612");
            attrList.getAttributes().add(attr2);

            // add warning if the batch size requested is higher than the maximum allowed
            if (blockSize > maxSize && maxSize < psicquicSearchResults.getNumberResults()) {
                Attribute attrWarning = new Attribute();
                attr.setName("caution");
                attr.setNameAc("MI:0618");
                attrWarning.setValue("Warning: The requested block size (" + blockSize + ") was higher than the maximum allowed (" + maxSize + ") by PSICQUIC the service. " +
                        maxSize + " results were returned from a total found of "+psicquicSearchResults.getNumberResults());
                attrList.getAttributes().add(attrWarning);

            }

            entry.setAttributeList(attrList);
        }

        // close threadlocal if not done yet
        ConverterContext.remove();

        return jEntrySet;
    }

    public static EntrySet createEntrySetFromInputStream(String query, long numberResults, StringWriter mitabWriter) throws PsimiTabException, IOException, XmlConversionException, IllegalAccessException, ConverterException{
        psidev.psi.mi.xml.model.EntrySet psiEntrySet;
        PsimiTabReader mitabReader = new PsimiTabReader();

        if (numberResults == 0 || mitabWriter == null) {
            psiEntrySet = new psidev.psi.mi.xml.model.EntrySet();
        }
        else {
            Tab2Xml tab2Xml = new Tab2Xml();
            psiEntrySet = tab2Xml.convert(mitabReader.read(mitabWriter.toString()));
        }

        EntrySetConverter entryConverter = new EntrySetConverter();
        entryConverter.setDAOFactory(new InMemoryDAOFactory());

        EntrySet jEntrySet = entryConverter.toJaxb(psiEntrySet);

        // add some annotations
        if (!jEntrySet.getEntries().isEmpty()) {
            AttributeList attrList = new AttributeList();

            Entry entry = jEntrySet.getEntries().iterator().next();

            Attribute attr = new Attribute();
            attr.setValue("Data retrieved using the PSICQUIC service. Query: "+query);
            attr.setName("comment");
            attr.setNameAc("MI:0612");
            attrList.getAttributes().add(attr);

            Attribute attr2 = new Attribute();
            attr2.setValue("Total results found: "+numberResults);
            attr2.setName("comment");
            attr2.setNameAc("MI:0612");
            attrList.getAttributes().add(attr2);

            entry.setAttributeList(attrList);
        }

        // close threadlocal if not done yet
        ConverterContext.remove();

        return jEntrySet;
    }

}
