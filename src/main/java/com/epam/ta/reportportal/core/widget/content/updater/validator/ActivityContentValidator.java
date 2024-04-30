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

package com.epam.ta.reportportal.core.widget.content.updater.validator;

import static com.epam.ta.reportportal.commons.Predicates.equalTo;
import static com.epam.reportportal.rules.commons.validation.BusinessRule.expect;

import com.epam.ta.reportportal.commons.querygen.Filter;
import com.epam.ta.reportportal.entity.widget.WidgetOptions;
import com.epam.reportportal.rules.exception.ErrorType;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * @author Pavel Bortnik
 */
@Service
public class ActivityContentValidator implements WidgetValidatorStrategy {

  @Override
  public void validate(List<String> contentFields, Map<Filter, Sort> filterSortMapping,
      WidgetOptions widgetOptions, int limit) {
    validateFilterSortMapping(filterSortMapping);
  }

  /**
   * Mapping should not be empty
   *
   * @param filterSortMapping Map of ${@link Filter} for query building as key and ${@link Sort} as
   *                          value for each filter
   */
  private void validateFilterSortMapping(Map<Filter, Sort> filterSortMapping) {
    expect(MapUtils.isNotEmpty(filterSortMapping), equalTo(true)).verify(
        ErrorType.BAD_REQUEST_ERROR,
        "Filter-Sort mapping should not be empty"
    );
  }

}
