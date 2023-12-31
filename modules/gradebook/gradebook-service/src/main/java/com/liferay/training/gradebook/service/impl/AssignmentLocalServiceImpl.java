/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.liferay.training.gradebook.service.impl;
import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.dao.orm.Disjunction;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowHandlerRegistryUtil;
import com.liferay.training.gradebook.model.Assignment;
import com.liferay.training.gradebook.model.*;
import com.liferay.training.gradebook.service.base.AssignmentLocalServiceBaseImpl;
import com.liferay.training.gradebook.validator.AssignmentValidator;

import java.io.Serializable;
import java.util.*;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The implementation of the assignment local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are
 * added, rerun ServiceBuilder to copy their definitions into the
 * <code>com.liferay.training.gradebook.service.AssignmentLocalService</code>
 * interface.
 *
 * <p>
 * This is a local service. Methods of this service will not have security
 * checks based on the propagated JAAS credentials because this service can only
 * be accessed from within the same VM.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see AssignmentLocalServiceBaseImpl
 */
@Component(
		property = "model.class.name=com.liferay.training.gradebook.model.Assignment",
		service = AopService.class
)

public class AssignmentLocalServiceImpl extends AssignmentLocalServiceBaseImpl {

	public Assignment updateAssignment(long assignmentId, Map<Locale, String> titleMap,
									   Map<Locale, String> description, Date dueDate, ServiceContext serviceContext) throws PortalException {

		// Validate assignment parameters.

		_assignmentValidator.validate(titleMap, description, dueDate);

		// Get the Assignment by id.

		Assignment assignment = getAssignment(assignmentId);

		// Set updated fields and modification date.

		assignment.setModifiedDate(new Date());
		assignment.setTitleMap(titleMap);
		assignment.setDueDate(dueDate);
		assignment.setDescriptionMap(description);
		assignment = super.updateAssignment(assignment);
		// Update Asset resources.
		updateAsset(assignment, serviceContext);
		return assignment;
	}

	public Assignment updateStatus(
			long userId, long assignmentId, int status,
			ServiceContext serviceContext)
			throws PortalException, SystemException {
		User user = userLocalService.getUser(userId);
		Assignment assignment = getAssignment(assignmentId);
		assignment.setStatus(status);
		assignment.setStatusByUserId(userId);
		assignment.setStatusByUserName(user.getFullName());
		assignment.setStatusDate(new Date());
		assignmentPersistence.update(assignment);
		if (status == WorkflowConstants.STATUS_APPROVED) {
			assetEntryLocalService.updateVisible(
					Assignment.class.getName(), assignmentId, true);
		}
		else {
			assetEntryLocalService.updateVisible(
					Assignment.class.getName(), assignmentId, false);
		}
		return assignment;
	}

	@Override
	public Assignment deleteAssignment(Assignment assignment) throws PortalException
	{
		// Delete permission resources.
		resourceLocalService.deleteResource(
				assignment, ResourceConstants.SCOPE_INDIVIDUAL);
		// Delete the Asset resource.
		assetEntryLocalService.deleteEntry(
				Assignment.class.getName(), assignment.getAssignmentId());
		// Delete the workflow resource.
		workflowInstanceLinkLocalService.deleteWorkflowInstanceLinks(
				assignment.getCompanyId(), assignment.getGroupId(),
				Assignment.class.getName(), assignment.getAssignmentId());
		// Delete the Assignment
		return super.deleteAssignment(assignment);
	}

	public List<Assignment> getAssignmentsByGroupId(long groupId) {
		return assignmentPersistence.findByGroupId(groupId);
	}
	public List<Assignment> getAssignmentsByGroupId(long groupId, int start, int end) {
		return assignmentPersistence.findByGroupId(groupId, start, end);
	}
	public List<Assignment> getAssignmentsByGroupId(long groupId, int start, int end,
													OrderByComparator<Assignment> orderByComparator) {
		return assignmentPersistence.findByGroupId(groupId, start, end, orderByComparator);
	}
	public List<Assignment> getAssignmentsByKeywords(
			long groupId, String keywords, int start, int end,
			OrderByComparator<Assignment> orderByComparator) {
		return assignmentLocalService.dynamicQuery(
				getKeywordSearchDynamicQuery(groupId, keywords), start, end,
				orderByComparator);
	}

	public long getAssignmentsCountByKeywords(
			long groupId, String keywords, int status) {
		DynamicQuery assignmentQuery = getKeywordSearchDynamicQuery(groupId, keywords);
		if (status != WorkflowConstants.STATUS_ANY) {
			assignmentQuery.add(RestrictionsFactoryUtil.eq("status", status));
		}
		return assignmentLocalService.dynamicQueryCount(assignmentQuery);
	}

