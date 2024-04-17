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

package com.epam.ta.reportportal.core.project.impl;

import static com.epam.ta.reportportal.commons.EntityUtils.normalizeId;
import static com.epam.ta.reportportal.commons.Preconditions.contains;
import static com.epam.ta.reportportal.commons.Predicates.equalTo;
import static com.epam.ta.reportportal.commons.Predicates.in;
import static com.epam.ta.reportportal.commons.Predicates.isNull;
import static com.epam.ta.reportportal.commons.Predicates.isPresent;
import static com.epam.ta.reportportal.commons.Predicates.not;
import static com.epam.ta.reportportal.commons.Predicates.notNull;
import static com.epam.reportportal.rules.commons.validation.BusinessRule.expect;
import static com.epam.reportportal.rules.commons.validation.BusinessRule.fail;
import static com.epam.reportportal.rules.commons.validation.Suppliers.formattedSupplier;
import static com.epam.ta.reportportal.core.analyzer.auto.impl.AnalyzerStatusCache.AUTO_ANALYZER_KEY;
import static com.epam.ta.reportportal.entity.enums.ProjectAttributeEnum.AUTO_PATTERN_ANALYZER_ENABLED;
import static com.epam.ta.reportportal.entity.enums.SendCase.findByName;
import static com.epam.ta.reportportal.ws.converter.converters.ProjectActivityConverter.TO_ACTIVITY_RESOURCE;
import static com.epam.reportportal.rules.exception.ErrorType.ACCESS_DENIED;
import static com.epam.reportportal.rules.exception.ErrorType.BAD_REQUEST_ERROR;
import static com.epam.reportportal.rules.exception.ErrorType.PROJECT_NOT_FOUND;
import static com.epam.reportportal.rules.exception.ErrorType.ROLE_NOT_FOUND;
import static com.epam.reportportal.rules.exception.ErrorType.UNABLE_ASSIGN_UNASSIGN_USER_TO_PROJECT;
import static com.epam.reportportal.rules.exception.ErrorType.USER_NOT_FOUND;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.epam.reportportal.extension.event.ProjectEvent;
import com.epam.ta.reportportal.commons.Preconditions;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.core.analyzer.auto.LogIndexer;
import com.epam.ta.reportportal.core.analyzer.auto.client.AnalyzerServiceClient;
import com.epam.ta.reportportal.core.analyzer.auto.impl.AnalyzerStatusCache;
import com.epam.ta.reportportal.core.analyzer.auto.impl.AnalyzerUtils;
import com.epam.ta.reportportal.core.analyzer.auto.indexer.IndexerStatusCache;
import com.epam.ta.reportportal.core.events.MessageBus;
import com.epam.ta.reportportal.core.events.activity.AssignUserEvent;
import com.epam.ta.reportportal.core.events.activity.ChangeRoleEvent;
import com.epam.ta.reportportal.core.events.activity.NotificationsConfigUpdatedEvent;
import com.epam.ta.reportportal.core.events.activity.ProjectAnalyzerConfigEvent;
import com.epam.ta.reportportal.core.events.activity.ProjectIndexEvent;
import com.epam.ta.reportportal.core.events.activity.ProjectPatternAnalyzerUpdateEvent;
import com.epam.ta.reportportal.core.events.activity.ProjectUpdatedEvent;
import com.epam.ta.reportportal.core.events.activity.UnassignUserEvent;
import com.epam.ta.reportportal.core.events.activity.util.ActivityDetailsUtil;
import com.epam.ta.reportportal.core.project.UpdateProjectHandler;
import com.epam.ta.reportportal.core.project.validator.attribute.ProjectAttributeValidator;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.ProjectUserRepository;
import com.epam.ta.reportportal.dao.UserPreferenceRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.enums.ProjectAttributeEnum;
import com.epam.ta.reportportal.entity.enums.ProjectAttributeEnum.Prefix;
import com.epam.ta.reportportal.entity.enums.ProjectType;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.project.ProjectRole;
import com.epam.ta.reportportal.entity.project.ProjectUtils;
import com.epam.ta.reportportal.entity.project.email.SenderCase;
import com.epam.ta.reportportal.entity.user.ProjectUser;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.entity.user.UserRole;
import com.epam.ta.reportportal.entity.user.UserType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.model.activity.ProjectAttributesActivityResource;
import com.epam.ta.reportportal.model.activity.UserActivityResource;
import com.epam.ta.reportportal.model.project.AssignUsersRQ;
import com.epam.ta.reportportal.model.project.ProjectResource;
import com.epam.ta.reportportal.model.project.UnassignUsersRQ;
import com.epam.ta.reportportal.model.project.UpdateProjectRQ;
import com.epam.ta.reportportal.model.project.config.ProjectConfigurationUpdate;
import com.epam.ta.reportportal.model.project.email.ProjectNotificationConfigDTO;
import com.epam.ta.reportportal.model.project.email.SenderCaseDTO;
import com.epam.ta.reportportal.util.ProjectExtractor;
import com.epam.ta.reportportal.util.email.EmailRulesValidator;
import com.epam.ta.reportportal.util.email.MailServiceFactory;
import com.epam.ta.reportportal.ws.converter.converters.NotificationConfigConverter;
import com.epam.ta.reportportal.ws.converter.converters.ProjectConverter;
import com.epam.ta.reportportal.ws.converter.converters.UserConverter;
import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.ta.reportportal.ws.reporting.OperationCompletionRS;
import com.epam.reportportal.model.ValidationConstraints;
import com.epam.ta.reportportal.ws.reporting.ItemAttributeResource;
import com.google.common.cache.Cache;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * @author Pavel Bortnik
 */
