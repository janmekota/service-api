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

package com.epam.ta.reportportal.ws.controller;

import com.epam.ta.reportportal.auth.ReportPortalUser;
import com.epam.ta.reportportal.commons.EntityUtils;
import com.epam.ta.reportportal.commons.querygen.CompositeFilter;
import com.epam.ta.reportportal.commons.querygen.Filter;
import com.epam.ta.reportportal.commons.querygen.Queryable;
import com.epam.ta.reportportal.core.user.CreateUserHandler;
import com.epam.ta.reportportal.core.user.DeleteUserHandler;
import com.epam.ta.reportportal.core.user.EditUserHandler;
import com.epam.ta.reportportal.core.user.GetUserHandler;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.entity.user.UserRole;
import com.epam.ta.reportportal.ws.model.ModelViews;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.YesNoRS;
import com.epam.ta.reportportal.ws.model.user.*;
import com.epam.ta.reportportal.ws.resolver.ActiveRole;
import com.epam.ta.reportportal.ws.resolver.FilterFor;
import com.epam.ta.reportportal.ws.resolver.ResponseView;
import com.epam.ta.reportportal.ws.resolver.SortFor;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;

import static com.epam.ta.reportportal.auth.permissions.Permissions.ADMIN_ONLY;
import static com.epam.ta.reportportal.auth.permissions.Permissions.ALLOWED_TO_EDIT_USER;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/user")
public class UserController {

	private final CreateUserHandler createUserMessageHandler;

	private final EditUserHandler editUserMessageHandler;

	private final DeleteUserHandler deleteUserHandler;

	private final GetUserHandler getUserHandler;

	@Autowired
	public UserController(CreateUserHandler createUserMessageHandler, EditUserHandler editUserMessageHandler,
			DeleteUserHandler deleteUserHandler, GetUserHandler getUserHandler) {
		this.createUserMessageHandler = createUserMessageHandler;
		this.editUserMessageHandler = editUserMessageHandler;
		this.deleteUserHandler = deleteUserHandler;
		this.getUserHandler = getUserHandler;
	}

	@PostMapping
	@ResponseStatus(CREATED)
	@PreAuthorize(ADMIN_ONLY)
	@ApiOperation(value = "Create specified user", notes = "Allowable only for users with administrator role")
	public CreateUserRS createUserByAdmin(@RequestBody @Validated CreateUserRQFull rq,
			@AuthenticationPrincipal ReportPortalUser currentUser, HttpServletRequest request) {
		String basicURL = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
				.replacePath(null)
				.replaceQuery(null)
				.build()
				.toUriString();
		return createUserMessageHandler.createUserByAdmin(rq, currentUser, basicURL);
	}

	@Transactional
	@PostMapping(value = "/bid")
	@ResponseStatus(CREATED)
	@PreAuthorize("hasPermission(#createUserRQ.getDefaultProject(), 'projectManagerPermission')")
	@ApiOperation("Register invitation for user who will be created")
	public CreateUserBidRS createUserBid(@RequestBody @Validated CreateUserRQ createUserRQ,
			@AuthenticationPrincipal ReportPortalUser currentUser, HttpServletRequest request) {
		/*
		 * Use Uri components since they are aware of x-forwarded-host headers
		 */
		URI rqUrl = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
				.replacePath(null)
				.replaceQuery(null)
				.build()
				.toUri();
		return createUserMessageHandler.createUserBid(createUserRQ, currentUser, rqUrl.toASCIIString());
	}

	@Transactional
	@PostMapping(value = "/registration")
	@ResponseStatus(CREATED)
	@ApiOperation("Activate invitation and create user in system")
	public CreateUserRS createUser(@RequestBody @Validated CreateUserRQConfirm request, @RequestParam(value = "uuid") String uuid) {
		return createUserMessageHandler.createUser(request, uuid);
	}

	@Transactional(readOnly = true)
	@GetMapping(value = "/registration")
	@ApiIgnore
	public UserBidRS getUserBidInfo(@RequestParam(value = "uuid") String uuid) {
		return getUserHandler.getBidInformation(uuid);
	}

	@Transactional
	@DeleteMapping(value = "/{login}")
	@PreAuthorize(ADMIN_ONLY)
	@ApiOperation(value = "Delete specified user", notes = "Allowable only for users with administrator role")
	public OperationCompletionRS deleteUser(@PathVariable String login, @AuthenticationPrincipal ReportPortalUser currentUser) {
		return deleteUserHandler.deleteUser(EntityUtils.normalizeId(login), currentUser);
	}

