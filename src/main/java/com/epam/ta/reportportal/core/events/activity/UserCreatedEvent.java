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

import com.epam.ta.reportportal.core.events.ActivityEvent;
import com.epam.ta.reportportal.entity.activity.Activity;
import com.epam.ta.reportportal.entity.activity.ActivityAction;
import com.epam.ta.reportportal.entity.activity.EventAction;
import com.epam.ta.reportportal.entity.activity.EventObject;
import com.epam.ta.reportportal.entity.activity.EventPriority;
import com.epam.ta.reportportal.entity.activity.EventSubject;
import com.epam.ta.reportportal.ws.converter.builders.ActivityBuilder;
import com.epam.ta.reportportal.ws.model.activity.UserActivityResource;

/**
 * @author Andrei Varabyeu
 */
public class UserCreatedEvent extends AbstractEvent implements ActivityEvent {

  private UserActivityResource userActivityResource;

  public UserCreatedEvent() {
  }

  public UserCreatedEvent(UserActivityResource userActivityResource, Long userId,
      String userLogin) {
    super(userId, userLogin);
    this.userActivityResource = userActivityResource;
  }

  public UserActivityResource getUserActivityResource() {
    return userActivityResource;
  }

  public void setUserActivityResource(UserActivityResource userActivityResource) {
    this.userActivityResource = userActivityResource;
  }

  @Override
  public Activity toActivity() {
    return new ActivityBuilder()
        .addCreatedNow()
        .addAction(EventAction.CREATE)
        .addEventName(ActivityAction.CREATE_USER.getValue())
        .addPriority(EventPriority.HIGH)
        .addObjectId(userActivityResource.getId())
        .addObjectName(userActivityResource.getFullName())
        .addObjectType(EventObject.USER)
        .addSubjectId(getUserId())
        .addSubjectName(getUserLogin())
        .addSubjectType(EventSubject.USER)
        .get();
  }
}