@Service
public class UpdateProjectHandlerImpl implements UpdateProjectHandler {

  private static final String UPDATE_EVENT = "update";

  private final ProjectExtractor projectExtractor;

  private final ProjectAttributeValidator projectAttributeValidator;

  private final ProjectRepository projectRepository;

  private final UserRepository userRepository;

  private final UserPreferenceRepository preferenceRepository;

  private final ProjectUserRepository projectUserRepository;

  private final MessageBus messageBus;

  private final ApplicationEventPublisher applicationEventPublisher;

  private final MailServiceFactory mailServiceFactory;

  private final AnalyzerStatusCache analyzerStatusCache;

  private final IndexerStatusCache indexerStatusCache;

  private final AnalyzerServiceClient analyzerServiceClient;

  private final LogIndexer logIndexer;

  private final ProjectConverter projectConverter;

  @Autowired
  public UpdateProjectHandlerImpl(ProjectExtractor projectExtractor,
      ProjectAttributeValidator projectAttributeValidator, ProjectRepository projectRepository,
      UserRepository userRepository, UserPreferenceRepository preferenceRepository,
      MessageBus messageBus, ProjectUserRepository projectUserRepository,
      ApplicationEventPublisher applicationEventPublisher, MailServiceFactory mailServiceFactory,
      AnalyzerStatusCache analyzerStatusCache, IndexerStatusCache indexerStatusCache,
      AnalyzerServiceClient analyzerServiceClient, LogIndexer logIndexer,
      ProjectConverter projectConverter) {
    this.projectExtractor = projectExtractor;
    this.projectAttributeValidator = projectAttributeValidator;
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
    this.preferenceRepository = preferenceRepository;
    this.messageBus = messageBus;
    this.projectUserRepository = projectUserRepository;
    this.applicationEventPublisher = applicationEventPublisher;
    this.mailServiceFactory = mailServiceFactory;
    this.analyzerStatusCache = analyzerStatusCache;
    this.indexerStatusCache = indexerStatusCache;
    this.analyzerServiceClient = analyzerServiceClient;
    this.logIndexer = logIndexer;
    this.projectConverter = projectConverter;
  }

