/*
 * Copyright 2010 James Pether Sörling
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
package com.hack23.cia.service.impl.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.drools.core.common.DefaultFactHandle;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hack23.cia.model.internal.application.data.committee.impl.ViewRiksdagenVoteDataBallotPartySummaryAnnual;
import com.hack23.cia.model.internal.application.data.committee.impl.ViewRiksdagenVoteDataBallotPartySummaryDaily;
import com.hack23.cia.model.internal.application.data.committee.impl.ViewRiksdagenVoteDataBallotPartySummaryMonthly;
import com.hack23.cia.model.internal.application.data.committee.impl.ViewRiksdagenVoteDataBallotPoliticianSummaryAnnual;
import com.hack23.cia.model.internal.application.data.committee.impl.ViewRiksdagenVoteDataBallotPoliticianSummaryDaily;
import com.hack23.cia.model.internal.application.data.committee.impl.ViewRiksdagenVoteDataBallotPoliticianSummaryMonthly;
import com.hack23.cia.model.internal.application.data.party.impl.ViewRiksdagenPartySummary;
import com.hack23.cia.model.internal.application.data.politician.impl.ViewRiksdagenPolitician;
import com.hack23.cia.service.api.action.kpi.ComplianceCheck;
import com.hack23.cia.service.data.api.DataViewer;

/**
 * The Class RulesEngineImpl.
 */
@Service
@Transactional(propagation = Propagation.REQUIRED, timeout = 1200)
public final class RulesEngineImpl implements RulesEngine {

	/** The data viewer. */
	@Autowired
	@Qualifier("DataViewer")
	private DataViewer dataViewer;

	@Autowired
	private KieContainer rulesContainer;
	

	/**
	 * Instantiates a new rules engine impl.
	 */
	public RulesEngineImpl() {
		super();
	}

	@Override
	@Cacheable("checkRulesCompliance")
	public List<ComplianceCheck> checkRulesCompliance() {
		final KieSession ksession = rulesContainer.newKieSession();
		final Map<String, ComplianceCheck> complianceChecks = new HashMap<>();
		ksession.addEventListener(new ComplianceCheckAgendaEventListener(complianceChecks));

		insertPoliticians(ksession, dataViewer.getAll(ViewRiksdagenPolitician.class));
		insertParties(ksession, dataViewer.getAll(ViewRiksdagenPartySummary.class));

		ksession.fireAllRules();
		ksession.dispose();
		return new ArrayList<>(complianceChecks.values());
	}

	/**
	 * Insert politicians.
	 *
	 * @param ksession
	 *            the ksession
	 * @param list
	 *            the list
	 */
	private void insertPoliticians(final KieSession ksession, final List<ViewRiksdagenPolitician> list) {

		final Map<String, List<ViewRiksdagenVoteDataBallotPoliticianSummaryAnnual>> politicanBallotSummaryAnnualMap = dataViewer
				.getAll(ViewRiksdagenVoteDataBallotPoliticianSummaryAnnual.class).stream()
				.collect(Collectors.groupingBy(p -> p.getEmbeddedId().getIntressentId()));
		final Map<String, List<ViewRiksdagenVoteDataBallotPoliticianSummaryMonthly>> politicanBallotSummaryMontlyMap = dataViewer
				.getAll(ViewRiksdagenVoteDataBallotPoliticianSummaryMonthly.class).stream()
				.collect(Collectors.groupingBy(p -> p.getEmbeddedId().getIntressentId()));
		final Map<String, List<ViewRiksdagenVoteDataBallotPoliticianSummaryDaily>> politicanBallotSummaryDailyMap = dataViewer
				.getAll(ViewRiksdagenVoteDataBallotPoliticianSummaryDaily.class).stream()
				.collect(Collectors.groupingBy(p -> p.getEmbeddedId().getIntressentId()));

		for (final ViewRiksdagenPolitician politicianData : list) {
			if (politicianData != null) {

				final List<ViewRiksdagenVoteDataBallotPoliticianSummaryDaily> dailyList = politicanBallotSummaryDailyMap
						.get(politicianData.getPersonId());
				final List<ViewRiksdagenVoteDataBallotPoliticianSummaryMonthly> monthlyList = politicanBallotSummaryMontlyMap
						.get(politicianData.getPersonId());
				final List<ViewRiksdagenVoteDataBallotPoliticianSummaryAnnual> annualList = politicanBallotSummaryAnnualMap
						.get(politicianData.getPersonId());

				if (politicianData.isActiveParliament() && dailyList != null && monthlyList != null
						&& annualList != null) {
					Collections.sort(dailyList,
							(e1, e2) -> e1.getEmbeddedId().getVoteDate().compareTo(e2.getEmbeddedId().getVoteDate()));
					final Optional<ViewRiksdagenVoteDataBallotPoliticianSummaryDaily> dailyListFirst = dailyList.stream().findFirst();
					
					Collections.sort(monthlyList,
							(e1, e2) -> e1.getEmbeddedId().getVoteDate().compareTo(e2.getEmbeddedId().getVoteDate()));
					final Optional<ViewRiksdagenVoteDataBallotPoliticianSummaryMonthly> monthlyListFirst = monthlyList.stream().findFirst();
					Collections.sort(annualList,
							(e1, e2) -> e1.getEmbeddedId().getVoteDate().compareTo(e2.getEmbeddedId().getVoteDate()));
					final Optional<ViewRiksdagenVoteDataBallotPoliticianSummaryAnnual> annualListFirst = annualList.stream().findFirst();

					if (annualListFirst.isPresent() && monthlyListFirst.isPresent() && dailyListFirst.isPresent()) {
						final PoliticianComplianceCheckImpl politicianComplianceCheckImpl = new PoliticianComplianceCheckImpl(
								politicianData, dailyListFirst.get(), monthlyListFirst.get(),
								annualListFirst.get());
						ksession.insert(politicianComplianceCheckImpl);
					}
				} else {
					final PoliticianComplianceCheckImpl politicianComplianceCheckImpl = new PoliticianComplianceCheckImpl(
							politicianData, null, null, null);
					ksession.insert(politicianComplianceCheckImpl);
				}
			}
		}
	}

