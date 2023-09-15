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
package com.epam.ta.reportportal.core.events.activity;

import static com.epam.ta.reportportal.core.events.activity.util.ActivityDetailsUtil.processDescription;
import static com.epam.ta.reportportal.core.events.activity.util.ActivityDetailsUtil.processName;

import com.epam.ta.reportportal.builder.ActivityBuilder;
import com.epam.ta.reportportal.core.events.ActivityEvent;
import com.epam.ta.reportportal.entity.activity.Activity;
import com.epam.ta.reportportal.entity.activity.ActivityAction;
import com.epam.ta.reportportal.entity.activity.EventAction;
import com.epam.ta.reportportal.entity.activity.EventObject;
import com.epam.ta.reportportal.entity.activity.EventPriority;
import com.epam.ta.reportportal.entity.activity.EventSubject;
import com.epam.ta.reportportal.ws.model.activity.DashboardActivityResource;

/**
 * @author Andrei Varabyeu
 */
public class DashboardUpdatedEvent extends AroundEvent<DashboardActivityResource> implements
    ActivityEvent {

  public DashboardUpdatedEvent() {
  }

  public DashboardUpdatedEvent(DashboardActivityResource before, DashboardActivityResource after,
      Long userId, String userLogin) {
    super(userId, userLogin, before, after);
  }

  @Override
  public Activity toActivity() {
    return new ActivityBuilder()
        .addCreatedNow()
        .addAction(EventAction.UPDATE)
        .addEventName(ActivityAction.UPDATE_DASHBOARD.getValue())
        .addPriority(EventPriority.LOW)
        .addObjectId(getAfter().getId())
        .addObjectName(getAfter().getName())
        .addObjectType(EventObject.DASHBOARD)
        .addProjectId(getAfter().getProjectId())
        .addSubjectId(getUserId())
        .addSubjectName(getUserLogin())
        .addSubjectType(EventSubject.USER)
        .addHistoryField(processName(getBefore().getName(), getAfter().getName()))
        .addHistoryField(
            processDescription(getBefore().getDescription(), getAfter().getDescription()))
        .get();
  }
}