  @Override
  public OperationCompletionRS updateProject(String projectName, UpdateProjectRQ updateProjectRQ,
      ReportPortalUser user) {
    Project project = projectRepository.findByName(projectName)
        .orElseThrow(() -> new ReportPortalException(ErrorType.PROJECT_NOT_FOUND, projectName));
    ProjectAttributesActivityResource before = TO_ACTIVITY_RESOURCE.apply(project);
    updateProjectConfiguration(updateProjectRQ.getConfiguration(), project);
    ofNullable(updateProjectRQ.getUserRoles()).ifPresent(
        roles -> updateProjectUserRoles(roles, project, user));
    projectRepository.save(project);
    ProjectAttributesActivityResource after = TO_ACTIVITY_RESOURCE.apply(project);

    applicationEventPublisher.publishEvent(new ProjectEvent(project.getId(), UPDATE_EVENT));
    publishUpdatedAttributesActivities(before, after, user, updateProjectRQ.getConfiguration());

    return new OperationCompletionRS(
        "Project with name = '" + project.getName() + "' is successfully updated.");
  }

  @Override
  public OperationCompletionRS updateProjectNotificationConfig(String projectName,
      ReportPortalUser user, ProjectNotificationConfigDTO updateProjectNotificationConfigRQ) {
    Project project = projectRepository.findByName(projectName)
        .orElseThrow(() -> new ReportPortalException(ErrorType.PROJECT_NOT_FOUND, projectName));
    ProjectResource before = projectConverter.TO_PROJECT_RESOURCE.apply(project);

    updateSenderCases(project, updateProjectNotificationConfigRQ.getSenderCases());

    project.getProjectAttributes().stream().filter(it -> it.getAttribute().getName()
            .equalsIgnoreCase(ProjectAttributeEnum.NOTIFICATIONS_ENABLED.getAttribute())).findAny()
        .ifPresent(
            pa -> pa.setValue(String.valueOf(updateProjectNotificationConfigRQ.isEnabled())));

    messageBus.publishActivity(
        new NotificationsConfigUpdatedEvent(before, updateProjectNotificationConfigRQ,
            user.getUserId(), user.getUsername()
        ));
    return new OperationCompletionRS(
        "Notification configuration of project - '" + projectName + "' is successfully updated.");
  }

  @Override
  public OperationCompletionRS unassignUsers(String projectName, UnassignUsersRQ unassignUsersRQ,
      ReportPortalUser user) {
    expect(unassignUsersRQ.getUsernames(), not(List::isEmpty)).verify(BAD_REQUEST_ERROR,
        "Request should contain at least one username."
    );
    Project project = projectRepository.findByName(projectName)
        .orElseThrow(() -> new ReportPortalException(PROJECT_NOT_FOUND, projectName));
    User modifier = userRepository.findById(user.getUserId())
        .orElseThrow(() -> new ReportPortalException(USER_NOT_FOUND, user.getUsername()));
    if (!UserRole.ADMINISTRATOR.equals(modifier.getRole())) {
      expect(unassignUsersRQ.getUsernames(), not(contains(equalTo(modifier.getLogin())))).verify(
          UNABLE_ASSIGN_UNASSIGN_USER_TO_PROJECT, "User should not unassign himself from project.");
    }

    List<ProjectUser> unassignedUsers =
        unassignUsers(unassignUsersRQ.getUsernames(), modifier, project, user);
    projectUserRepository.deleteAll(unassignedUsers);
    ProjectUtils.excludeProjectRecipients(
        unassignedUsers.stream().map(ProjectUser::getUser).collect(Collectors.toSet()), project);
    unassignedUsers.forEach(it -> preferenceRepository.removeByProjectIdAndUserId(project.getId(),
        it.getUser().getId()
    ));

    return new OperationCompletionRS("User(s) with username(s)='" + unassignUsersRQ.getUsernames()
        + "' was successfully un-assigned from project='" + project.getName() + "'");
  }

