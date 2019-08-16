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

package com.epam.ta.reportportal.ws.converter.converters;

import com.epam.ta.reportportal.commons.EntityUtils;
import com.epam.ta.reportportal.entity.enums.LogLevel;
import com.epam.ta.reportportal.entity.log.Log;
import com.epam.ta.reportportal.ws.model.log.LogResource;
import com.epam.ta.reportportal.ws.model.log.SearchLogRs;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Converts internal DB model to DTO
 *
 * @author Pavel Bortnik
 */
public final class LogConverter {

	private LogConverter() {
		//static only
	}

	public static final Function<Log, LogResource> TO_RESOURCE = model -> {

		Preconditions.checkNotNull(model);

		LogResource resource = new LogResource();
		resource.setId(model.getId());
		resource.setUuid(model.getUuid());
		resource.setMessage(ofNullable(model.getLogMessage()).orElse("NULL"));
		resource.setLogTime(EntityUtils.TO_DATE.apply(model.getLogTime()));

		if (isBinaryDataExists(model)) {

			LogResource.BinaryContent binaryContent = new LogResource.BinaryContent();

			binaryContent.setBinaryDataId(model.getAttachment().getFileId());
			binaryContent.setContentType(model.getAttachment().getContentType());
			binaryContent.setThumbnailId(model.getAttachment().getThumbnailId());
			resource.setBinaryContent(binaryContent);
		}

		ofNullable(model.getTestItem()).ifPresent(testItem -> resource.setItemId(testItem.getItemId()));
		ofNullable(model.getLaunch()).ifPresent(launch -> resource.setLaunchId(launch.getId()));
		ofNullable(model.getLogLevel()).ifPresent(level -> resource.setLevel(LogLevel.toLevel(level).toString()));
		return resource;
	};

	public static final BiFunction<Long, Log, SearchLogRs> TO_SEARCH_LOG_RS = (launchId, log) -> {
		SearchLogRs searchLogRs = new SearchLogRs();
		searchLogRs.setLaunchId(launchId);
		searchLogRs.setItemId(log.getTestItem().getItemId());
		searchLogRs.setItemName(log.getTestItem().getName());
		searchLogRs.setPath(log.getTestItem().getPath());
		searchLogRs.setPatternTemplates(log.getTestItem()
				.getPatternTemplateTestItems()
				.stream()
				.map(patternTemplateTestItem -> patternTemplateTestItem.getPatternTemplate().getName())
				.collect(toSet()));
		searchLogRs.setDuration(log.getTestItem().getItemResults().getDuration());
		searchLogRs.setStatus(log.getTestItem().getItemResults().getStatus().name());
		searchLogRs.setIssue(IssueConverter.TO_MODEL.apply(log.getTestItem().getItemResults().getIssue()));
		searchLogRs.setLogMessages(Arrays.asList(log.getLogMessage()));
		return searchLogRs;
	};

	private static boolean isBinaryDataExists(Log log) {
		return ofNullable(log.getAttachment()).map(a -> isNotEmpty(a.getContentType()) || isNotEmpty(a.getThumbnailId())
				|| isNotEmpty(a.getFileId())).orElse(false);
	}

}
