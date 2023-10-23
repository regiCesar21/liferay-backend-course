package com.liferay.training.gradebook.internal.search.spi.model.query.contributor;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.search.spi.model.query.contributor.ModelPreFilterContributor;
import com.liferay.portal.search.spi.model.registrar.ModelSearchSettings;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
@Component(
        immediate = true,
        property = "indexer.class.name=com.liferay.training.gradebook.model.Assignment",
        service = ModelPreFilterContributor.class
)
public class AssignmentModelPreFilterContributor
        implements ModelPreFilterContributor {
    @Override
    public void contribute(
            BooleanFilter booleanFilter, ModelSearchSettings modelSearchSettings,
            SearchContext searchContext) {
        addWorkflowStatusFilter(
                booleanFilter, modelSearchSettings, searchContext);
    }
    protected void addWorkflowStatusFilter(
            BooleanFilter booleanFilter, ModelSearchSettings modelSearchSettings,
            SearchContext searchContext) {
        workflowStatusModelPreFilterContributor.contribute(
                booleanFilter, modelSearchSettings, searchContext);
    }
    @Reference(target = "(model.pre.filter.contributor.id=WorkflowStatus)")
    protected ModelPreFilterContributor workflowStatusModelPreFilterContributor;
}