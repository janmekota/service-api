/*
 * Copyright 2018 EPAM Systems
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

package com.epam.ta.reportportal.core.item.impl;

import com.epam.ta.reportportal.auth.ReportPortalUser;
import com.epam.ta.reportportal.commons.querygen.Queryable;
import com.epam.ta.reportportal.core.item.GetTestItemHandler;
import com.epam.ta.reportportal.dao.ItemAttributeRepository;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.dao.TestItemRepository;
import com.epam.ta.reportportal.entity.item.TestItem;
import com.epam.ta.reportportal.entity.launch.Launch;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.converter.PagedResourcesAssembler;
import com.epam.ta.reportportal.ws.converter.TestItemResourceAssembler;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.TestItemResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.ta.reportportal.commons.Predicates.equalTo;
import static com.epam.ta.reportportal.commons.validation.BusinessRule.expect;
import static com.epam.ta.reportportal.commons.validation.Suppliers.formattedSupplier;
import static com.epam.ta.reportportal.ws.model.ErrorType.LAUNCH_NOT_FOUND;

/**
 * GET operations for {@link TestItem}<br>
 * Default implementation
 *
 * @author Andrei Varabyeu
 * @author Aliaksei Makayed
 */
@Service
class GetTestItemHandlerImpl implements GetTestItemHandler {

	private final LaunchRepository launchRepository;

	private final TestItemRepository testItemRepository;

	private final ItemAttributeRepository itemAttributeRepository;

	private final TestItemResourceAssembler itemResourceAssembler;

	@Autowired
	public GetTestItemHandlerImpl(LaunchRepository launchRepository, TestItemRepository testItemRepository,
			ItemAttributeRepository itemAttributeRepository, TestItemResourceAssembler itemResourceAssembler) {
		this.launchRepository = launchRepository;
		this.testItemRepository = testItemRepository;
		this.itemAttributeRepository = itemAttributeRepository;
		this.itemResourceAssembler = itemResourceAssembler;
	}

	@Override
	public TestItemResource getTestItem(Long testItemId, ReportPortalUser.ProjectDetails projectDetails, ReportPortalUser user) {
		TestItem testItem = testItemRepository.findById(testItemId)
				.orElseThrow(() -> new ReportPortalException(ErrorType.TEST_ITEM_NOT_FOUND, testItemId));
		return itemResourceAssembler.toResource(testItem);
	}

	@Override
	public Iterable<TestItemResource> getTestItems(Queryable filter, Pageable pageable, ReportPortalUser.ProjectDetails projectDetails,
			ReportPortalUser user, Long launchId) {
		validate(launchId, projectDetails.getProjectId());
		Page<TestItem> testItemPage = testItemRepository.findByFilter(filter, pageable);
		return PagedResourcesAssembler.pageConverter(itemResourceAssembler::toResource).apply(testItemPage);
	}

	@Override
	public List<String> getAttributeKeys(Long launchId, String value) {
		return itemAttributeRepository.findTestItemAttributeKeys(launchId, value, false);
	}

	@Override
	public List<String> getAttributeValues(Long launchId, String key, String value) {
		return itemAttributeRepository.findTestItemAttributeValues(launchId, key, value, false);
	}

	@Override
	public List<TestItemResource> getTestItems(Long[] ids, ReportPortalUser.ProjectDetails projectDetails, ReportPortalUser user) {
		List<TestItem> testItems = testItemRepository.findAllById(Arrays.asList(ids));
		return testItems.stream().map(itemResourceAssembler::toResource).collect(Collectors.toList());
	}

	private void validate(Long launchId, Long projectId) {
		Launch launch = launchRepository.findById(launchId).orElseThrow(() -> new ReportPortalException(LAUNCH_NOT_FOUND, launchId));
		expect(launch.getProjectId(), equalTo(projectId)).verify(ErrorType.FORBIDDEN_OPERATION,
				formattedSupplier("Specified launch with id '{}' not referenced to specified project with id '{}'",
						launch.getId(),
						projectId
				)
		);
	}
}
