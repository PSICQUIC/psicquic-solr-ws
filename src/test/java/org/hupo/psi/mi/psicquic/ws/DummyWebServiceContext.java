/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hupo.psi.mi.psicquic.ws;

import org.w3c.dom.Element;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.security.Principal;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: DummyWebServiceContext.java 12551 2008-12-19 13:51:05Z baranda $
 */
public class DummyWebServiceContext implements WebServiceContext {

    public MessageContext getMessageContext() {
        return null;
    }

    public Principal getUserPrincipal() {
        return null;
    }

    public boolean isUserInRole(String role) {
        return false;
    }

    public EndpointReference getEndpointReference(Element... referenceParameters) {
        return null;
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz, Element... referenceParameters) {
        return null;
    }
}
