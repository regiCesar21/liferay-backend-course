package com.liferay.training.gradebook.configuration;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import aQute.bnd.annotation.metatype.Meta;
/**
 * Configuration interface for Gradebook service.
 *
 * An user interface for this interface is automatically created
 * in Control Panel -> System settings.
 *
 * @see <a
 * href="https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-2/making-appl * on making configurable applications at Liferay Developer Network</a>
 * @author liferay
 */
@ExtendedObjectClassDefinition(
        category = "Gradebook",
        scope = ExtendedObjectClassDefinition.Scope.SYSTEM
)
@Meta.OCD(
        id = "com.liferay.training.gradebook.configuration.GradebookSystemServiceConfiguration",
        localization = "content/Language",
        name = "gradebook-service-configuration-name"
)
public interface GradebookSystemServiceConfiguration {
    @Meta.AD(
            deflt = "10",
            description = "description-min-length-description",
            name = "description-min-length-name",
            required = false
    )
    public int descriptionMinLength();
    @Meta.AD(
            deflt = "200",
            description = "description-max-length-description",
            name = "description-max-length-name",
            required = false
    )
    public int descriptionMaxLength();
}