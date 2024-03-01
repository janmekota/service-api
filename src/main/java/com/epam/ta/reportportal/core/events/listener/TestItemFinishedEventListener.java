/*
 * Copyright 2023 EPAM Systems
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
package com.epam.ta.reportportal.core.events.listener;

import com.epam.ta.reportportal.core.events.activity.item.TestItemFinishedEvent;
import com.epam.ta.reportportal.core.events.subscriber.EventSubscriber;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author <a href="mailto:pavel_bortnik@epam.com">Pavel Bortnik</a>
 */
public class TestItemFinishedEventListener {

  private final List<EventSubscriber<TestItemFinishedEvent>> subscribers;

  public TestItemFinishedEventListener(List<EventSubscriber<TestItemFinishedEvent>> subscribers) {
    this.subscribers = subscribers;
  }

  @Async(value = "eventListenerExecutor")
  @TransactionalEventListener
  public void onApplicationEvent(TestItemFinishedEvent event) {
    subscribers.forEach(s -> s.handleEvent(event));
  }
}