  @Override
  public OperationCompletionRS assignUsers(String projectName, AssignUsersRQ assignUsersRQ,
      ReportPortalUser user) {
    if (UserRole.ADMINISTRATOR.equals(user.getUserRole())) {
      Project project = projectRepository.findByName(normalizeId(projectName)).orElseThrow(
          () -> new ReportPortalException(ErrorType.PROJECT_NOT_FOUND, normalizeId(projectName)));

      List<String> assignedUsernames =
          project.getUsers().stream().map(u -> u.getUser().getLogin()).collect(toList());
      assignUsersRQ.getUserNames().forEach((name, role) -> {
        ProjectRole projectRole = ProjectRole.forName(role)
            .orElseThrow(() -> new ReportPortalException(ROLE_NOT_FOUND, role));
        assignUser(name, projectRole, assignedUsernames, project, user);
      });
    } else {
      expect(assignUsersRQ.getUserNames().keySet(),
          not(Preconditions.contains(equalTo(user.getUsername())))
      ).verify(UNABLE_ASSIGN_UNASSIGN_USER_TO_PROJECT,
          "User should not assign himself to project."
      );

      ReportPortalUser.ProjectDetails projectDetails =
          projectExtractor.extractProjectDetails(user, projectName);
      Project project = projectRepository.findById(projectDetails.getProjectId()).orElseThrow(
          () -> new ReportPortalException(ErrorType.PROJECT_NOT_FOUND, normalizeId(projectName)));

      List<String> assignedUsernames =
          project.getUsers().stream().map(u -> u.getUser().getLogin()).collect(toList());
      assignUsersRQ.getUserNames().forEach((name, role) -> {

        ProjectRole projectRole = ProjectRole.forName(role)
            .orElseThrow(() -> new ReportPortalException(ROLE_NOT_FOUND, role));
        ProjectRole modifierRole = projectDetails.getProjectRole();
        expect(modifierRole.sameOrHigherThan(projectRole), BooleanUtils::isTrue).verify(
            ACCESS_DENIED);
        assignUser(name, projectRole, assignedUsernames, project, user);
      });
    }

    return new OperationCompletionRS(
        "User(s) with username='" + assignUsersRQ.getUserNames().keySet()
            + "' was successfully assigned to project='" + normalizeId(projectName) + "'");
  }

  @Override
  public OperationCompletionRS indexProjectData(String projectName, ReportPortalUser user) {
    expect(analyzerServiceClient.hasClients(), Predicate.isEqual(true)).verify(
        ErrorType.UNABLE_INTERACT_WITH_INTEGRATION, "There are no analyzer deployed.");

    Project project = projectRepository.findByName(projectName)
        .orElseThrow(() -> new ReportPortalException(PROJECT_NOT_FOUND, projectName));

    expect(ofNullable(indexerStatusCache.getIndexingStatus().getIfPresent(project.getId())).orElse(
        false), equalTo(false)).verify(ErrorType.FORBIDDEN_OPERATION,
        "Index can not be removed until index generation proceeds."
    );

    Cache<Long, Long> analyzeStatus = analyzerStatusCache.getAnalyzeStatus(AUTO_ANALYZER_KEY)
        .orElseThrow(
            () -> new ReportPortalException(ErrorType.ANALYZER_NOT_FOUND, AUTO_ANALYZER_KEY));
    expect(analyzeStatus.asMap().containsValue(project.getId()), equalTo(false)).verify(
        ErrorType.FORBIDDEN_OPERATION, "Index can not be removed until auto-analysis proceeds.");

    logIndexer.deleteIndex(project.getId());

    logIndexer.index(project.getId(), AnalyzerUtils.getAnalyzerConfig(project)).thenAcceptAsync(
        indexedCount -> mailServiceFactory.getDefaultEmailService(true)
            .sendIndexFinishedEmail("Index generation has been finished", user.getEmail(),
                indexedCount
            ));

    messageBus.publishActivity(
        new ProjectIndexEvent(user.getUserId(), user.getUsername(), project.getId(),
            project.getName(), true
        ));
    return new OperationCompletionRS("Log indexing has been started");
  }

