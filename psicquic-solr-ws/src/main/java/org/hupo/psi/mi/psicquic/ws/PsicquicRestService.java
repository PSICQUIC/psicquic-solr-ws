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

import org.hupo.psi.mi.psicquic.NotSupportedMethodException;
import org.hupo.psi.mi.psicquic.NotSupportedTypeException;
import org.hupo.psi.mi.psicquic.PsicquicServiceException;

import javax.ws.rs.*;

/**
 * RESTful web service.
 *
 * v.1.0/search/query/brca2
 * current/search/query/species:human?format=xml25&firstResult=50&maxResults=100
 *
 *
 * @author MarineDumousseau (marine@ebi.ac.uk)
 * @version $Id: IntactPsicquicService.java 12873 2009-03-18 02:51:31Z baranda $
 */
@Path("/search")
public interface PsicquicRestService {

    @GET
    @Path("/interactor/{interactorAc}")
    Object getByInteractor(@PathParam("interactorAc") String interactorAc,
                           @DefaultValue("") @QueryParam("db") String db,
                           @DefaultValue("tab25") @QueryParam("format") String format,
                           @DefaultValue("0") @QueryParam("firstResult") String firstResult,
                           @DefaultValue("2147483647") @QueryParam("maxResults") String maxResults) throws PsicquicServiceException,
            NotSupportedMethodException,
            NotSupportedTypeException;

    @GET
    @Path("/interaction/{interactionAc}")
    Object getByInteraction(@PathParam("interactionAc") String interactionAc,
                            @DefaultValue("") @QueryParam("db") String db,
                            @DefaultValue("tab25") @QueryParam("format") String format,
                            @DefaultValue("0") @QueryParam("firstResult") String firstResult,
                            @DefaultValue("2147483647") @QueryParam("maxResults") String maxResults) throws PsicquicServiceException,
            NotSupportedMethodException,
            NotSupportedTypeException;

    @GET
    @Path("/query/{query}")
    Object getByQuery(@PathParam("query") String query,
                      @DefaultValue("tab25") @QueryParam("format") String format,
                      @DefaultValue("0") @QueryParam("firstResult") String firstResult,
                      @DefaultValue("2147483647") @QueryParam("maxResults") String maxResults) throws PsicquicServiceException,
            NotSupportedMethodException,
            NotSupportedTypeException;

    @GET
    @Path("/formats")
    Object getSupportedFormats() throws PsicquicServiceException,
            NotSupportedMethodException,
            NotSupportedTypeException;

    @GET
    @Path("/property/{propertyName}")
    Object getProperty(@PathParam("propertyName") String propertyName);

    @GET
    @Path("/properties")
    Object getProperties();

    @GET
    @Path("/version")
    String getVersion();

}
