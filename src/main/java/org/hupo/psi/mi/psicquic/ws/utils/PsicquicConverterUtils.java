package org.hupo.psi.mi.psicquic.ws.utils;

import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import psidev.psi.mi.tab.PsimiTabException;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;
import psidev.psi.mi.xml254.jaxb.Attribute;
import psidev.psi.mi.xml254.jaxb.AttributeList;
import psidev.psi.mi.xml254.jaxb.Entry;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import java.io.IOException;

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
            attrList.getAttributes().add(attr);

            Attribute attr2 = new Attribute();
            attr2.setValue("Total results found: "+psicquicSearchResults.getNumberResults());
            attrList.getAttributes().add(attr2);

            entry.setAttributeList(attrList);
        }

        return jEntrySet;
    }

    public static EntrySet extractJaxbEntrySetFromPsicquicResults(PsicquicSearchResults psicquicSearchResults, String query, int blockSize) throws ConverterException, IOException, XmlConversionException, IllegalAccessException, PsimiTabException {
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
            attrList.getAttributes().add(attr);

            Attribute attr2 = new Attribute();
            attr2.setValue("Total results found: "+psicquicSearchResults.getNumberResults());
            attrList.getAttributes().add(attr2);

            entry.setAttributeList(attrList);
        }

        return jEntrySet;
    }
}