  private List<ProjectUser> unassignUsers(List<String> usernames, User modifier, Project project,
      ReportPortalUser user) {

    List<ProjectUser> unassignedUsers = Lists.newArrayListWithExpectedSize(usernames.size());
    if (modifier.getRole() == UserRole.ADMINISTRATOR) {
      usernames.forEach(username -> {
        User userForUnassign = userRepository.findByLogin(username)
            .orElseThrow(() -> new ReportPortalException(USER_NOT_FOUND, username));
        validateUnassigningUser(modifier, userForUnassign, project.getId(), project);
        unassignedUsers.add(unassignUser(project, username, userForUnassign, user));

      });
    } else {
      ReportPortalUser.ProjectDetails projectDetails =
          projectExtractor.extractProjectDetails(user, project.getName());

      usernames.forEach(username -> {
        User userForUnassign = userRepository.findByLogin(username)
            .orElseThrow(() -> new ReportPortalException(USER_NOT_FOUND, username));
        ProjectUser projectUser = userForUnassign.getProjects().stream()
            .filter(it -> Objects.equals(it.getProject().getId(), project.getId())).findFirst()
            .orElseThrow(() -> new ReportPortalException(USER_NOT_FOUND, userForUnassign.getLogin(),
                String.format("User not found in project %s", project.getName())
            ));

        expect(
            projectDetails.getProjectRole().sameOrHigherThan(projectUser.getProjectRole()),
            BooleanUtils::isTrue
        ).verify(ACCESS_DENIED);

        validateUnassigningUser(modifier, userForUnassign, project.getId(), project);
        unassignedUsers.add(unassignUser(project, username, userForUnassign, user));

      });
    }

    return unassignedUsers;
  }

  private ProjectUser unassignUser(Project project, String username, User userForUnassign,
      ReportPortalUser authorizedUser) {
    ProjectUser projectUser =
        project.getUsers().stream().filter(it -> it.getUser().getLogin().equalsIgnoreCase(username))
            .findFirst().orElseThrow(() -> new ReportPortalException(USER_NOT_FOUND, username));
    project.getUsers().remove(projectUser);
    userForUnassign.getProjects().remove(projectUser);

    UnassignUserEvent unassignUserEvent =
        new UnassignUserEvent(convertUserToResource(userForUnassign, projectUser),
            authorizedUser.getUserId(), authorizedUser.getUsername()
        );
    applicationEventPublisher.publishEvent(unassignUserEvent);

    return projectUser;
  }

  private UserActivityResource convertUserToResource(User user, ProjectUser projectUser) {
    Long projectId = projectUser.getProject().getId();
    return UserConverter.TO_ACTIVITY_RESOURCE.apply(user, projectId);
  }

  private void assignUser(String name, ProjectRole projectRole, List<String> assignedUsernames,
      Project project, ReportPortalUser authorizedUser) {

    User modifyingUser = userRepository.findByLogin(normalizeId(name))
        .orElseThrow(() -> new ReportPortalException(USER_NOT_FOUND, name));
    expect(name, not(in(assignedUsernames))).verify(UNABLE_ASSIGN_UNASSIGN_USER_TO_PROJECT,
        formattedSupplier("User '{}' cannot be assigned to project twice.", name)
    );
    if (ProjectType.UPSA.equals(project.getProjectType()) && UserType.UPSA.equals(
        modifyingUser.getUserType())) {
      fail().withError(UNABLE_ASSIGN_UNASSIGN_USER_TO_PROJECT, "Project and user has UPSA type!");
    }
    ProjectUser projectUser = new ProjectUser();
    projectUser.setProjectRole(projectRole);
    projectUser.setUser(modifyingUser);
    projectUser.setProject(project);
    project.getUsers().add(projectUser);

    AssignUserEvent assignUserEvent =
        new AssignUserEvent(convertUserToResource(modifyingUser, projectUser),
            authorizedUser.getUserId(), authorizedUser.getUsername(), false
        );
    applicationEventPublisher.publishEvent(assignUserEvent);
  }

  private void validateUnassigningUser(User modifier, User userForUnassign, Long projectId,
      Project project) {
    if (ProjectUtils.isPersonalForUser(project.getProjectType(), project.getName(),
        userForUnassign.getLogin()
    )) {
      fail().withError(
          UNABLE_ASSIGN_UNASSIGN_USER_TO_PROJECT,
          "Unable to unassign user from his personal project"
      );
    }
    if (ProjectType.UPSA.equals(project.getProjectType()) && UserType.UPSA.equals(
        userForUnassign.getUserType())) {
      fail().withError(UNABLE_ASSIGN_UNASSIGN_USER_TO_PROJECT, "Project and user has UPSA type!");
    }
    if (!ProjectUtils.doesHaveUser(project, userForUnassign.getLogin())) {
      fail().withError(USER_NOT_FOUND, userForUnassign.getLogin(),
          String.format("User not found in project %s", project.getName())
      );
    }
  }