	/**
	 * Insert parties.
	 *
	 * @param ksession
	 *            the ksession
	 * @param list
	 *            the list
	 */
	private void insertParties(final KieSession ksession, final List<ViewRiksdagenPartySummary> list) {
		final Map<String, List<ViewRiksdagenVoteDataBallotPartySummaryDaily>> politicanBallotSummaryAnnualMap = dataViewer
				.getAll(ViewRiksdagenVoteDataBallotPartySummaryDaily.class).stream()
				.collect(Collectors.groupingBy(p -> p.getEmbeddedId().getParty()));
		final Map<String, List<ViewRiksdagenVoteDataBallotPartySummaryMonthly>> politicanBallotSummaryMontlyMap = dataViewer
				.getAll(ViewRiksdagenVoteDataBallotPartySummaryMonthly.class).stream()
				.collect(Collectors.groupingBy(p -> p.getEmbeddedId().getParty()));
		final Map<String, List<ViewRiksdagenVoteDataBallotPartySummaryAnnual>> politicanBallotSummaryDailyMap = dataViewer
				.getAll(ViewRiksdagenVoteDataBallotPartySummaryAnnual.class).stream()
				.collect(Collectors.groupingBy(p -> p.getEmbeddedId().getParty()));

		for (final ViewRiksdagenPartySummary partyData : list) {
			if (partyData != null) {				
				final List<ViewRiksdagenVoteDataBallotPartySummaryAnnual> dailyList = politicanBallotSummaryDailyMap
						.get(partyData.getParty());
				final List<ViewRiksdagenVoteDataBallotPartySummaryMonthly> monthlyList = politicanBallotSummaryMontlyMap
						.get(partyData.getParty());
				final List<ViewRiksdagenVoteDataBallotPartySummaryDaily> annualList = politicanBallotSummaryAnnualMap
						.get(partyData.getParty());

				if (partyData.isActiveParliament() && dailyList != null && monthlyList != null
						&& annualList != null) {
					Collections.sort(dailyList,
							(e1, e2) -> e1.getEmbeddedId().getVoteDate().compareTo(e2.getEmbeddedId().getVoteDate()));
					final Optional<ViewRiksdagenVoteDataBallotPartySummaryAnnual> dailyListFirst = dailyList.stream().findFirst();
					Collections.sort(monthlyList,
							(e1, e2) -> e1.getEmbeddedId().getVoteDate().compareTo(e2.getEmbeddedId().getVoteDate()));
					final Optional<ViewRiksdagenVoteDataBallotPartySummaryMonthly> monthlyListFirst = monthlyList.stream().findFirst();
					Collections.sort(annualList,
							(e1, e2) -> e1.getEmbeddedId().getVoteDate().compareTo(e2.getEmbeddedId().getVoteDate()));
					final Optional<ViewRiksdagenVoteDataBallotPartySummaryDaily> annualListFirst = annualList.stream().findFirst();

					if (annualListFirst.isPresent() && monthlyListFirst.isPresent() && dailyListFirst.isPresent()) {					
						final PartyComplianceCheckImpl politicianComplianceCheckImpl = new PartyComplianceCheckImpl(
								partyData, dailyListFirst.get(), monthlyListFirst.get(),
								annualListFirst.get());
						ksession.insert(politicianComplianceCheckImpl);
					}
				} else {
					final PartyComplianceCheckImpl politicianComplianceCheckImpl = new PartyComplianceCheckImpl(
							partyData, null, null, null);
					ksession.insert(politicianComplianceCheckImpl);
				}
			}
		}
	}

	/**
	 * The Class ComplianceCheckAgendaEventListener.
	 */
	private static final class ComplianceCheckAgendaEventListener extends DefaultAgendaEventListener {

		/** The compliance checks. */
		private final Map<String, ComplianceCheck> complianceChecks;

		/**
		 * Instantiates a new compliance check agenda event listener.
		 *
		 * @param complianceChecks
		 *            the compliance checks
		 */
		public ComplianceCheckAgendaEventListener(final Map<String, ComplianceCheck> complianceChecks) {
			this.complianceChecks = complianceChecks;
		}

		@Override
		public void afterMatchFired(final AfterMatchFiredEvent event) {
			super.afterMatchFired(event);
			final AbstractComplianceCheckImpl check = (AbstractComplianceCheckImpl) ((DefaultFactHandle) event.getMatch()
					.getFactHandles().stream().findFirst().orElse(null)).getObject();
			complianceChecks.put(check.getId(), check);
		}
	}
}
