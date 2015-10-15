package org.keycloak.subsystem.server.as7;

import org.jboss.as.server.deployment.*;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Created by marko on 15/10/15.
 */
public class KeycloakDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier INFINISPAN = ModuleIdentifier.create("org.infinispan");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        addPrivateDependencies(moduleSpecification, moduleLoader);

    }

    private void addPrivateDependencies(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        // ModuleDependency(ModuleLoader moduleLoader, ModuleIdentifier identifier, boolean optional, boolean export, boolean importServices, boolean userSpecified)
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, INFINISPAN, false, false, false, false));
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }
}