	public long getAssignmentsCountByKeywords(long groupId, String keywords) {
		return assignmentLocalService.dynamicQueryCount(
				getKeywordSearchDynamicQuery(groupId, keywords));
	}
	private DynamicQuery getKeywordSearchDynamicQuery(
			long groupId, String keywords) {
		DynamicQuery dynamicQuery = dynamicQuery().add(
				RestrictionsFactoryUtil.eq("groupId", groupId));
		if (Validator.isNotNull(keywords)) {
			Disjunction disjunctionQuery =
					RestrictionsFactoryUtil.disjunction();
			disjunctionQuery.add(
					RestrictionsFactoryUtil.like("title", "%" + keywords + "%"));
			disjunctionQuery.add(
					RestrictionsFactoryUtil.like(
							"description", "%" + keywords + "%"));
			dynamicQuery.add(disjunctionQuery);
		}
		return dynamicQuery;
	}

	private void updateAsset(
			Assignment assignment, ServiceContext serviceContext)
			throws PortalException {
		assetEntryLocalService.updateEntry(
				serviceContext.getUserId(), serviceContext.getScopeGroupId(),
				assignment.getCreateDate(), assignment.getModifiedDate(),
				Assignment.class.getName(), assignment.getAssignmentId(),
				assignment.getUserUuid(), 0, serviceContext.getAssetCategoryIds(),
				serviceContext.getAssetTagNames(), true, true,
				assignment.getCreateDate(), null, null, null,
				ContentTypes.TEXT_HTML,
				assignment.getTitle(serviceContext.getLocale()),
				assignment.getDescription(), null, null, null, 0, 0,
				serviceContext.getAssetPriority());
	}

	protected Assignment startWorkflowInstance(
			long userId, Assignment assignment, ServiceContext serviceContext)
			throws PortalException {
		Map<String, Serializable> workflowContext = new HashMap();
		String userPortraitURL = StringPool.BLANK;
		String userURL = StringPool.BLANK;
		if (serviceContext.getThemeDisplay() != null) {
			User user = userLocalService.getUser(userId);
			userPortraitURL =
					user.getPortraitURL(serviceContext.getThemeDisplay());
			userURL = user.getDisplayURL(serviceContext.getThemeDisplay());
		}
		workflowContext.put(
				WorkflowConstants.CONTEXT_USER_PORTRAIT_URL, userPortraitURL);
		workflowContext.put(WorkflowConstants.CONTEXT_USER_URL, userURL);
		return WorkflowHandlerRegistryUtil.startWorkflowInstance(
				assignment.getCompanyId(), assignment.getGroupId(), userId,
				Assignment.class.getName(), assignment.getAssignmentId(),
				assignment, serviceContext, workflowContext);
	}

	public Assignment addAssignment(long groupId, Map<Locale, String> titleMap, Map<Locale, String> descriptionMap, Date dueDate, ServiceContext serviceContext) throws PortalException {
		// Validate assignment parameters.
		_assignmentValidator.validate(titleMap, descriptionMap, dueDate);
		// Get group and user.
		Group group = groupLocalService.getGroup(groupId);
		long userId = serviceContext.getUserId();
		User user = userLocalService.getUser(userId);
		// Generate primary key for the assignment.
		long assignmentId = counterLocalService.increment(Assignment.class.getName());
		// Create assigment. This doesn't yet persist the entity.
		Assignment assignment = createAssignment(assignmentId);
		// Populate fields.
		assignment.setCompanyId(group.getCompanyId());
		assignment.setCreateDate(serviceContext.getCreateDate(new Date()));
		assignment.setDueDate(dueDate);
		assignment.setDescriptionMap(descriptionMap);
		assignment.setGroupId(groupId);
		assignment.setModifiedDate(serviceContext.getModifiedDate(new Date()));
		assignment.setTitleMap(titleMap);
		assignment.setUserId(userId);
		assignment.setUserName(user.getScreenName());
		// Set Status fields.
		assignment.setStatus(WorkflowConstants.STATUS_DRAFT);
		assignment.setStatusByUserId(userId);
		assignment.setStatusByUserName(user.getFullName());
		assignment.setStatusDate(serviceContext.getModifiedDate(null));
		// Persist assignment to database.
		assignment = super.addAssignment(assignment);
		// Add permission resources.
		boolean portletActions = false;
		boolean addGroupPermissions = true;
		boolean addGuestPermissions = true;
		resourceLocalService.addResources(group.getCompanyId(), groupId, userId, Assignment.class.getName(), assignment.getAssignmentId(), portletActions, addGroupPermissions, addGuestPermissions);
				// Update asset.
				updateAsset(assignment, serviceContext);
		// Start workflow instance and return the assignment.

		return startWorkflowInstance(userId, assignment, serviceContext);
	}

	@Override
	public Assignment addAssignment(Assignment assignment) {
		throw new UnsupportedOperationException("Not supported.");
	}
	@Override
	public Assignment updateAssignment(Assignment assignment) {
		throw new UnsupportedOperationException("Not supported.");
	}
	@Reference
	AssignmentValidator _assignmentValidator;

	@Override
	public <T> T dslQuery(DSLQuery dslQuery) {
		return null;
	}

	@Override
	public int dslQueryCount(DSLQuery dslQuery) {
		return 0;
	}
}