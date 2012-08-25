/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.milton.http.report;

import io.milton.http.ExistingEntityHandler;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.resource.Resource;
import io.milton.http.ResourceHandlerHelper;
import io.milton.http.Response;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.webdav.WebDavResponseHandler;
import io.milton.common.ReadingException;
import io.milton.common.WritingException;
import io.milton.resource.ReportableResource;
import io.milton.http.entity.ByteArrayEntity;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alex
 */
public class ReportHandler implements ExistingEntityHandler {

	private Logger log = LoggerFactory.getLogger(ReportHandler.class);
	private final WebDavResponseHandler responseHandler;
	private final ResourceHandlerHelper resourceHandlerHelper;
	private final Map<String, Report> reports;

	public ReportHandler(WebDavResponseHandler responseHandler, ResourceHandlerHelper resourceHandlerHelper, Map<String, Report> reports) {
		this.responseHandler = responseHandler;
		this.resourceHandlerHelper = resourceHandlerHelper;
		this.reports = reports;
	}

	@Override
	public String[] getMethods() {
		return new String[]{Method.REPORT.code};
	}

	@Override
	public void process(HttpManager httpManager, Request request, Response response) throws ConflictException, NotAuthorizedException, BadRequestException {
		resourceHandlerHelper.process(httpManager, request, response, this);
	}

	@Override
	public void processResource(HttpManager manager, Request request, Response response, Resource r) throws NotAuthorizedException, ConflictException, BadRequestException {
		resourceHandlerHelper.processResource(manager, request, response, r, this);
	}

	@Override
	public void processExistingResource(HttpManager manager, Request request, Response response, Resource resource) throws NotAuthorizedException, BadRequestException, ConflictException {
		try {
			org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
			org.jdom.Document doc = builder.build(request.getInputStream());
			String reportName = doc.getRootElement().getName();
			Report r = reports.get(reportName);
			if (r == null) {
				log.error("report not known: " + reportName);
				throw new BadRequestException(resource);
			} else {
				log.trace("process report: " + reportName + " with : " + r.getClass());
				String xml = r.process(request.getHostHeader(), request.getAbsolutePath(), resource, doc);
				response.setStatus(Response.Status.SC_MULTI_STATUS);
				response.setContentTypeHeader("text/xml");
				response.setEntity(new ByteArrayEntity(xml.getBytes("UTF-8")));
			}
		} catch (JDOMException ex) {
			java.util.logging.Logger.getLogger(ReportHandler.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ReadingException ex) {
			throw new RuntimeException(ex);
		} catch (WritingException ex) {
			throw new RuntimeException(ex);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public boolean isCompatible(Resource res) {
		return (res instanceof ReportableResource);
	}
}