  private void updateProjectUserRoles(Map<String, String> userRoles, Project project,
      ReportPortalUser user) {

    if (!user.getUserRole().equals(UserRole.ADMINISTRATOR)) {
      expect(userRoles.get(user.getUsername()), isNull()).verify(
          ErrorType.UNABLE_TO_UPDATE_YOURSELF_ROLE, user.getUsername());
    }

    if (MapUtils.isNotEmpty(userRoles)) {
      userRoles.forEach((key, value) -> {

        Optional<ProjectRole> newProjectRole = ProjectRole.forName(value);
        expect(newProjectRole, isPresent()).verify(ErrorType.ROLE_NOT_FOUND, value);

        Optional<ProjectUser> updatingProjectUser =
            ofNullable(ProjectUtils.findUserConfigByLogin(project, key));
        expect(updatingProjectUser, isPresent()).verify(ErrorType.USER_NOT_FOUND, key);

        if (UserRole.ADMINISTRATOR != user.getUserRole()) {
          ProjectRole principalRole =
              projectExtractor.extractProjectDetails(user, project.getName()).getProjectRole();
          ProjectRole updatingUserRole =
              ofNullable(ProjectUtils.findUserConfigByLogin(project, key)).orElseThrow(
                  () -> new ReportPortalException(ErrorType.USER_NOT_FOUND, key)).getProjectRole();
          /*
           * Validate principal role level is high enough
           */
          if (principalRole.sameOrHigherThan(updatingUserRole)) {
            expect(newProjectRole.get(), Preconditions.isLevelEnough(principalRole)).verify(
                ErrorType.ACCESS_DENIED);
          } else {
            expect(updatingUserRole, Preconditions.isLevelEnough(principalRole)).verify(
                ErrorType.ACCESS_DENIED);
          }
        }
        String oldRole = updatingProjectUser.get().getProjectRole().getRoleName();
        updatingProjectUser.get().setProjectRole(newProjectRole.get());

        publishChangeRoleEvent(user, updatingProjectUser.get(), oldRole);
      });
    }
  }

  private void publishChangeRoleEvent(ReportPortalUser loggedUser, ProjectUser updatingProjectUser,
      String oldRole) {
    String newRole = updatingProjectUser.getProjectRole().getRoleName();
    ChangeRoleEvent changeRoleEvent =
        getChangeRoleEvent(updatingProjectUser.getUser(), updatingProjectUser.getProject().getId(),
            loggedUser, oldRole, newRole
        );
    applicationEventPublisher.publishEvent(changeRoleEvent);
  }

  private ChangeRoleEvent getChangeRoleEvent(User updatingUser, Long projectId,
      ReportPortalUser loggedUser, String oldRole, String newRole) {
    UserActivityResource userActivityResource =
        new UserActivityResource(updatingUser.getId(), projectId, updatingUser.getLogin());
    return new ChangeRoleEvent(userActivityResource, oldRole, newRole, loggedUser.getUserId(),
        loggedUser.getUsername()
    );
  }

  private void updateProjectConfiguration(ProjectConfigurationUpdate configuration,
      Project project) {
    ofNullable(configuration).flatMap(config -> ofNullable(config.getProjectAttributes()))
        .ifPresent(attributes -> {
          projectAttributeValidator.verifyProjectAttributes(
              ProjectUtils.getConfigParameters(project.getProjectAttributes()), attributes);
          attributes.forEach((attribute, value) -> project.getProjectAttributes().stream()
              .filter(it -> it.getAttribute().getName().equalsIgnoreCase(attribute)).findFirst()
              .ifPresent(attr -> attr.setValue(value)));
        });
  }

