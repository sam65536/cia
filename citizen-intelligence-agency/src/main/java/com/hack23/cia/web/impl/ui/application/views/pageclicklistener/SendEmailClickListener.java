/*
 * Copyright 2014 James Pether Sörling
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *	$Id$
 *  $HeadURL$
*/
package com.hack23.cia.web.impl.ui.application.views.pageclicklistener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hack23.cia.service.api.ApplicationManager;
import com.hack23.cia.service.api.action.admin.SendEmailRequest;
import com.hack23.cia.service.api.action.common.ServiceResponse;
import com.hack23.cia.service.api.action.common.ServiceResponse.ServiceResult;
import com.hack23.cia.web.impl.ui.application.views.common.viewnames.UserViews;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

/**
 * The listener interface for receiving sendEmailClick events. The class that is
 * interested in processing a sendEmailClick event implements this interface,
 * and the object created with that class is registered with a component using
 * the component's <code>addSendEmailClickListener<code> method. When the
 * sendEmailClick event occurs, that object's appropriate method is invoked.
 *
 * @see SendEmailClickEvent
 */
public final class SendEmailClickListener implements ClickListener {

	/** The Constant SEND_EMAIL_FAILURE. */
	private static final String SEND_EMAIL_FAILURE = "SendEmail {} failure";

	/** The Constant ERROR_MESSAGE. */
	private static final String ERROR_MESSAGE = "Error message";

	/** The Constant SEND_EMAIL_FAILEDFAILED. */
	private static final String SEND_EMAIL_FAILEDFAILED = "Send email failed";

	/** The Constant LOG_MSG_SEND_EMAIL. */
	private static final String LOG_MSG_SEND_EMAIL = "SendEmail {}";

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(SendEmailClickListener.class);

	/** The reqister request. */
	private final SendEmailRequest sendEmailRequest;

	/** The application manager. */
	private final transient ApplicationManager applicationManager;

	/**
	 * Instantiates a new register user click listener.
	 *
	 * @param sendEmailRequest
	 *            the reqister request
	 * @param applicationManager
	 *            the application manager
	 */
	public SendEmailClickListener(final SendEmailRequest sendEmailRequest, final ApplicationManager applicationManager) {
		this.sendEmailRequest = sendEmailRequest;
		this.applicationManager = applicationManager;
	}

	@Override
	public void buttonClick(final ClickEvent event) {
		final ServiceResponse response = applicationManager.service(sendEmailRequest);
		if (ServiceResult.SUCCESS == response.getResult()) {
			LOGGER.info(LOG_MSG_SEND_EMAIL,sendEmailRequest.getEmail());
			UI.getCurrent().getNavigator().navigateTo(UserViews.USERHOME_VIEW_NAME);
		} else {
			Notification.show(SEND_EMAIL_FAILEDFAILED,
	                  ERROR_MESSAGE,
	                  Notification.Type.WARNING_MESSAGE);
			LOGGER.info(SEND_EMAIL_FAILURE,sendEmailRequest.getEmail());
		}
	}
}