	@Transactional
	@PutMapping(value = "/{login}")
	@PreAuthorize(ALLOWED_TO_EDIT_USER)
	@ApiOperation(value = "Edit specified user", notes = "Only for administrators and profile's owner")
	public OperationCompletionRS editUser(@PathVariable String login, @RequestBody @Validated EditUserRQ editUserRQ,
			@ActiveRole UserRole role, @AuthenticationPrincipal ReportPortalUser currentUser) {
		return editUserMessageHandler.editUser(EntityUtils.normalizeId(login), editUserRQ, role);
	}

	@Transactional(readOnly = true)
	@GetMapping(value = "/{login}")
	@ResponseView(ModelViews.FullUserView.class)
	@PreAuthorize(ALLOWED_TO_EDIT_USER)
	@ApiOperation(value = "Return information about specified user", notes = "Only for administrators and profile's owner")
	public UserResource getUser(@PathVariable String login, @AuthenticationPrincipal ReportPortalUser currentUser) {
		return getUserHandler.getUser(EntityUtils.normalizeId(login), currentUser);
	}

	@Transactional
	@GetMapping(value = { "", "/" })
	@ApiOperation("Return information about current logged-in user")
	public UserResource getMyself(@AuthenticationPrincipal ReportPortalUser currentUser) {
		return getUserHandler.getUser(currentUser);
	}

	@Transactional(readOnly = true)
	@GetMapping(value = "/all")
	@ResponseView(ModelViews.FullUserView.class)
	@PreAuthorize(ADMIN_ONLY)
	@ApiOperation(value = "Return information about all users", notes = "Allowable only for users with administrator role")
	public Iterable<UserResource> getUsers(@FilterFor(User.class) Filter filter, @SortFor(User.class) Pageable pageable,
			@FilterFor(User.class) Queryable queryable, @AuthenticationPrincipal ReportPortalUser currentUser) {
		return getUserHandler.getAllUsers(new CompositeFilter(filter, queryable), pageable);
	}

	@Transactional(readOnly = true)
	@GetMapping(value = "/registration/info")
	@ApiIgnore
	public YesNoRS validateInfo(@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "email", required = false) String email) {
		return getUserHandler.validateInfo(username, email);
	}

	@Transactional
	@PostMapping(value = "/password/restore")
	@ResponseStatus(OK)
	@ApiOperation("Create a restore password request")
	public OperationCompletionRS restorePassword(@RequestBody @Validated RestorePasswordRQ rq, HttpServletRequest request) {
		/*
		 * Use Uri components since they are aware of x-forwarded-host headers
		 */
		String baseUrl = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
				.replacePath(null)
				.replaceQuery(null)
				.build()
				.toUriString();
		return createUserMessageHandler.createRestorePasswordBid(rq, baseUrl);
	}

	@Transactional
	@PostMapping(value = "/password/reset")
	@ResponseStatus(OK)
	@ApiOperation("Reset password")
	public OperationCompletionRS resetPassword(@RequestBody @Validated ResetPasswordRQ rq) {
		return createUserMessageHandler.resetPassword(rq);
	}

	@Transactional(readOnly = true)
	@GetMapping(value = "/password/reset/{uuid}")
	@ResponseStatus(OK)
	@ApiOperation("Check if a restore password bid exists")
	public YesNoRS isRestorePasswordBidExist(@PathVariable String uuid) {
		return createUserMessageHandler.isResetPasswordBidExist(uuid);
	}

	@Transactional
	@PostMapping(value = "/password/change")
	@ResponseStatus(OK)
	@ApiOperation("Change own password")
	public OperationCompletionRS changePassword(@RequestBody @Validated ChangePasswordRQ changePasswordRQ,
			@AuthenticationPrincipal ReportPortalUser currentUser) {
		return editUserMessageHandler.changePassword(currentUser, changePasswordRQ);
	}

	@Transactional(readOnly = true)
	@GetMapping(value = "/{userName}/projects")
	@ResponseStatus(OK)
	@ApiIgnore
	public Map<String, UserResource.AssignedProject> getUserProjects(@PathVariable String userName,
			@AuthenticationPrincipal ReportPortalUser currentUser) {
		return getUserHandler.getUserProjects(userName);
	}

}