  private void updateSenderCases(Project project, List<SenderCaseDTO> cases) {

    project.getSenderCases().clear();
    if (CollectionUtils.isNotEmpty(cases)) {
      cases.forEach(sendCase -> validateSenderCase(sendCase, project));

      /* Check project notification settings duplicates */
      Set<SenderCase> withoutDuplicateCases =
          cases.stream().distinct().map(NotificationConfigConverter.TO_CASE_MODEL)
              .peek(sc -> sc.setProject(project)).collect(toSet());
      if (cases.size() != withoutDuplicateCases.size()) {
        fail().withError(BAD_REQUEST_ERROR, "Project notification settings contain duplicate cases for this communication channel");
      }

      project.getSenderCases().addAll(withoutDuplicateCases);
    }

  }

  private void validateSenderCase(SenderCaseDTO sendCase, Project project) {
    expect(findByName(sendCase.getSendCase()).isPresent(), equalTo(true)).verify(
        BAD_REQUEST_ERROR, sendCase.getSendCase());
    expect(sendCase.getRecipients(), notNull()).verify(BAD_REQUEST_ERROR,
        "Recipients list should not be null"
    );
    expect(sendCase.getRecipients().isEmpty(), equalTo(false)).verify(BAD_REQUEST_ERROR,
        formattedSupplier("Empty recipients list for case '{}' ", sendCase)
    );
    sendCase.setRecipients(sendCase.getRecipients().stream().map(it -> {
      EmailRulesValidator.validateRecipient(project, it);
      return it.trim();
    }).distinct().collect(toList()));

    ofNullable(sendCase.getLaunchNames()).ifPresent(
        launchNames -> sendCase.setLaunchNames(launchNames.stream().map(name -> {
          EmailRulesValidator.validateLaunchName(name);
          return name.trim();
        }).distinct().collect(toList())));

    ofNullable(sendCase.getAttributes()).ifPresent(
        attributes -> sendCase.setAttributes(attributes.stream().peek(attribute -> {
          EmailRulesValidator.validateLaunchAttribute(attribute);
          cutAttributeToMaxLength(attribute);
          attribute.setValue(attribute.getValue().trim());
        }).collect(Collectors.toSet())));
  }

  private void cutAttributeToMaxLength(ItemAttributeResource entity) {
    String key = entity.getKey();
    String value = entity.getValue();
    if (key != null && key.length() > ValidationConstraints.MAX_ATTRIBUTE_LENGTH) {
      entity.setKey(key.substring(0, ValidationConstraints.MAX_ATTRIBUTE_LENGTH));
    }
    if (value != null && value.length() > ValidationConstraints.MAX_ATTRIBUTE_LENGTH) {
      entity.setValue(value.substring(0, ValidationConstraints.MAX_ATTRIBUTE_LENGTH));
    }
  }

  /**
   * Resolves and publishes activities according to changed attributes
   *
   * @param before              Object before update
   * @param after               Object after update
   * @param user                User
   * @param updateConfiguration Configuration fields that has been updated
   */
  private void publishUpdatedAttributesActivities(ProjectAttributesActivityResource before,
      ProjectAttributesActivityResource after, ReportPortalUser user,
      ProjectConfigurationUpdate updateConfiguration) {

    if (ActivityDetailsUtil.configChanged(before.getConfig(), after.getConfig(), Prefix.JOB)) {
      applicationEventPublisher.publishEvent(
          new ProjectUpdatedEvent(before, after, user.getUserId(), user.getUsername()));
    }

    if (ActivityDetailsUtil.configChanged(before.getConfig(), after.getConfig(), Prefix.ANALYZER)) {
      if (ActivityDetailsUtil.extractConfigByPrefix(updateConfiguration.getProjectAttributes(),
          AUTO_PATTERN_ANALYZER_ENABLED.getAttribute()
      ).isEmpty()) {
        applicationEventPublisher.publishEvent(
            new ProjectAnalyzerConfigEvent(before, after, user.getUserId(), user.getUsername()));
      } else {
        applicationEventPublisher.publishEvent(
            new ProjectPatternAnalyzerUpdateEvent(before, after, user.getUserId(),
                user.getUsername()
            ));
      }
    }
  }

}
