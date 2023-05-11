/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.ta.reportportal.core.dashboard.impl;

import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.commons.querygen.Filter;
import com.epam.ta.reportportal.commons.querygen.ProjectFilter;
import com.epam.ta.reportportal.core.dashboard.GetDashboardHandler;
import com.epam.ta.reportportal.dao.DashboardRepository;
import com.epam.ta.reportportal.entity.dashboard.Dashboard;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.converter.PagedResourcesAssembler;
import com.epam.ta.reportportal.ws.converter.converters.DashboardConverter;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.dashboard.DashboardResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * @author Pavel Bortnik
 */
@Service
public class GetDashboardHandlerImpl implements GetDashboardHandler {

	private DashboardRepository dashboardRepository;

	@Autowired
	public void setDashboardRepository(DashboardRepository dashboardRepository) {
		this.dashboardRepository = dashboardRepository;
	}

	@Override
	public Iterable<DashboardResource> getDashboards(ReportPortalUser.ProjectDetails projectDetails, Pageable pageable, Filter filter,
			ReportPortalUser user) {
		final Page<Dashboard> dashboards = dashboardRepository.findByFilter(ProjectFilter.of(filter, projectDetails.getProjectId()),
				pageable
		);
		return PagedResourcesAssembler.pageConverter(DashboardConverter.TO_RESOURCE).apply(dashboards);
	}

	@Override
	public DashboardResource getDashboard(Long id, ReportPortalUser.ProjectDetails projectDetails) {
		final Dashboard dashboard = dashboardRepository.findByIdAndProjectId(id, projectDetails.getProjectId())
				.orElseThrow(() -> new ReportPortalException(ErrorType.DASHBOARD_NOT_FOUND_IN_PROJECT,
						id,
						projectDetails.getProjectName()
				));
		return DashboardConverter.TO_RESOURCE.apply(dashboard);
	}
}