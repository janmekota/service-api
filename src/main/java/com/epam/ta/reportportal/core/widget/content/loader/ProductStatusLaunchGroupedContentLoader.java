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

package com.epam.ta.reportportal.core.widget.content.loader;

import static com.epam.ta.reportportal.core.widget.content.constant.ContentLoaderConstants.CUSTOM_COLUMNS;
import static com.epam.ta.reportportal.core.widget.content.constant.ContentLoaderConstants.LATEST_OPTION;
import static com.epam.ta.reportportal.core.widget.content.constant.ContentLoaderConstants.RESULT;
import static com.epam.ta.reportportal.core.widget.util.WidgetFilterUtil.GROUP_FILTERS;
import static com.epam.ta.reportportal.core.widget.util.WidgetFilterUtil.GROUP_SORTS;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import com.epam.ta.reportportal.commons.querygen.Filter;
import com.epam.ta.reportportal.core.widget.util.WidgetOptionUtil;
import com.epam.ta.reportportal.dao.WidgetContentRepository;
import com.epam.ta.reportportal.entity.widget.WidgetOptions;
import com.epam.ta.reportportal.entity.widget.content.ProductStatusStatisticsContent;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class ProductStatusLaunchGroupedContentLoader implements ProductStatusContentLoader {

  @Autowired
  private WidgetContentRepository widgetContentRepository;

  @Override
  public Map<String, ?> loadContent(List<String> contentFields, Map<Filter, Sort> filterSortMapping,
      WidgetOptions widgetOptions,
      int limit) {

    Map<String, String> attributeColumns = WidgetOptionUtil.getMapByKey(CUSTOM_COLUMNS,
        widgetOptions);

    Filter filter = GROUP_FILTERS.apply(filterSortMapping.keySet());
    Sort sort = GROUP_SORTS.apply(filterSortMapping.values());

    boolean latestMode = WidgetOptionUtil.getBooleanByKey(LATEST_OPTION, widgetOptions);

    final List<ProductStatusStatisticsContent> content = widgetContentRepository.productStatusGroupedByLaunchesStatistics(
        filter,
        contentFields,
        attributeColumns,
        sort,
        latestMode,
        limit
    );

    return content.isEmpty() ? emptyMap() : singletonMap(RESULT, content);
  }

}
